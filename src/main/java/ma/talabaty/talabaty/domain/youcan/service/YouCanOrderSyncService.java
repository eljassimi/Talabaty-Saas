package ma.talabaty.talabaty.domain.youcan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.talabaty.talabaty.domain.orders.model.Order;
import ma.talabaty.talabaty.domain.orders.model.OrderSource;
import ma.talabaty.talabaty.domain.orders.model.OrderStatus;
import ma.talabaty.talabaty.domain.orders.repository.OrderRepository;
import ma.talabaty.talabaty.domain.orders.service.OrderService;
import ma.talabaty.talabaty.domain.stores.model.Store;
import ma.talabaty.talabaty.domain.youcan.model.YouCanStore;
import ma.talabaty.talabaty.domain.youcan.repository.YouCanStoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class YouCanOrderSyncService {

    private final YouCanStoreRepository youCanStoreRepository;
    private final YouCanApiService youCanApiService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public YouCanOrderSyncService(
            YouCanStoreRepository youCanStoreRepository,
            YouCanApiService youCanApiService,
            OrderService orderService,
            OrderRepository orderRepository,
            ObjectMapper objectMapper) {
        this.youCanStoreRepository = youCanStoreRepository;
        this.youCanApiService = youCanApiService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Sync orders from a YouCan store
     */
    @Transactional
    public int syncOrdersFromYouCanStore(UUID youCanStoreId) {
        YouCanStore youCanStore = youCanStoreRepository.findById(youCanStoreId)
                .orElseThrow(() -> new RuntimeException("YouCan store not found"));

        Store store = youCanStore.getStore();
        int syncedCount = 0;

        try {
            // Build filters for orders (e.g., last 30 days, or since last sync)
            Map<String, String> filters = new HashMap<>();
            if (youCanStore.getLastSyncAt() != null) {
                // Sync orders created or updated since last sync
                filters.put("updated_at_min", youCanStore.getLastSyncAt().toString());
            } else {
                // First sync: get orders from last 30 days
                filters.put("created_at_min", OffsetDateTime.now().minusDays(30).toString());
            }

            // Fetch orders from YouCan
            Map<String, Object> ordersResponse = youCanApiService.listOrders(youCanStore, filters);
            
            // Parse orders from response (adjust based on actual YouCan API response structure)
            List<Map<String, Object>> orders = extractOrdersFromResponse(ordersResponse);

            for (Map<String, Object> youcanOrder : orders) {
                try {
                    syncSingleOrder(store, youcanOrder, youCanStore);
                    syncedCount++;
                } catch (Exception e) {
                    System.err.println("Failed to sync YouCan order " + youcanOrder.get("id") + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Update last sync timestamp
            youCanStore.setLastSyncAt(OffsetDateTime.now());
            youCanStoreRepository.save(youCanStore);

        } catch (Exception e) {
            System.err.println("Failed to sync orders from YouCan store " + youCanStoreId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to sync orders from YouCan", e);
        }

        return syncedCount;
    }

    /**
     * Sync a single order from YouCan
     */
    @Transactional
    public Order syncSingleOrder(Store store, Map<String, Object> youcanOrder, YouCanStore youCanStore) {
        // Extract YouCan order ID - handle different possible formats
        Object orderIdObj = youcanOrder.get("id");
        String youcanOrderId = null;
        
        if (orderIdObj != null) {
            youcanOrderId = String.valueOf(orderIdObj).trim();
            // Handle null string
            if (youcanOrderId.equals("null") || youcanOrderId.isEmpty()) {
                youcanOrderId = null;
            }
        }
        
        // If no valid order ID, skip this order (can't track duplicates)
        if (youcanOrderId == null || youcanOrderId.isEmpty()) {
            throw new RuntimeException("YouCan order missing ID, cannot sync");
        }
        
        // Check if order already exists (by external_order_id)
        Order existingOrder = orderRepository.findByStoreIdAndExternalOrderId(
                store.getId(), youcanOrderId)
                .orElse(null);

        // Extract customer ID from order and fetch full customer details
        String customerId = extractCustomerId(youcanOrder);
        Map<String, Object> customerData = null;
        
        if (customerId != null && !customerId.isEmpty()) {
            try {
                customerData = youCanApiService.getCustomer(youCanStore, customerId);
                System.out.println("Fetched customer data for ID: " + customerId);
            } catch (Exception e) {
                System.err.println("Failed to fetch customer data for ID " + customerId + ": " + e.getMessage());
                // Continue with order data extraction as fallback
            }
        }
        
        // Extract order data from YouCan order (use customer data if available)
        String customerName = extractCustomerName(youcanOrder, customerData);
        String customerPhone = extractCustomerPhone(youcanOrder, customerData);
        String destinationAddress = extractDestinationAddress(youcanOrder, customerData);
        BigDecimal totalAmount = extractTotalAmount(youcanOrder);
        String currency = extractCurrency(youcanOrder);
        OrderStatus status = mapYouCanStatusToOrderStatus(youcanOrder);
        
        System.out.println("Extracted values:");
        System.out.println("  Customer Name: " + customerName);
        System.out.println("  Customer Phone: " + customerPhone);
        System.out.println("  Product Name: " + extractProductName(youcanOrder));
        System.out.println("  Total Amount: " + totalAmount);
        
        // Build metadata JSON
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("youcan_order_id", youcanOrderId);
        metadata.put("youcan_order_number", youcanOrder.get("order_number"));
        metadata.put("youcan_store_id", youCanStore.getYoucanStoreId());
        metadata.put("youcan_store_domain", youCanStore.getYoucanStoreDomain());
        
        // Add city if available in shipping address or customer data
        String city = extractCity(youcanOrder, customerData);
        if (city != null) {
            metadata.put("city", city);
        }

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            metadataJson = "{}";
        }

        if (existingOrder != null) {
            // Check if order was manually updated recently (within last 5 minutes)
            // If so, preserve manual changes and skip sync update
            OffsetDateTime orderUpdatedAt = existingOrder.getUpdatedAt();
            OffsetDateTime now = OffsetDateTime.now();
            
            // If order was updated within the last 5 minutes, assume it was manually updated
            // Preserve manual changes by skipping the sync update
            if (orderUpdatedAt != null) {
                long minutesSinceUpdate = java.time.Duration.between(orderUpdatedAt, now).toMinutes();
                
                if (minutesSinceUpdate < 5) {
                    // Order was recently updated (likely manually), preserve changes
                    // Only update status if it changed in YouCan (status updates are safe)
                    OrderStatus youcanStatus = mapYouCanStatusToOrderStatus(youcanOrder);
                    if (existingOrder.getStatus() != youcanStatus) {
                        // Only update status if it changed in YouCan
                        existingOrder.setStatus(youcanStatus);
                        orderRepository.saveAndFlush(existingOrder);
                    }
                    return existingOrder;
                }
            }
            
            // Order hasn't been manually updated recently, safe to sync from YouCan
            return orderService.updateOrder(
                    existingOrder.getId(),
                    customerName,
                    customerPhone,
                    destinationAddress,
                    city,
                    totalAmount,
                    currency,
                    metadataJson,
                    extractProductName(youcanOrder),
                    extractProductId(youcanOrder)
            );
        } else {
            // Create new order
            return orderService.createOrder(
                    store.getId(),
                    customerName,
                    customerPhone,
                    destinationAddress,
                    totalAmount,
                    currency,
                    youcanOrderId, // external_order_id
                    OrderSource.YOUCAN, // YouCan orders come from YouCan integration
                    extractProductName(youcanOrder),
                    extractProductId(youcanOrder)
            );
        }
    }

    /**
     * Extract orders list from YouCan API response
     * Adjust this based on actual YouCan API response structure
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractOrdersFromResponse(Map<String, Object> response) {
        // YouCan API might return orders in different formats
        // Common patterns: { "data": [...], "orders": [...], or direct array
        if (response.containsKey("data")) {
            Object data = response.get("data");
            if (data instanceof List) {
                return (List<Map<String, Object>>) data;
            }
        }
        if (response.containsKey("orders")) {
            Object orders = response.get("orders");
            if (orders instanceof List) {
                return (List<Map<String, Object>>) orders;
            }
        }
        // If response is directly a list
        if (response instanceof List) {
            return (List<Map<String, Object>>) response;
        }
        return List.of();
    }

    /**
     * Extract customer ID from order
     */
    private String extractCustomerId(Map<String, Object> youcanOrder) {
        // Try customer_id field
        if (youcanOrder.containsKey("customer_id")) {
            return String.valueOf(youcanOrder.get("customer_id"));
        }
        // Try customer object with id
        if (youcanOrder.containsKey("customer")) {
            Object customer = youcanOrder.get("customer");
            if (customer instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> customerMap = (Map<String, Object>) customer;
                if (customerMap.containsKey("id")) {
                    return String.valueOf(customerMap.get("id"));
                }
            } else if (customer instanceof String) {
                return (String) customer;
            }
        }
        return null;
    }

    private String extractCustomerName(Map<String, Object> youcanOrder, Map<String, Object> customerData) {
        // First, try to use customer data from customer endpoint
        if (customerData != null) {
            // Try full_name first
            if (customerData.containsKey("full_name")) {
                String name = String.valueOf(customerData.get("full_name"));
                if (name != null && !name.equals("null") && !name.trim().isEmpty()) {
                    return name;
                }
            }
            // Try first_name + last_name
            if (customerData.containsKey("first_name") || customerData.containsKey("last_name")) {
                String firstName = customerData.containsKey("first_name") ? 
                    String.valueOf(customerData.get("first_name")) : "";
                String lastName = customerData.containsKey("last_name") ? 
                    String.valueOf(customerData.get("last_name")) : "";
                String fullName = (firstName + " " + lastName).trim();
                if (!fullName.isEmpty() && !fullName.equals("null")) {
                    return fullName;
                }
            }
        }
        
        // Fallback to order data extraction
        // YouCan API: customer info can be in customer object (if included) or in shipping/payment address arrays
        // First, try customer object (if included with ?include=customer)
        if (youcanOrder.containsKey("customer")) {
            Object customer = youcanOrder.get("customer");
            if (customer instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> customerMap = (Map<String, Object>) customer;
                if (customerMap.containsKey("name")) {
                    String name = String.valueOf(customerMap.get("name"));
                    if (name != null && !name.equals("null") && !name.trim().isEmpty()) {
                        return name;
                    }
                }
                if (customerMap.containsKey("first_name") || customerMap.containsKey("last_name")) {
                    String firstName = customerMap.containsKey("first_name") ? 
                        String.valueOf(customerMap.get("first_name")) : "";
                    String lastName = customerMap.containsKey("last_name") ? 
                        String.valueOf(customerMap.get("last_name")) : "";
                    String fullName = (firstName + " " + lastName).trim();
                    if (!fullName.isEmpty() && !fullName.equals("null")) {
                        return fullName;
                    }
                }
            }
        }
        
        // Check shipping.address array (YouCan API structure)
        if (youcanOrder.containsKey("shipping")) {
            Object shipping = youcanOrder.get("shipping");
            if (shipping instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> shippingMap = (Map<String, Object>) shipping;
                if (shippingMap.containsKey("address") && shippingMap.get("address") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> addressList = (List<Object>) shippingMap.get("address");
                    if (!addressList.isEmpty() && addressList.get(0) instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> address = (Map<String, Object>) addressList.get(0);
                        if (address.containsKey("name")) {
                            String name = String.valueOf(address.get("name"));
                            if (name != null && !name.equals("null") && !name.trim().isEmpty()) {
                                return name;
                            }
                        }
                        if (address.containsKey("first_name") || address.containsKey("last_name")) {
                            String firstName = address.containsKey("first_name") ? 
                                String.valueOf(address.get("first_name")) : "";
                            String lastName = address.containsKey("last_name") ? 
                                String.valueOf(address.get("last_name")) : "";
                            String fullName = (firstName + " " + lastName).trim();
                            if (!fullName.isEmpty() && !fullName.equals("null")) {
                                return fullName;
                            }
                        }
                    }
                }
            }
        }
        
        // Check payment.address array
        if (youcanOrder.containsKey("payment")) {
            Object payment = youcanOrder.get("payment");
            if (payment instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> paymentMap = (Map<String, Object>) payment;
                if (paymentMap.containsKey("address") && paymentMap.get("address") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> addressList = (List<Object>) paymentMap.get("address");
                    if (!addressList.isEmpty() && addressList.get(0) instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> address = (Map<String, Object>) addressList.get(0);
                        if (address.containsKey("name")) {
                            String name = String.valueOf(address.get("name"));
                            if (name != null && !name.equals("null") && !name.trim().isEmpty()) {
                                return name;
                            }
                        }
                    }
                }
            }
        }
        
        return "YouCan Customer";
    }

    private String extractCustomerPhone(Map<String, Object> youcanOrder, Map<String, Object> customerData) {
        // First, try to use customer data from customer endpoint
        if (customerData != null) {
            if (customerData.containsKey("phone")) {
                String phone = String.valueOf(customerData.get("phone"));
                if (phone != null && !phone.equals("null") && !phone.trim().isEmpty()) {
                    return phone;
                }
            }
            // Try customer address array
            if (customerData.containsKey("address") && customerData.get("address") instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> addressList = (List<Object>) customerData.get("address");
                if (!addressList.isEmpty() && addressList.get(0) instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> address = (Map<String, Object>) addressList.get(0);
                    if (address.containsKey("phone")) {
                        String phone = String.valueOf(address.get("phone"));
                        if (phone != null && !phone.equals("null") && !phone.trim().isEmpty()) {
                            return phone;
                        }
                    }
                }
            }
        }
        
        // Fallback: YouCan API: phone is in shipping.address or payment.address arrays
        // Check shipping.address array
        if (youcanOrder.containsKey("shipping")) {
            Object shipping = youcanOrder.get("shipping");
            if (shipping instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> shippingMap = (Map<String, Object>) shipping;
                if (shippingMap.containsKey("address") && shippingMap.get("address") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> addressList = (List<Object>) shippingMap.get("address");
                    if (!addressList.isEmpty() && addressList.get(0) instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> address = (Map<String, Object>) addressList.get(0);
                        if (address.containsKey("phone")) {
                            String phone = String.valueOf(address.get("phone"));
                            if (phone != null && !phone.equals("null") && !phone.trim().isEmpty()) {
                                return phone;
                            }
                        }
                        if (address.containsKey("phone_number")) {
                            String phone = String.valueOf(address.get("phone_number"));
                            if (phone != null && !phone.equals("null") && !phone.trim().isEmpty()) {
                                return phone;
                            }
                        }
                        if (address.containsKey("mobile")) {
                            String phone = String.valueOf(address.get("mobile"));
                            if (phone != null && !phone.equals("null") && !phone.trim().isEmpty()) {
                                return phone;
                            }
                        }
                    }
                }
            }
        }
        
        // Check payment.address array
        if (youcanOrder.containsKey("payment")) {
            Object payment = youcanOrder.get("payment");
            if (payment instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> paymentMap = (Map<String, Object>) payment;
                if (paymentMap.containsKey("address") && paymentMap.get("address") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> addressList = (List<Object>) paymentMap.get("address");
                    if (!addressList.isEmpty() && addressList.get(0) instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> address = (Map<String, Object>) addressList.get(0);
                        if (address.containsKey("phone")) {
                            String phone = String.valueOf(address.get("phone"));
                            if (phone != null && !phone.equals("null") && !phone.trim().isEmpty()) {
                                return phone;
                            }
                        }
                    }
                }
            }
        }
        
        // Try customer object (if included)
        if (youcanOrder.containsKey("customer")) {
            Object customer = youcanOrder.get("customer");
            if (customer instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> customerMap = (Map<String, Object>) customer;
                if (customerMap.containsKey("phone")) {
                    String phone = String.valueOf(customerMap.get("phone"));
                    if (phone != null && !phone.equals("null") && !phone.trim().isEmpty()) {
                        return phone;
                    }
                }
            }
        }
        
        return "N/A";
    }

    private String extractDestinationAddress(Map<String, Object> youcanOrder, Map<String, Object> customerData) {
        // First, try to use customer address data from customer endpoint
        if (customerData != null && customerData.containsKey("address") && customerData.get("address") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> addressList = (List<Object>) customerData.get("address");
            if (!addressList.isEmpty() && addressList.get(0) instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> address = (Map<String, Object>) addressList.get(0);
                StringBuilder addressStr = new StringBuilder();
                
                // YouCan customer address structure: first_line, second_line, city, country_name
                if (address.containsKey("first_line")) {
                    String firstLine = String.valueOf(address.get("first_line"));
                    if (firstLine != null && !firstLine.equals("null") && !firstLine.trim().isEmpty()) {
                        addressStr.append(firstLine);
                    }
                }
                if (address.containsKey("second_line")) {
                    String secondLine = String.valueOf(address.get("second_line"));
                    if (secondLine != null && !secondLine.equals("null") && !secondLine.trim().isEmpty()) {
                        if (addressStr.length() > 0) addressStr.append(", ");
                        addressStr.append(secondLine);
                    }
                }
                if (address.containsKey("city")) {
                    String city = String.valueOf(address.get("city"));
                    if (city != null && !city.equals("null") && !city.trim().isEmpty()) {
                        if (addressStr.length() > 0) addressStr.append(", ");
                        addressStr.append(city);
                    }
                }
                if (address.containsKey("country_name")) {
                    String country = String.valueOf(address.get("country_name"));
                    if (country != null && !country.equals("null") && !country.trim().isEmpty()) {
                        if (addressStr.length() > 0) addressStr.append(", ");
                        addressStr.append(country);
                    }
                }
                if (addressStr.length() > 0) {
                    return addressStr.toString();
                }
            }
        }
        
        // Fallback: YouCan API: address is in shipping.address array
        if (youcanOrder.containsKey("shipping")) {
            Object shipping = youcanOrder.get("shipping");
            if (shipping instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> shippingMap = (Map<String, Object>) shipping;
                if (shippingMap.containsKey("address") && shippingMap.get("address") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> addressList = (List<Object>) shippingMap.get("address");
                    if (!addressList.isEmpty() && addressList.get(0) instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> address = (Map<String, Object>) addressList.get(0);
                        StringBuilder addressStr = new StringBuilder();
                        
                        // Try various address field names
                        if (address.containsKey("address1") || address.containsKey("address")) {
                            String addr = address.containsKey("address1") ? 
                                String.valueOf(address.get("address1")) : 
                                String.valueOf(address.get("address"));
                            if (addr != null && !addr.equals("null") && !addr.trim().isEmpty()) {
                                addressStr.append(addr);
                            }
                        }
                        if (address.containsKey("address2")) {
                            String addr2 = String.valueOf(address.get("address2"));
                            if (addr2 != null && !addr2.equals("null") && !addr2.trim().isEmpty()) {
                                if (addressStr.length() > 0) addressStr.append(", ");
                                addressStr.append(addr2);
                            }
                        }
                        if (address.containsKey("city")) {
                            String city = String.valueOf(address.get("city"));
                            if (city != null && !city.equals("null") && !city.trim().isEmpty()) {
                                if (addressStr.length() > 0) addressStr.append(", ");
                                addressStr.append(city);
                            }
                        }
                        if (address.containsKey("country")) {
                            String country = String.valueOf(address.get("country"));
                            if (country != null && !country.equals("null") && !country.trim().isEmpty()) {
                                if (addressStr.length() > 0) addressStr.append(", ");
                                addressStr.append(country);
                            }
                        }
                        if (addressStr.length() > 0) {
                            return addressStr.toString();
                        }
                    }
                }
            }
        }
        
        return "Address not provided";
    }

    private String extractCity(Map<String, Object> youcanOrder, Map<String, Object> customerData) {
        // First, try to use customer data from customer endpoint
        if (customerData != null) {
            // Try direct city field
            if (customerData.containsKey("city")) {
                String city = String.valueOf(customerData.get("city"));
                if (city != null && !city.equals("null") && !city.trim().isEmpty()) {
                    return city;
                }
            }
            // Try customer address array
            if (customerData.containsKey("address") && customerData.get("address") instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> addressList = (List<Object>) customerData.get("address");
                if (!addressList.isEmpty() && addressList.get(0) instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> address = (Map<String, Object>) addressList.get(0);
                    if (address.containsKey("city")) {
                        String city = String.valueOf(address.get("city"));
                        if (city != null && !city.equals("null") && !city.trim().isEmpty()) {
                            return city;
                        }
                    }
                }
            }
        }
        
        // Fallback: YouCan API: city is in shipping.address array
        if (youcanOrder.containsKey("shipping")) {
            Object shipping = youcanOrder.get("shipping");
            if (shipping instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> shippingMap = (Map<String, Object>) shipping;
                if (shippingMap.containsKey("address") && shippingMap.get("address") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> addressList = (List<Object>) shippingMap.get("address");
                    if (!addressList.isEmpty() && addressList.get(0) instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> address = (Map<String, Object>) addressList.get(0);
                        if (address.containsKey("city")) {
                            String city = String.valueOf(address.get("city"));
                            if (city != null && !city.equals("null") && !city.trim().isEmpty()) {
                                return city;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private BigDecimal extractTotalAmount(Map<String, Object> youcanOrder) {
        // YouCan might use "total_price", "total", "amount", etc.
        Object total = youcanOrder.get("total_price");
        if (total == null) {
            total = youcanOrder.get("total");
        }
        if (total == null) {
            total = youcanOrder.get("amount");
        }
        
        if (total instanceof Number) {
            return BigDecimal.valueOf(((Number) total).doubleValue());
        } else if (total != null) {
            try {
                return new BigDecimal(total.toString());
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return BigDecimal.ZERO;
    }

    private String extractCurrency(Map<String, Object> youcanOrder) {
        Object currency = youcanOrder.get("currency");
        if (currency != null) {
            return currency.toString();
        }
        return "MAD"; // Default currency
    }

    private OrderStatus mapYouCanStatusToOrderStatus(Map<String, Object> youcanOrder) {
        // Map YouCan order status to our OrderStatus enum
        // Adjust based on actual YouCan status values
        Object status = youcanOrder.get("status");
        if (status == null) {
            return OrderStatus.ENCOURS;
        }
        
        String statusStr = status.toString().toUpperCase();
        // Common YouCan statuses: pending, paid, fulfilled, cancelled, etc.
        return switch (statusStr) {
            case "PENDING", "UNPAID" -> OrderStatus.ENCOURS;
            case "PAID", "CONFIRMED" -> OrderStatus.CONFIRMED;
            case "FULFILLED", "SHIPPED" -> OrderStatus.CONCLED;
            case "CANCELLED", "CANCELED" -> OrderStatus.CONCLED;
            default -> OrderStatus.ENCOURS;
        };
    }

    private String extractProductName(Map<String, Object> youcanOrder) {
        // YouCan API structure: variants[].variant.product.name
        if (youcanOrder.containsKey("variants")) {
            Object variants = youcanOrder.get("variants");
            if (variants instanceof List && !((List<?>) variants).isEmpty()) {
                @SuppressWarnings("unchecked")
                List<Object> variantList = (List<Object>) variants;
                StringBuilder productNames = new StringBuilder();
                
                for (Object variantObj : variantList) {
                    if (variantObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> variant = (Map<String, Object>) variantObj;
                        
                        // YouCan structure: variant.variant.product.name
                        if (variant.containsKey("variant")) {
                            Object variantInner = variant.get("variant");
                            if (variantInner instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> variantMap = (Map<String, Object>) variantInner;
                                
                                if (variantMap.containsKey("product")) {
                                    Object product = variantMap.get("product");
                                    if (product instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> productMap = (Map<String, Object>) product;
                                        
                                        // Product name is in product.name
                                        if (productMap.containsKey("name")) {
                                            String itemName = String.valueOf(productMap.get("name"));
                                            if (itemName != null && !itemName.equals("null") && !itemName.trim().isEmpty()) {
                                                if (productNames.length() > 0) {
                                                    productNames.append(", ");
                                                }
                                                productNames.append(itemName);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (productNames.length() > 0) {
                    return productNames.toString();
                }
            }
        }
        
        return "YouCan Product";
    }

    private String extractProductId(Map<String, Object> youcanOrder) {
        if (youcanOrder.containsKey("line_items")) {
            Object lineItems = youcanOrder.get("line_items");
            if (lineItems instanceof List && !((List<?>) lineItems).isEmpty()) {
                Object firstItem = ((List<?>) lineItems).get(0);
                if (firstItem instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> item = (Map<String, Object>) firstItem;
                    if (item.containsKey("product_id")) {
                        return String.valueOf(item.get("product_id"));
                    }
                }
            }
        }
        return null;
    }
}


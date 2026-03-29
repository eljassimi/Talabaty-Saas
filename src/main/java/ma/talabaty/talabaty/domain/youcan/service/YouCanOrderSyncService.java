package ma.talabaty.talabaty.domain.youcan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.talabaty.talabaty.domain.orders.model.Order;
import ma.talabaty.talabaty.domain.orders.model.OrderSource;
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

    
    @Transactional
    public int syncOrdersFromYouCanStore(UUID youCanStoreId) {
        YouCanStore youCanStore = youCanStoreRepository.findById(youCanStoreId)
                .orElseThrow(() -> new RuntimeException("YouCan store not found"));

        Store store = youCanStore.getStore();
        int syncedCount = 0;

        try {
            
            Map<String, String> filters = new HashMap<>();
            if (youCanStore.getLastSyncAt() != null) {
                
                filters.put("updated_at_min", youCanStore.getLastSyncAt().toString());
            } else {
                
                filters.put("created_at_min", OffsetDateTime.now().minusDays(30).toString());
            }

            
            Map<String, Object> ordersResponse = youCanApiService.listOrders(youCanStore, filters);
            
            
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

            
            youCanStore.setLastSyncAt(OffsetDateTime.now());
            youCanStoreRepository.save(youCanStore);

        } catch (Exception e) {
            System.err.println("Failed to sync orders from YouCan store " + youCanStoreId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to sync orders from YouCan", e);
        }

        return syncedCount;
    }

    
    @Transactional
    public Order syncSingleOrder(Store store, Map<String, Object> youcanOrder, YouCanStore youCanStore) {
        
        Object orderIdObj = youcanOrder.get("id");
        String youcanOrderId = null;
        
        if (orderIdObj != null) {
            youcanOrderId = String.valueOf(orderIdObj).trim();
            
            if (youcanOrderId.equals("null") || youcanOrderId.isEmpty()) {
                youcanOrderId = null;
            }
        }
        
        
        if (youcanOrderId == null || youcanOrderId.isEmpty()) {
            throw new RuntimeException("YouCan order missing ID, cannot sync");
        }
        
        
        Order existingOrder = orderRepository.findByStoreIdAndExternalOrderId(
                store.getId(), youcanOrderId)
                .orElse(null);

        
        String customerId = extractCustomerId(youcanOrder);
        Map<String, Object> customerData = null;
        
        if (customerId != null && !customerId.isEmpty()) {
            try {
                customerData = youCanApiService.getCustomer(youCanStore, customerId);
            } catch (Exception e) {
                System.err.println("Failed to fetch customer data for ID " + customerId + ": " + e.getMessage());
                
            }
        }
        
        
        String customerName = extractCustomerName(youcanOrder, customerData);
        String customerPhone = extractCustomerPhone(youcanOrder, customerData);
        String destinationAddress = extractDestinationAddress(youcanOrder, customerData);
        BigDecimal totalAmount = extractTotalAmount(youcanOrder);
        String currency = extractCurrency(youcanOrder);

        String city = extractCity(youcanOrder, customerData);
        if (city == null) {
            city = enrichCityFromFullOrder(youCanStore, youcanOrderId, customerData, youcanOrder);
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("youcan_order_id", youcanOrderId);
        metadata.put("youcan_order_number", youcanOrder.get("order_number"));
        metadata.put("youcan_store_id", youCanStore.getYoucanStoreId());
        metadata.put("youcan_store_domain", youCanStore.getYoucanStoreDomain());

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
            
            
            OffsetDateTime orderUpdatedAt = existingOrder.getUpdatedAt();
            OffsetDateTime now = OffsetDateTime.now();

            if (orderUpdatedAt != null) {
                long minutesSinceUpdate = java.time.Duration.between(orderUpdatedAt, now).toMinutes();

                if (minutesSinceUpdate < 5) {
                    
                    
                    return existingOrder;
                }
            }

            
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
            return orderService.createOrder(
                    store.getId(),
                    customerName,
                    customerPhone,
                    destinationAddress,
                    totalAmount,
                    currency,
                    youcanOrderId,
                    OrderSource.YOUCAN,
                    extractProductName(youcanOrder),
                    extractProductId(youcanOrder),
                    city,
                    metadataJson
            );
        }
    }

    
    private String enrichCityFromFullOrder(YouCanStore youCanStore, String youcanOrderId,
                                          Map<String, Object> customerData,
                                          Map<String, Object> listOrderPayload) {
        try {
            Map<String, Object> full = youCanApiService.getOrder(youCanStore, youcanOrderId);
            String c = extractCity(full, customerData);
            if (c != null) {
                return c;
            }
            c = extractCity(listOrderPayload, customerData);
            if (c != null) {
                return c;
            }
            String cid = extractCustomerId(full);
            if (cid == null || cid.isBlank() || "null".equalsIgnoreCase(cid)) {
                cid = extractCustomerId(listOrderPayload);
            }
            if (cid == null || cid.isBlank() || "null".equalsIgnoreCase(cid)) {
                return null;
            }
            try {
                Map<String, Object> fresh = youCanApiService.getCustomer(youCanStore, cid);
                c = extractCity(full, fresh);
                if (c != null) {
                    return c;
                }
                return extractCity(listOrderPayload, fresh);
            } catch (Exception e) {
                System.err.println("YouCan enrichCity: getCustomer failed: " + e.getMessage());
                return null;
            }
        } catch (Exception e) {
            System.err.println("YouCan enrichCityFromFullOrder failed for order " + youcanOrderId + ": " + e.getMessage());
            return null;
        }
    }

    
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractOrdersFromResponse(Map<String, Object> response) {
        
        
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
        
        if (response instanceof List) {
            return (List<Map<String, Object>>) response;
        }
        return List.of();
    }

    
    private String extractCustomerId(Map<String, Object> youcanOrder) {
        
        if (youcanOrder.containsKey("customer_id")) {
            return String.valueOf(youcanOrder.get("customer_id"));
        }
        
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
        
        if (customerData != null) {
            
            if (customerData.containsKey("full_name")) {
                String name = String.valueOf(customerData.get("full_name"));
                if (name != null && !name.equals("null") && !name.trim().isEmpty()) {
                    return name;
                }
            }
            
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
        
        if (customerData != null) {
            if (customerData.containsKey("phone")) {
                String phone = String.valueOf(customerData.get("phone"));
                if (phone != null && !phone.equals("null") && !phone.trim().isEmpty()) {
                    return phone;
                }
            }
            
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
        
        if (customerData != null && customerData.containsKey("address") && customerData.get("address") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> addressList = (List<Object>) customerData.get("address");
            if (!addressList.isEmpty() && addressList.get(0) instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> address = (Map<String, Object>) addressList.get(0);
                StringBuilder addressStr = new StringBuilder();
                
                
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

    private static String textValue(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        return s;
    }

    
    private static String cityFromAddressMap(Map<String, Object> address) {
        if (address == null) {
            return null;
        }
        for (String key : new String[]{"city", "city_name", "town", "locality", "village"}) {
            String v = textValue(address.get(key));
            if (v != null) {
                return v;
            }
        }
        String region = textValue(address.get("region"));
        if (region != null) {
            return region;
        }
        Object loc = address.get("location");
        if (loc instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> locMap = (Map<String, Object>) loc;
            for (String key : new String[]{"city", "name", "locality"}) {
                String v = textValue(locMap.get(key));
                if (v != null) {
                    return v;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String cityFromAddressField(Object addressField) {
        if (addressField == null) {
            return null;
        }
        if (addressField instanceof Map) {
            return cityFromAddressMap((Map<String, Object>) addressField);
        }
        if (addressField instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map) {
                return cityFromAddressMap((Map<String, Object>) first);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String cityFromShippingOrPayment(Map<String, Object> youcanOrder, String shippingOrPaymentKey) {
        Object container = youcanOrder.get(shippingOrPaymentKey);
        if (!(container instanceof Map)) {
            return null;
        }
        Map<String, Object> map = (Map<String, Object>) container;
        String fromAddr = cityFromAddressField(map.get("address"));
        if (fromAddr != null) {
            return fromAddr;
        }
        return cityFromAddressField(map.get("shipping_address"));
    }

    @SuppressWarnings("unchecked")
    private String extractCityFromEmbeddedCustomer(Map<String, Object> youcanOrder) {
        Object c = youcanOrder.get("customer");
        if (!(c instanceof Map)) {
            return null;
        }
        Map<String, Object> customerMap = (Map<String, Object>) c;
        String v = textValue(customerMap.get("city"));
        if (v != null) {
            return v;
        }
        v = cityFromAddressField(customerMap.get("address"));
        if (v != null) {
            return v;
        }
        Object addresses = customerMap.get("addresses");
        if (addresses instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map) {
                    v = cityFromAddressMap((Map<String, Object>) item);
                    if (v != null) {
                        return v;
                    }
                }
            }
        }
        return null;
    }

    private String extractCity(Map<String, Object> youcanOrder, Map<String, Object> customerData) {
        if (customerData != null) {
            String v = textValue(customerData.get("city"));
            if (v != null) {
                return v;
            }
            v = cityFromAddressField(customerData.get("address"));
            if (v != null) {
                return v;
            }
            Object addresses = customerData.get("addresses");
            if (addresses instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> addr = (Map<String, Object>) item;
                        v = cityFromAddressMap(addr);
                        if (v != null) {
                            return v;
                        }
                    }
                }
            }
        }

        String embedded = extractCityFromEmbeddedCustomer(youcanOrder);
        if (embedded != null) {
            return embedded;
        }

        String fromShip = cityFromShippingOrPayment(youcanOrder, "shipping");
        if (fromShip != null) {
            return fromShip;
        }
        fromShip = cityFromShippingOrPayment(youcanOrder, "payment");
        if (fromShip != null) {
            return fromShip;
        }

        return cityFromAddressField(youcanOrder.get("shipping_address"));
    }

    private BigDecimal extractTotalAmount(Map<String, Object> youcanOrder) {
        
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
                
            }
        }
        return BigDecimal.ZERO;
    }

    private String extractCurrency(Map<String, Object> youcanOrder) {
        Object currency = youcanOrder.get("currency");
        if (currency != null) {
            return currency.toString();
        }
        return "MAD"; 
    }

    private String extractProductName(Map<String, Object> youcanOrder) {
        
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


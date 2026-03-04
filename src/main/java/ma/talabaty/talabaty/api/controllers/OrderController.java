package ma.talabaty.talabaty.api.controllers;

import jakarta.validation.Valid;
import ma.talabaty.talabaty.api.dtos.OrderDto;
import ma.talabaty.talabaty.api.mappers.OrderMapper;
import ma.talabaty.talabaty.core.security.AuthenticationHelper;
import ma.talabaty.talabaty.core.security.PermissionChecker;
import ma.talabaty.talabaty.domain.orders.model.Order;
import org.springframework.security.access.AccessDeniedException;
import ma.talabaty.talabaty.domain.orders.model.OrderSource;
import ma.talabaty.talabaty.domain.orders.model.OrderStatus;
import ma.talabaty.talabaty.domain.orders.model.OrderStatusHistory;
import ma.talabaty.talabaty.domain.orders.repository.OrderRepository;
import ma.talabaty.talabaty.domain.orders.service.OrderService;
import ma.talabaty.talabaty.domain.shipping.model.ProviderType;
import ma.talabaty.talabaty.domain.shipping.service.OzonExpressService;
import ma.talabaty.talabaty.domain.shipping.service.ShippingProviderService;
import ma.talabaty.talabaty.domain.stores.repository.StoreRepository;
import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.model.UserRole;
import ma.talabaty.talabaty.domain.users.service.UserService;
import ma.talabaty.talabaty.utils.CityMappingUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final OzonExpressService ozonExpressService;
    private final ShippingProviderService shippingProviderService;
    private final UserService userService;
    private final PermissionChecker permissionChecker;
    private final ObjectMapper objectMapper;

    public OrderController(OrderService orderService, OrderMapper orderMapper, StoreRepository storeRepository,
                          OrderRepository orderRepository, OzonExpressService ozonExpressService,
                          ShippingProviderService shippingProviderService, UserService userService,
                          PermissionChecker permissionChecker) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
        this.storeRepository = storeRepository;
        this.orderRepository = orderRepository;
        this.ozonExpressService = ozonExpressService;
        this.shippingProviderService = shippingProviderService;
        this.userService = userService;
        this.permissionChecker = permissionChecker;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderRequest request, Authentication authentication) {
        // Validate and parse storeId
        UUID storeId;
        try {
            if (request.getStoreId() == null || request.getStoreId().trim().isEmpty()) {
                throw new IllegalArgumentException("storeId is required and cannot be empty");
            }
            storeId = UUID.fromString(request.getStoreId());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid storeId format: '" + request.getStoreId() + "'. Please provide a valid UUID.", e);
        }
        
        // Verify that the store belongs to the authenticated user's account
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        storeRepository.findByAccountIdAndId(accountId, storeId)
                .orElseThrow(() -> new RuntimeException("Store not found or does not belong to your account"));
        
        Order order = orderService.createOrder(
                storeId,
                request.getCustomerName(),
                request.getCustomerPhone(),
                request.getDestinationAddress(),
                request.getTotalAmount(),
                request.getCurrency(),
                request.getExternalOrderId(),
                request.getSource(),
                request.getProductName(),
                request.getProductId()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(orderMapper.toDto(order));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<OrderDto>> getOrdersByStore(@PathVariable String storeId, Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID storeUuid;
        try {
            storeUuid = UUID.fromString(storeId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid storeId format: '" + storeId + "'. Please provide a valid UUID.", e);
        }
        
        // Verify that the store belongs to the authenticated user's account
        var storeOptional = storeRepository.findByAccountIdAndId(accountId, storeUuid);
        if (storeOptional.isEmpty()) {
            var storeExists = storeRepository.findById(storeUuid);
            if (storeExists.isPresent()) {
                throw new RuntimeException("Store not found or does not belong to your account. Store belongs to account: " + storeExists.get().getAccount().getId() + ", but you are authenticated as account: " + accountId);
            } else {
                throw new RuntimeException("Store not found or does not belong to your account");
            }
        }
        
        List<Order> orders = orderService.findByStoreId(storeUuid);
        List<OrderDto> dtos = orders.stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable String id, Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID orderUuid;
        try {
            orderUuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid orderId format: '" + id + "'. Please provide a valid UUID.", e);
        }
        
        Order order = orderService.findById(orderUuid)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Verify that the order's store belongs to the authenticated user's account
        storeRepository.findByAccountIdAndId(accountId, order.getStore().getId())
                .orElseThrow(() -> new RuntimeException("Order not found or does not belong to your account"));
        
        return ResponseEntity.ok(orderMapper.toDto(order));
    }
    
    @GetMapping("/{id}/debug")
    public ResponseEntity<Map<String, Object>> debugOrder(@PathVariable String id, Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID orderUuid;
        try {
            orderUuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid orderId format: '" + id + "'. Please provide a valid UUID.", e);
        }
        
        // Use orderRepository directly to force a fresh query
        Order order = orderRepository.findById(orderUuid)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Verify that the order's store belongs to the authenticated user's account
        storeRepository.findByAccountIdAndId(accountId, order.getStore().getId())
                .orElseThrow(() -> new RuntimeException("Order not found or does not belong to your account"));
        
        // Force Hibernate to load the totalAmount field
        BigDecimal totalAmount = order.getTotalAmount();
        
        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("id", order.getId().toString());
        debugInfo.put("totalAmount", totalAmount);
        debugInfo.put("totalAmount_string", totalAmount != null ? totalAmount.toString() : "null");
        debugInfo.put("totalAmount_double", totalAmount != null ? totalAmount.doubleValue() : null);
        debugInfo.put("totalAmount_compareToZero", totalAmount != null ? totalAmount.compareTo(BigDecimal.ZERO) : null);
        debugInfo.put("totalAmount_isZero", totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) == 0);
        debugInfo.put("totalAmountString", order.getTotalAmount() != null ? order.getTotalAmount().toString() : "null");
        debugInfo.put("totalAmountDouble", order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : null);
        debugInfo.put("currency", order.getCurrency());
        debugInfo.put("metadata", order.getMetadata());
        debugInfo.put("customerName", order.getCustomerName());
        debugInfo.put("customerPhone", order.getCustomerPhone());
        debugInfo.put("productName", order.getProductName());
        debugInfo.put("productId", order.getProductId());
        
        // Try to parse metadata
        if (order.getMetadata() != null && !order.getMetadata().trim().isEmpty()) {
            try {
                JsonNode metadataNode = objectMapper.readTree(order.getMetadata());
                Map<String, Object> metadataMap = new HashMap<>();
                metadataNode.fieldNames().forEachRemaining(key -> {
                    JsonNode value = metadataNode.get(key);
                    if (value.isTextual()) {
                        metadataMap.put(key, value.asText());
                    } else if (value.isNumber()) {
                        metadataMap.put(key, value.asDouble());
                    } else {
                        metadataMap.put(key, value.toString());
                    }
                });
                debugInfo.put("metadataParsed", metadataMap);
            } catch (Exception e) {
                debugInfo.put("metadataParseError", e.getMessage());
            }
        }
        
        return ResponseEntity.ok(debugInfo);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable String id,
            @RequestBody UpdateStatusRequest request,
            Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID currentUserId = AuthenticationHelper.getUserIdFromAuth(authentication);
        UUID orderUuid;
        try {
            orderUuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid orderId format: '" + id + "'. Please provide a valid UUID.", e);
        }
        
        Order order = orderService.findById(orderUuid)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Verify that the order's store belongs to the authenticated user's account
        storeRepository.findByAccountIdAndId(accountId, order.getStore().getId())
                .orElseThrow(() -> new RuntimeException("Order not found or does not belong to your account"));
        
        // Check permissions
        User currentUser = userService.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        if (!permissionChecker.canUpdateOrder(currentUser.getRole())) {
            throw new AccessDeniedException("You do not have permission to update order status");
        }
        
        // For SUPPORT role: check if order is assigned to someone else
        if (!permissionChecker.canUpdateOrder(currentUser.getRole(), currentUserId, order)) {
            throw new AccessDeniedException("This order is assigned to another support member. You cannot modify it.");
        }
        
        UUID changedByUserId = null;
        if (request.getChangedByUserId() != null && !request.getChangedByUserId().trim().isEmpty()) {
            try {
                changedByUserId = UUID.fromString(request.getChangedByUserId());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid changedByUserId format: '" + request.getChangedByUserId() + "'. Please provide a valid UUID.", e);
            }
        } else {
            // Use authenticated user ID if not provided
            changedByUserId = currentUserId;
        }
        
        Order updatedOrder = orderService.updateOrderStatus(
                orderUuid,
                request.getStatus(),
                request.getNote(),
                changedByUserId
        );
        return ResponseEntity.ok(orderMapper.toDto(updatedOrder));
    }

    /**
     * Update order details (customer info, address, amount, etc.)
     * SUPPORT, MANAGER, ACCOUNT_OWNER, and PLATFORM_ADMIN can update orders
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> updateOrder(
            @PathVariable String id,
            @RequestBody UpdateOrderRequest request,
            Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID currentUserId = AuthenticationHelper.getUserIdFromAuth(authentication);
        UUID orderUuid;
        try {
            orderUuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid orderId format: '" + id + "'. Please provide a valid UUID.", e);
        }
        
        Order order = orderService.findById(orderUuid)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Verify that the order's store belongs to the authenticated user's account
        storeRepository.findByAccountIdAndId(accountId, order.getStore().getId())
                .orElseThrow(() -> new RuntimeException("Order not found or does not belong to your account"));
        
        // Check permissions: SUPPORT, MANAGER, ACCOUNT_OWNER, and PLATFORM_ADMIN can update orders
        User currentUser = userService.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        UserRole currentRole = currentUser.getRole();
        if (currentRole != UserRole.SUPPORT &&
            currentRole != UserRole.MANAGER &&
            currentRole != UserRole.ACCOUNT_OWNER &&
            currentRole != UserRole.PLATFORM_ADMIN) {
            throw new RuntimeException("You do not have permission to update orders. Only SUPPORT, MANAGER, ACCOUNT_OWNER, and PLATFORM_ADMIN can update orders.");
        }
        
        Order updatedOrder = orderService.updateOrder(
                orderUuid,
                request.getCustomerName(),
                request.getCustomerPhone(),
                request.getDestinationAddress(),
                request.getCity(),
                request.getTotalAmount(),
                request.getCurrency(),
                request.getMetadata(),
                request.getProductName(),
                request.getProductId()
        );
        
        return ResponseEntity.ok(orderMapper.toDto(updatedOrder));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<OrderStatusHistory>> getOrderHistory(@PathVariable String id, Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID orderUuid;
        try {
            orderUuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid orderId format: '" + id + "'. Please provide a valid UUID.", e);
        }
        
        Order order = orderService.findById(orderUuid)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Verify that the order's store belongs to the authenticated user's account
        storeRepository.findByAccountIdAndId(accountId, order.getStore().getId())
                .orElseThrow(() -> new RuntimeException("Order not found or does not belong to your account"));
        
        List<OrderStatusHistory> history = orderService.getOrderStatusHistory(orderUuid);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/store/{storeId}/status/{status}")
    public ResponseEntity<List<OrderDto>> getOrdersByStoreAndStatus(
            @PathVariable String storeId,
            @PathVariable OrderStatus status,
            Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID storeUuid;
        try {
            storeUuid = UUID.fromString(storeId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid storeId format: '" + storeId + "'. Please provide a valid UUID.", e);
        }
        
        // Verify that the store belongs to the authenticated user's account
        storeRepository.findByAccountIdAndId(accountId, storeUuid)
                .orElseThrow(() -> new RuntimeException("Store not found or does not belong to your account"));
        
        List<Order> orders = orderService.findByStoreIdAndStatus(storeUuid, status);
        List<OrderDto> dtos = orders.stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/send-to-shipping")
    public ResponseEntity<Map<String, Object>> sendOrderToShipping(
            @PathVariable String id,
            @RequestBody SendToShippingRequest request,
            Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID orderUuid;
        try {
            orderUuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid orderId format: '" + id + "'. Please provide a valid UUID.", e);
        }
        
        // Get order and verify it belongs to the account
        Order order = orderService.findById(orderUuid)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        storeRepository.findByAccountIdAndId(accountId, order.getStore().getId())
                .orElseThrow(() -> new RuntimeException("Order not found or does not belong to your account"));
        
        // Get Ozon Express provider for the store (store-level first, then account-level)
        var provider = shippingProviderService.getActiveProviderForStore(order.getStore().getId(), ProviderType.OZON_EXPRESS)
                .orElseThrow(() -> new RuntimeException("Ozon Express provider not configured for this store. Please configure it first using POST /api/stores/{storeId}/shipping-providers"));
        
        // Validate required fields
        if (request.getCityId() == null || request.getCityId().trim().isEmpty()) {
            throw new IllegalArgumentException("cityId is required. You can get the list of cities using GET /api/shipping/ozon-express/cities");
        }
        
        // Map order to Ozon Express parcel request
        OzonExpressService.CreateParcelRequest parcelRequest = new OzonExpressService.CreateParcelRequest();
        parcelRequest.setReceiver(order.getCustomerName());
        parcelRequest.setPhone(order.getCustomerPhone());
        parcelRequest.setCityId(request.getCityId());
        parcelRequest.setAddress(order.getDestinationAddress());
        parcelRequest.setPrice(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0);
        parcelRequest.setStock(request.getStock() != null ? request.getStock() : 1); // Default: stock
        parcelRequest.setOpen(request.getOpen() != null ? request.getOpen() : 1); // Default: open parcel
        parcelRequest.setFragile(request.getFragile() != null ? request.getFragile() : 0); // Default: not fragile
        parcelRequest.setReplace(request.getReplace() != null ? request.getReplace() : 0); // Default: no replacement
        
        // Optional fields
        if (request.getNote() != null) {
            parcelRequest.setNote(request.getNote());
        }
        if (request.getNature() != null) {
            parcelRequest.setNature(request.getNature());
        }
        if (request.getTrackingNumber() != null) {
            parcelRequest.setTrackingNumber(request.getTrackingNumber());
        }
        if (request.getProducts() != null) {
            parcelRequest.setProducts(request.getProducts());
        }
        
        // Send to Ozon Express
        Map<String, Object> result = ozonExpressService.createParcel(
                provider.getCustomerId(),
                provider.getApiKey(),
                parcelRequest
        );
        
        // Extract tracking number from response (can be in different places)
        String trackingNumber = null;
        
        // First, try direct access
        if (result.containsKey("TRACKING-NUMBER")) {
            trackingNumber = (String) result.get("TRACKING-NUMBER");
        } else if (result.containsKey("ADD-PARCEL")) {
            // Try nested structure: ADD-PARCEL -> NEW-PARCEL -> TRACKING-NUMBER
            Object addParcelValue = result.get("ADD-PARCEL");
            if (addParcelValue instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> addParcelMap = (Map<String, Object>) addParcelValue;
                
                if (addParcelMap.containsKey("NEW-PARCEL")) {
                    Object newParcelValue = addParcelMap.get("NEW-PARCEL");
                    if (newParcelValue instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> newParcelMap = (Map<String, Object>) newParcelValue;
                        
                        if (newParcelMap.containsKey("TRACKING-NUMBER")) {
                            trackingNumber = String.valueOf(newParcelMap.get("TRACKING-NUMBER"));
                        }
                    }
                }
                
                // Also check if TRACKING-NUMBER is directly in ADD-PARCEL
                if (trackingNumber == null && addParcelMap.containsKey("TRACKING-NUMBER")) {
                    trackingNumber = String.valueOf(addParcelMap.get("TRACKING-NUMBER"));
                }
            }
        }
        
        // If still not found, try searching all keys recursively
        if (trackingNumber == null) {
            trackingNumber = findTrackingNumberRecursive(result);
        }
        
        // Update order with tracking number if found
        if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
            try {
                order.setOzonTrackingNumber(trackingNumber.trim());
                orderRepository.saveAndFlush(order); // Use saveAndFlush to ensure immediate database write
            } catch (Exception e) {
                // If save fails, throw exception so the user knows the tracking number wasn't saved
                throw new RuntimeException("Order sent to shipping successfully, but failed to save tracking number: " + trackingNumber + ". Error: " + e.getMessage(), e);
            }
        }
        // Note: If tracking number is not found, we don't throw an exception
        // The order was still sent to shipping, just without a tracking number saved
        
        // Add response metadata
        Map<String, Object> response = new HashMap<>(result);
        response.put("orderId", order.getId().toString());
        response.put("orderStatus", order.getStatus().toString());
        if (trackingNumber != null) {
            response.put("trackingNumber", trackingNumber);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/batch/send-to-shipping")
    public ResponseEntity<Map<String, Object>> sendOrdersToShipping(
            @RequestBody BatchSendToShippingRequest request,
            Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        
        if (request.getOrderIds() == null || request.getOrderIds().isEmpty()) {
            throw new IllegalArgumentException("At least one order ID is required");
        }
        
        // Validate required fields
        if (request.getCityId() == null || request.getCityId().trim().isEmpty()) {
            throw new IllegalArgumentException("cityId is required. You can get the list of cities using GET /api/shipping/ozon-express/cities");
        }
        
        List<String> orderIds = request.getOrderIds();
        List<Map<String, Object>> results = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        
        // Group orders by store to optimize provider lookups
        Map<UUID, List<Order>> ordersByStore = new HashMap<>();
        for (String orderIdStr : orderIds) {
            UUID orderUuid;
            try {
                orderUuid = UUID.fromString(orderIdStr);
            } catch (IllegalArgumentException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("orderId", orderIdStr);
                error.put("error", "Invalid order ID format: " + orderIdStr);
                errors.add(error);
                continue;
            }
            
            Order order = orderService.findById(orderUuid)
                    .orElse(null);
            
            if (order == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("orderId", orderIdStr);
                error.put("error", "Order not found");
                errors.add(error);
                continue;
            }
            
            // Verify order belongs to account
            try {
                storeRepository.findByAccountIdAndId(accountId, order.getStore().getId())
                        .orElseThrow(() -> new RuntimeException("Order does not belong to your account"));
            } catch (Exception e) {
                Map<String, Object> error = new HashMap<>();
                error.put("orderId", orderIdStr);
                error.put("error", "Order does not belong to your account");
                errors.add(error);
                continue;
            }
            
            // Skip orders that are already cancelled
            if (order.getStatus() == OrderStatus.CONCLED) {
                Map<String, Object> error = new HashMap<>();
                error.put("orderId", orderIdStr);
                error.put("error", "Cannot send cancelled orders to shipping");
                errors.add(error);
                continue;
            }
            
            ordersByStore.computeIfAbsent(order.getStore().getId(), k -> new ArrayList<>()).add(order);
        }
        
        // Process orders by store
        for (Map.Entry<UUID, List<Order>> entry : ordersByStore.entrySet()) {
            UUID storeId = entry.getKey();
            List<Order> storeOrders = entry.getValue();
            
            // Get Ozon Express provider for the store
            var provider = shippingProviderService.getActiveProviderForStore(storeId, ProviderType.OZON_EXPRESS)
                    .orElse(null);
            
            if (provider == null) {
                for (Order order : storeOrders) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("orderId", order.getId().toString());
                    error.put("error", "Ozon Express provider not configured for this store");
                    errors.add(error);
                }
                continue;
            }
            
            // Process each order
            for (Order orderFromList : storeOrders) {
                try {
                    // Reload order from database to ensure we have the latest data
                    // Use orderRepository directly to force a fresh query
                    final Order order = orderRepository.findById(orderFromList.getId())
                            .orElseThrow(() -> new RuntimeException("Order not found: " + orderFromList.getId()));
                    
                    // Force Hibernate to load all fields by accessing them
                    order.getTotalAmount(); // Force load
                    order.getCurrency(); // Force load
                    order.getCustomerName(); // Force load
                    
                    // Get city ID for this specific order from metadata
                    String orderCityId = request.getCityId(); // Default to request cityId
                    
                    // Try to extract city from order metadata and find its ID
                    if (order.getMetadata() != null && !order.getMetadata().trim().isEmpty()) {
                        try {
                            JsonNode metadataNode = objectMapper.readTree(order.getMetadata());
                            if (metadataNode.has("city")) {
                                String cityName = metadataNode.get("city").asText();
                                Integer cityId = CityMappingUtil.findCityId(cityName);
                                if (cityId != null) {
                                    orderCityId = cityId.toString();
                                }
                            }
                        } catch (Exception e) {
                            // Use default cityId
                        }
                    }
                    
                    // Map order to Ozon Express parcel request
                    OzonExpressService.CreateParcelRequest parcelRequest = new OzonExpressService.CreateParcelRequest();
                    parcelRequest.setReceiver(order.getCustomerName());
                    parcelRequest.setPhone(order.getCustomerPhone());
                    parcelRequest.setCityId(orderCityId);
                    parcelRequest.setAddress(order.getDestinationAddress());
                    
                    // Get price from order - ALWAYS use totalAmount if it exists (even if 0)
                    // Only use metadata as fallback if totalAmount is null
                    Double orderPrice = null;
                    
                    // Force Hibernate to load the field by accessing it
                    BigDecimal totalAmount = order.getTotalAmount();
                    
                    // ALWAYS use totalAmount if it's not null (even if it's 0)
                    if (totalAmount != null) {
                        orderPrice = totalAmount.doubleValue();
                    }
                    
                    // If totalAmount is null, try metadata as fallback
                    if (orderPrice == null && order.getMetadata() != null && !order.getMetadata().trim().isEmpty()) {
                        try {
                            JsonNode metadataNode = objectMapper.readTree(order.getMetadata());
                            
                            // Try multiple possible field names for price
                            String[] priceFieldNames = {"price", "totalAmount", "total_amount", "prix", "amount", "montant", "total", "Prix", "PRIX", "Price", "PRICE", "Amount", "AMOUNT"};
                            for (String fieldName : priceFieldNames) {
                                if (metadataNode.has(fieldName)) {
                                    JsonNode priceNode = metadataNode.get(fieldName);
                                    Double metadataPrice = null;
                                    
                                    if (priceNode.isNumber()) {
                                        metadataPrice = priceNode.asDouble();
                                    } else if (priceNode.isTextual()) {
                                        try {
                                            // Clean the string: remove spaces, replace comma with dot, remove currency symbols
                                            String priceStr = priceNode.asText()
                                                .trim()
                                                .replace(",", ".")
                                                .replace(" ", "")
                                                .replace("€", "")
                                                .replace("$", "")
                                                .replace("MAD", "")
                                                .replace("USD", "")
                                                .replace("EUR", "")
                                                .replace("DH", "")
                                                .replace("dh", "")
                                                .replace("Dh", "");
                                            metadataPrice = Double.parseDouble(priceStr);
                                        } catch (NumberFormatException e) {
                                            continue;
                                        }
                                    }
                                    
                                    if (metadataPrice != null && metadataPrice > 0) {
                                        orderPrice = metadataPrice;
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    
                    // Final check - if still no price found, use 0
                    if (orderPrice == null) {
                        orderPrice = 0.0;
                    }
                    
                    // Round price to nearest integer (delivery dashboard only accepts integer prices)
                    orderPrice = (double) Math.round(orderPrice);
                    parcelRequest.setPrice(orderPrice);
                    
                    parcelRequest.setStock(request.getStock() != null ? request.getStock() : 0); // Default: Pickup (0)
                    parcelRequest.setOpen(request.getOpen() != null ? request.getOpen() : 1); // Default: Open (1)
                    parcelRequest.setFragile(request.getFragile() != null ? request.getFragile() : 0); // Default: No (0)
                    parcelRequest.setReplace(request.getReplace() != null ? request.getReplace() : 0); // Default: No (0)
                    
                    // Optional fields
                    if (request.getNote() != null) {
                        parcelRequest.setNote(request.getNote());
                    }
                    if (request.getNature() != null) {
                        parcelRequest.setNature(request.getNature());
                    }
                    if (request.getProducts() != null) {
                        parcelRequest.setProducts(request.getProducts());
                    }
                    
                    // Send to Ozon Express
                    Map<String, Object> result = ozonExpressService.createParcel(
                            provider.getCustomerId(),
                            provider.getApiKey(),
                            parcelRequest
                    );
                    
                    // Check if we got a tracking number (this is the real indicator of success)
                    // The tracking number can be in different places in the response:
                    // 1. Directly: result.get("TRACKING-NUMBER")
                    // 2. Nested: result.get("ADD-PARCEL").get("NEW-PARCEL").get("TRACKING-NUMBER")
                    String trackingNumber = null;
                    
                    // First, try direct access
                    if (result.containsKey("TRACKING-NUMBER")) {
                        trackingNumber = (String) result.get("TRACKING-NUMBER");
                    } else if (result.containsKey("ADD-PARCEL")) {
                        // Try nested structure: ADD-PARCEL -> NEW-PARCEL -> TRACKING-NUMBER
                        Object addParcelValue = result.get("ADD-PARCEL");
                        if (addParcelValue instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> addParcelMap = (Map<String, Object>) addParcelValue;
                            
                            if (addParcelMap.containsKey("NEW-PARCEL")) {
                                Object newParcelValue = addParcelMap.get("NEW-PARCEL");
                                if (newParcelValue instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> newParcelMap = (Map<String, Object>) newParcelValue;
                                    
                                    if (newParcelMap.containsKey("TRACKING-NUMBER")) {
                                        trackingNumber = String.valueOf(newParcelMap.get("TRACKING-NUMBER"));
                                    }
                                }
                            }
                            
                            // Also check if TRACKING-NUMBER is directly in ADD-PARCEL
                            if (trackingNumber == null && addParcelMap.containsKey("TRACKING-NUMBER")) {
                                trackingNumber = String.valueOf(addParcelMap.get("TRACKING-NUMBER"));
                            }
                        }
                    }
                    
                    // If still not found, try searching all keys recursively
                    if (trackingNumber == null) {
                        trackingNumber = findTrackingNumberRecursive(result);
                    }
                    
                    // Only consider it successful if we have a tracking number
                    if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
                        // Verify the parcel was actually created by checking the price in response
                        Double responsePrice = null;
                        if (result.containsKey("ADD-PARCEL")) {
                            Object addParcelValue = result.get("ADD-PARCEL");
                            if (addParcelValue instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> addParcelMap = (Map<String, Object>) addParcelValue;
                                if (addParcelMap.containsKey("NEW-PARCEL")) {
                                    Object newParcelValue = addParcelMap.get("NEW-PARCEL");
                                    if (newParcelValue instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> newParcelMap = (Map<String, Object>) newParcelValue;
                                        if (newParcelMap.containsKey("PRICE")) {
                                            Object priceObj = newParcelMap.get("PRICE");
                                            if (priceObj instanceof Number) {
                                                responsePrice = ((Number) priceObj).doubleValue();
                                            } else if (priceObj != null) {
                                                try {
                                                    responsePrice = Double.parseDouble(priceObj.toString());
                                                } catch (NumberFormatException e) {
                                                    // Ignore
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        order.setOzonTrackingNumber(trackingNumber.trim());
                        orderRepository.saveAndFlush(order); // Use saveAndFlush to ensure immediate database write
                        
                        Map<String, Object> successResult = new HashMap<>(result);
                        successResult.put("orderId", order.getId().toString());
                        successResult.put("success", true);
                        successResult.put("trackingNumber", trackingNumber);
                        successResult.put("responsePrice", responsePrice);
                        results.add(successResult);
                    } else {
                        // No tracking number = not successful, even if API returned 200
                        Map<String, Object> error = new HashMap<>();
                        error.put("orderId", order.getId().toString());
                        error.put("error", "Ozon Express API did not return a tracking number. Response: " + result.toString());
                        errors.add(error);
                    }
                    
                } catch (Exception e) {
                    // Get order ID from the original order in the list for error reporting
                    UUID orderIdForError = orderFromList.getId();
                    Map<String, Object> error = new HashMap<>();
                    error.put("orderId", orderIdForError.toString());
                    
                    // Extract the actual error message from the exception
                    String errorMessage = e.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "Failed to send order to shipping";
                    }
                    
                    // If it's a RuntimeException from OzonExpressService, it should contain the API error
                    if (e instanceof RuntimeException && e.getMessage() != null && e.getMessage().contains("Ozon Express API error")) {
                        // The error message already contains the API error, use it as-is
                    } else {
                        // For other exceptions, include the exception class name
                        if (e.getCause() != null) {
                            errorMessage += " (" + e.getCause().getClass().getSimpleName() + ")";
                        }
                    }
                    
                    error.put("error", errorMessage);
                    errors.add(error);
                }
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("successful", results);
        response.put("failed", errors);
        response.put("total", orderIds.size());
        response.put("successCount", results.size());
        response.put("failureCount", errors.size());
        
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /**
     * Recursively search for TRACKING-NUMBER in a nested map structure
     */
    @SuppressWarnings("unchecked")
    private String findTrackingNumberRecursive(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Check if this key contains "TRACKING" and the value is a string
            if (key.toUpperCase().contains("TRACKING") && value != null) {
                String strValue = value.toString();
                // Skip empty strings, "null", and non-tracking-number values
                if (!strValue.trim().isEmpty() && 
                    !strValue.equals("null") && 
                    !strValue.equalsIgnoreCase("true") && 
                    !strValue.equalsIgnoreCase("false") &&
                    strValue.length() > 5) { // Tracking numbers are usually longer than 5 characters
                    return strValue.trim();
                }
            }
            
            // Also check for "TRACKING-NUMBER" key specifically (case-insensitive)
            if (key.toUpperCase().equals("TRACKING-NUMBER") && value != null) {
                String strValue = value.toString();
                if (!strValue.trim().isEmpty() && !strValue.equals("null")) {
                    return strValue.trim();
                }
            }
            
            // Recursively search in nested maps
            if (value instanceof Map) {
                String found = findTrackingNumberRecursive((Map<String, Object>) value);
                if (found != null && !found.trim().isEmpty()) {
                    return found;
                }
            }
            
            // Also check in lists/arrays
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                for (Object item : list) {
                    if (item instanceof Map) {
                        String found = findTrackingNumberRecursive((Map<String, Object>) item);
                        if (found != null && !found.trim().isEmpty()) {
                            return found;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static class CreateOrderRequest {
        private String storeId;
        private String customerName;
        private String customerPhone;
        private String destinationAddress;
        private BigDecimal totalAmount;
        private String currency = "USD";
        private String externalOrderId;
        private OrderSource source = OrderSource.API;
        private String productName;
        private String productId;

        // Getters and setters
        public String getStoreId() { return storeId; }
        public void setStoreId(String storeId) { this.storeId = storeId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getCustomerPhone() { return customerPhone; }
        public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
        public String getDestinationAddress() { return destinationAddress; }
        public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        
        public void setTotalAmount(BigDecimal totalAmount) { 
            this.totalAmount = totalAmount; 
        }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getExternalOrderId() { return externalOrderId; }
        public void setExternalOrderId(String externalOrderId) { this.externalOrderId = externalOrderId; }
        public OrderSource getSource() { return source; }
        public void setSource(OrderSource source) { this.source = source; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
    }

    public static class SendToShippingRequest {
        private String cityId; // Required: Ozon Express city ID
        private String note; // Optional: Special instructions
        private String nature; // Optional: Description of contents
        private String trackingNumber; // Optional: Custom tracking number
        private String products; // Optional: JSON string of products
        private Integer stock; // Optional: 1 = stock, 0 = pickup (default: 1)
        private Integer open; // Optional: 1 = Open parcel, 2 = Don't open (default: 1)
        private Integer fragile; // Optional: 1 = Yes, 0 = No (default: 0)
        private Integer replace; // Optional: 1 = Yes, 0 = No (default: 0)

        public String getCityId() { return cityId; }
        public void setCityId(String cityId) { this.cityId = cityId; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public String getNature() { return nature; }
        public void setNature(String nature) { this.nature = nature; }
        public String getTrackingNumber() { return trackingNumber; }
        public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
        public String getProducts() { return products; }
        public void setProducts(String products) { this.products = products; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
        public Integer getOpen() { return open; }
        public void setOpen(Integer open) { this.open = open; }
        public Integer getFragile() { return fragile; }
        public void setFragile(Integer fragile) { this.fragile = fragile; }
        public Integer getReplace() { return replace; }
        public void setReplace(Integer replace) { this.replace = replace; }
    }

    public static class UpdateStatusRequest {
        private OrderStatus status;
        private String note;
        private String changedByUserId;

        public OrderStatus getStatus() { return status; }
        public void setStatus(OrderStatus status) { this.status = status; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public String getChangedByUserId() { return changedByUserId; }
        public void setChangedByUserId(String changedByUserId) { this.changedByUserId = changedByUserId; }
    }

    public static class UpdateOrderRequest {
        private String customerName;
        private String customerPhone;
        private String destinationAddress;
        private String city;
        private BigDecimal totalAmount;
        private String currency;
        private String metadata;
        private String productName;
        private String productId;

        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getCustomerPhone() { return customerPhone; }
        public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
        public String getDestinationAddress() { return destinationAddress; }
        public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getMetadata() { return metadata; }
        public void setMetadata(String metadata) { this.metadata = metadata; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
    }

    public static class BatchSendToShippingRequest {
        private List<String> orderIds;
        private String cityId; // Required: Ozon Express city ID
        private String note; // Optional: Special instructions
        private String nature; // Optional: Description of contents
        private String products; // Optional: JSON string of products
        private Integer stock; // Optional: 1 = stock, 0 = pickup (default: 1)
        private Integer open; // Optional: 1 = Open parcel, 2 = Don't open (default: 1)
        private Integer fragile; // Optional: 1 = Yes, 0 = No (default: 0)
        private Integer replace; // Optional: 1 = Yes, 0 = No (default: 0)

        public List<String> getOrderIds() { return orderIds; }
        public void setOrderIds(List<String> orderIds) { this.orderIds = orderIds; }
        public String getCityId() { return cityId; }
        public void setCityId(String cityId) { this.cityId = cityId; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public String getNature() { return nature; }
        public void setNature(String nature) { this.nature = nature; }
        public String getProducts() { return products; }
        public void setProducts(String products) { this.products = products; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
        public Integer getOpen() { return open; }
        public void setOpen(Integer open) { this.open = open; }
        public Integer getFragile() { return fragile; }
        public void setFragile(Integer fragile) { this.fragile = fragile; }
        public Integer getReplace() { return replace; }
        public void setReplace(Integer replace) { this.replace = replace; }
    }
}


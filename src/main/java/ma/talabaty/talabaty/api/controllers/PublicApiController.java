package ma.talabaty.talabaty.api.controllers;

import jakarta.validation.Valid;
import ma.talabaty.talabaty.api.dtos.OrderDto;
import ma.talabaty.talabaty.api.mappers.OrderMapper;
import ma.talabaty.talabaty.core.security.JwtUser;
import ma.talabaty.talabaty.domain.orders.model.Order;
import ma.talabaty.talabaty.domain.orders.model.OrderSource;
import ma.talabaty.talabaty.domain.orders.model.OrderStatus;
import ma.talabaty.talabaty.domain.orders.service.OrderService;
import ma.talabaty.talabaty.domain.stores.repository.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
public class PublicApiController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final StoreRepository storeRepository;

    public PublicApiController(OrderService orderService, OrderMapper orderMapper, StoreRepository storeRepository) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
        this.storeRepository = storeRepository;
    }

    private UUID getAccountIdFromAuth(Authentication authentication) {
        if (authentication.getPrincipal() instanceof JwtUser) {
            JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
            return UUID.fromString(jwtUser.getAccountId());
        }
        // Fallback for backward compatibility (also works with API key authentication)
        return UUID.fromString(authentication.getName());
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderRequest request, Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        
        // Find first store for the account (or you can add storeId to request)
        var stores = storeRepository.findByAccountId(accountId);
        if (stores.isEmpty()) {
            throw new RuntimeException("No store found for this account");
        }
        UUID storeId = stores.get(0).getId();

        Order order = orderService.createOrder(
                storeId,
                request.getCustomerName(),
                request.getCustomerPhone(),
                request.getDestinationAddress(),
                request.getTotalAmount(),
                request.getCurrency(),
                request.getExternalOrderId(),
                OrderSource.API,
                request.getProductName(),
                request.getProductId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(orderMapper.toDto(order));
    }

    @GetMapping("/orders/{orderId}/status")
    public ResponseEntity<OrderStatusResponse> getOrderStatus(@PathVariable String orderId, Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        UUID orderUuid = UUID.fromString(orderId);
        Order order = orderService.findById(orderUuid)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Verify order belongs to account
        if (!order.getStore().getAccount().getId().equals(accountId)) {
            throw new RuntimeException("Order not found");
        }

        OrderStatusResponse response = new OrderStatusResponse();
        response.setOrderId(order.getId().toString());
        response.setStatus(order.getStatus());
        response.setExternalOrderId(order.getExternalOrderId());
        return ResponseEntity.ok(response);
    }

    public static class CreateOrderRequest {
        private String customerName;
        private String customerPhone;
        private String destinationAddress;
        private BigDecimal totalAmount;
        private String currency = "USD";
        private String externalOrderId;
        private String productName;
        private String productId;

        // Getters and setters
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getCustomerPhone() { return customerPhone; }
        public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
        public String getDestinationAddress() { return destinationAddress; }
        public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getExternalOrderId() { return externalOrderId; }
        public void setExternalOrderId(String externalOrderId) { this.externalOrderId = externalOrderId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
    }

    public static class OrderStatusResponse {
        private String orderId;
        private OrderStatus status;
        private String externalOrderId;

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public OrderStatus getStatus() { return status; }
        public void setStatus(OrderStatus status) { this.status = status; }
        public String getExternalOrderId() { return externalOrderId; }
        public void setExternalOrderId(String externalOrderId) { this.externalOrderId = externalOrderId; }
    }
}


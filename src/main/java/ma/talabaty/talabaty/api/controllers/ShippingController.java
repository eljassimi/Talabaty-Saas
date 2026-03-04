package ma.talabaty.talabaty.api.controllers;

import ma.talabaty.talabaty.core.security.JwtUser;
import ma.talabaty.talabaty.domain.orders.model.Order;
import ma.talabaty.talabaty.domain.orders.repository.OrderRepository;
import ma.talabaty.talabaty.domain.shipping.model.ProviderType;
import ma.talabaty.talabaty.domain.shipping.model.ShippingProvider;
import ma.talabaty.talabaty.domain.shipping.service.OzonExpressService;
import ma.talabaty.talabaty.domain.shipping.service.ShippingProviderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {

    private final ShippingProviderService providerService;
    private final OzonExpressService ozonExpressService;
    private final OrderRepository orderRepository;

    public ShippingController(ShippingProviderService providerService, 
                              OzonExpressService ozonExpressService,
                              OrderRepository orderRepository) {
        this.providerService = providerService;
        this.ozonExpressService = ozonExpressService;
        this.orderRepository = orderRepository;
    }

    private UUID getAccountIdFromAuth(Authentication authentication) {
        if (authentication.getPrincipal() instanceof JwtUser) {
            JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
            return UUID.fromString(jwtUser.getAccountId());
        }
        // Fallback for backward compatibility
        return UUID.fromString(authentication.getName());
    }

    // ========== Shipping Provider Management ==========

    @PostMapping("/providers")
    public ResponseEntity<ShippingProvider> createProvider(@RequestBody CreateProviderRequest request, Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        ShippingProvider provider = providerService.createProvider(
                accountId,
                ProviderType.OZON_EXPRESS,
                request.getCustomerId(),
                request.getApiKey(),
                request.getDisplayName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(provider);
    }

    @GetMapping("/providers")
    public ResponseEntity<List<ShippingProvider>> getProviders(Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        List<ShippingProvider> providers = providerService.getAccountProviders(accountId);
        return ResponseEntity.ok(providers);
    }

    @PutMapping("/providers/{id}")
    public ResponseEntity<ShippingProvider> updateProvider(
            @PathVariable String id,
            @RequestBody UpdateProviderRequest request) {
        ShippingProvider provider = providerService.updateProvider(
                UUID.fromString(id),
                request.getCustomerId(),
                request.getApiKey(),
                request.getDisplayName(),
                request.getActive()
        );
        return ResponseEntity.ok(provider);
    }

    @DeleteMapping("/providers/{id}")
    public ResponseEntity<Void> deleteProvider(@PathVariable String id) {
        providerService.deleteProvider(UUID.fromString(id));
        return ResponseEntity.noContent().build();
    }

    // ========== Ozon Express Operations ==========

    @PostMapping("/ozon-express/parcels")
    public ResponseEntity<Map<String, Object>> createParcel(
            @RequestBody CreateParcelRequest request,
            Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        
        ShippingProvider provider = providerService.getActiveProvider(accountId, ProviderType.OZON_EXPRESS)
                .orElseThrow(() -> new RuntimeException("Ozon Express provider not configured for this account"));

        OzonExpressService.CreateParcelRequest ozonRequest = new OzonExpressService.CreateParcelRequest();
        ozonRequest.setTrackingNumber(request.getTrackingNumber());
        ozonRequest.setReceiver(request.getReceiver());
        ozonRequest.setPhone(request.getPhone());
        ozonRequest.setCityId(request.getCityId());
        ozonRequest.setAddress(request.getAddress());
        ozonRequest.setNote(request.getNote());
        ozonRequest.setPrice(request.getPrice());
        ozonRequest.setNature(request.getNature());
        ozonRequest.setStock(request.getStock() != null ? request.getStock() : 1);
        ozonRequest.setOpen(request.getOpen() != null ? request.getOpen() : 1);
        ozonRequest.setFragile(request.getFragile() != null ? request.getFragile() : 0);
        ozonRequest.setReplace(request.getReplace() != null ? request.getReplace() : 0);
        ozonRequest.setProducts(request.getProducts());

        Map<String, Object> result = ozonExpressService.createParcel(
                provider.getCustomerId(),
                provider.getApiKey(),
                ozonRequest
        );

        // Si un orderId est fourni, mettre à jour l'order avec le tracking number
        if (request.getOrderId() != null && result.containsKey("TRACKING-NUMBER")) {
            Order order = orderRepository.findById(UUID.fromString(request.getOrderId()))
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            order.setOzonTrackingNumber((String) result.get("TRACKING-NUMBER"));
            orderRepository.save(order);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/ozon-express/parcels/info")
    public ResponseEntity<Map<String, Object>> getParcelInfo(
            @RequestBody ParcelInfoRequest request,
            Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        
        ShippingProvider provider = providerService.getActiveProvider(accountId, ProviderType.OZON_EXPRESS)
                .orElseThrow(() -> new RuntimeException("Ozon Express provider not configured for this account"));

        Map<String, Object> result = ozonExpressService.getParcelInfo(
                provider.getCustomerId(),
                provider.getApiKey(),
                request.getTrackingNumber()
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping("/ozon-express/parcels/track")
    public ResponseEntity<Map<String, Object>> trackParcel(
            @RequestBody TrackParcelRequest request,
            Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        
        ShippingProvider provider = providerService.getActiveProvider(accountId, ProviderType.OZON_EXPRESS)
                .orElseThrow(() -> new RuntimeException("Ozon Express provider not configured for this account"));

        // Validate request
        if ((request.getTrackingNumber() == null || request.getTrackingNumber().trim().isEmpty()) &&
            (request.getTrackingNumbers() == null || request.getTrackingNumbers().isEmpty())) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Tracking number is required");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            Map<String, Object> result;
            if (request.getTrackingNumbers() != null && !request.getTrackingNumbers().isEmpty()) {
                // Bulk tracking
                result = ozonExpressService.trackMultipleParcels(
                        provider.getCustomerId(),
                        provider.getApiKey(),
                        request.getTrackingNumbers()
                );
            } else {
                // Single tracking
                result = ozonExpressService.trackParcel(
                        provider.getCustomerId(),
                        provider.getApiKey(),
                        request.getTrackingNumber()
                );
            }

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("status", "error");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/ozon-express/delivery-notes")
    public ResponseEntity<Map<String, Object>> createDeliveryNote(Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        
        ShippingProvider provider = providerService.getActiveProvider(accountId, ProviderType.OZON_EXPRESS)
                .orElseThrow(() -> new RuntimeException("Ozon Express provider not configured for this account"));

        Map<String, Object> result = ozonExpressService.createDeliveryNote(
                provider.getCustomerId(),
                provider.getApiKey()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/ozon-express/delivery-notes/{ref}/parcels")
    public ResponseEntity<Map<String, Object>> addParcelsToDeliveryNote(
            @PathVariable String ref,
            @RequestBody AddParcelsRequest request,
            Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        
        ShippingProvider provider = providerService.getActiveProvider(accountId, ProviderType.OZON_EXPRESS)
                .orElseThrow(() -> new RuntimeException("Ozon Express provider not configured for this account"));

        Map<String, Object> result = ozonExpressService.addParcelsToDeliveryNote(
                provider.getCustomerId(),
                provider.getApiKey(),
                ref,
                request.getTrackingNumbers()
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping("/ozon-express/delivery-notes/{ref}/save")
    public ResponseEntity<Map<String, Object>> saveDeliveryNote(
            @PathVariable String ref,
            Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        
        ShippingProvider provider = providerService.getActiveProvider(accountId, ProviderType.OZON_EXPRESS)
                .orElseThrow(() -> new RuntimeException("Ozon Express provider not configured for this account"));

        Map<String, Object> result = ozonExpressService.saveDeliveryNote(
                provider.getCustomerId(),
                provider.getApiKey(),
                ref
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/ozon-express/cities")
    public ResponseEntity<List<Map<String, Object>>> getCities() {
        List<Map<String, Object>> cities = ozonExpressService.getCities();
        return ResponseEntity.ok(cities);
    }

    // ========== Request DTOs ==========

    public static class CreateProviderRequest {
        private String customerId;
        private String apiKey;
        private String displayName;

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }

    public static class UpdateProviderRequest {
        private String customerId;
        private String apiKey;
        private String displayName;
        private Boolean active;

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    public static class CreateParcelRequest {
        private String orderId; // Optionnel: pour lier à une commande
        private String trackingNumber;
        private String receiver;
        private String phone;
        private String cityId;
        private String address;
        private String note;
        private Double price;
        private String nature;
        private Integer stock;
        private Integer open;
        private Integer fragile;
        private Integer replace;
        private String products;

        // Getters and setters
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getTrackingNumber() { return trackingNumber; }
        public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
        public String getReceiver() { return receiver; }
        public void setReceiver(String receiver) { this.receiver = receiver; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getCityId() { return cityId; }
        public void setCityId(String cityId) { this.cityId = cityId; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public String getNature() { return nature; }
        public void setNature(String nature) { this.nature = nature; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
        public Integer getOpen() { return open; }
        public void setOpen(Integer open) { this.open = open; }
        public Integer getFragile() { return fragile; }
        public void setFragile(Integer fragile) { this.fragile = fragile; }
        public Integer getReplace() { return replace; }
        public void setReplace(Integer replace) { this.replace = replace; }
        public String getProducts() { return products; }
        public void setProducts(String products) { this.products = products; }
    }

    public static class ParcelInfoRequest {
        private String trackingNumber;

        public String getTrackingNumber() { return trackingNumber; }
        public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    }

    public static class TrackParcelRequest {
        private String trackingNumber;
        private List<String> trackingNumbers; // Pour bulk tracking

        public String getTrackingNumber() { return trackingNumber; }
        public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
        public List<String> getTrackingNumbers() { return trackingNumbers; }
        public void setTrackingNumbers(List<String> trackingNumbers) { this.trackingNumbers = trackingNumbers; }
    }

    public static class AddParcelsRequest {
        private List<String> trackingNumbers;

        public List<String> getTrackingNumbers() { return trackingNumbers; }
        public void setTrackingNumbers(List<String> trackingNumbers) { this.trackingNumbers = trackingNumbers; }
    }
}


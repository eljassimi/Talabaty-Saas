package ma.talabaty.talabaty.api.controllers;

import ma.talabaty.talabaty.api.dtos.StoreDto;
import ma.talabaty.talabaty.api.mappers.StoreMapper;
import ma.talabaty.talabaty.core.security.AuthenticationHelper;
import ma.talabaty.talabaty.core.security.JwtTokenProvider;
import ma.talabaty.talabaty.domain.shipping.model.ProviderType;
import ma.talabaty.talabaty.domain.shipping.model.ShippingProvider;
import ma.talabaty.talabaty.domain.shipping.service.ShippingProviderService;
import ma.talabaty.talabaty.core.security.PermissionChecker;
import ma.talabaty.talabaty.domain.stores.model.Store;
import ma.talabaty.talabaty.domain.stores.model.StoreSettings;
import ma.talabaty.talabaty.domain.orders.repository.OrderRepository;
import ma.talabaty.talabaty.domain.stores.service.StoreService;
import ma.talabaty.talabaty.domain.whatsapp.WhatsAppService;
import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.model.UserRole;
import ma.talabaty.talabaty.domain.users.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;
    private final StoreMapper storeMapper;
    private final JwtTokenProvider tokenProvider;
    private final ShippingProviderService shippingProviderService;
    private final UserRepository userRepository;
    private final PermissionChecker permissionChecker;
    private final WhatsAppService whatsAppService;
    private final OrderRepository orderRepository;

    public StoreController(StoreService storeService, StoreMapper storeMapper, JwtTokenProvider tokenProvider,
                          ShippingProviderService shippingProviderService, UserRepository userRepository,
                          PermissionChecker permissionChecker, WhatsAppService whatsAppService,
                          OrderRepository orderRepository) {
        this.storeService = storeService;
        this.storeMapper = storeMapper;
        this.tokenProvider = tokenProvider;
        this.shippingProviderService = shippingProviderService;
        this.userRepository = userRepository;
        this.permissionChecker = permissionChecker;
        this.whatsAppService = whatsAppService;
        this.orderRepository = orderRepository;
    }


    @PostMapping
    public ResponseEntity<StoreDto> createStore(@RequestBody CreateStoreRequest request, Authentication authentication) {
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check permission
        if (!permissionChecker.canCreateStore(user.getRole())) {
            throw new AccessDeniedException("You don't have permission to create stores");
        }
        
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID managerId = request.getManagerId() != null && !request.getManagerId().isEmpty() 
                ? UUID.fromString(request.getManagerId()) 
                : null;
        Store store = storeService.createStore(accountId, request.getName(), managerId);
        if (request.getLogoUrl() != null) {
            store.setLogoUrl(request.getLogoUrl());
        }
        if (request.getColor() != null) {
            store.setColor(request.getColor());
        }
        store = storeService.save(store);
        return ResponseEntity.status(HttpStatus.CREATED).body(storeMapper.toDto(store));
    }

    @GetMapping
    public ResponseEntity<List<StoreDto>> getStores(Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        
        // Get user role from database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserRole userRole = user.getRole();
        
        // Get stores based on user role
        List<Store> stores = storeService.findStoresForUser(accountId, userId, userRole);
        List<StoreDto> dtos = stores.stream()
                .map(storeMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreDto> getStore(@PathVariable String id, Authentication authentication) {
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Try to find store by ID (not restricted to account)
        Store store = storeService.findById(UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Store not found"));
        
        // Check permission to view this store (includes team membership check)
        if (!permissionChecker.canViewStore(user.getRole(), userId, store)) {
            throw new AccessDeniedException("You don't have permission to view this store");
        }
        
        return ResponseEntity.ok(storeMapper.toDto(store));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoreDto> updateStore(@PathVariable String id, @RequestBody UpdateStoreRequest request, Authentication authentication) {
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        Store store = storeService.findByAccountIdAndId(accountId, UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Store not found"));
        
        // Check permission
        if (!permissionChecker.canUpdateStore(user.getRole(), userId, store)) {
            throw new AccessDeniedException("You don't have permission to update this store");
        }
        
        store = storeService.updateStore(UUID.fromString(id), request.getName(), request.getTimezone());
        if (request.getLogoUrl() != null) {
            store.setLogoUrl(request.getLogoUrl());
        }
        if (request.getColor() != null) {
            store.setColor(request.getColor());
        }
        store = storeService.save(store);
        return ResponseEntity.ok(storeMapper.toDto(store));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStore(@PathVariable String id, Authentication authentication) {
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check permission
        if (!permissionChecker.canDeleteStore(user.getRole())) {
            throw new AccessDeniedException("You don't have permission to delete stores");
        }
        
        storeService.deleteStore(UUID.fromString(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/shipping-providers")
    public ResponseEntity<ShippingProvider> createShippingProvider(
            @PathVariable String id,
            @RequestBody CreateShippingProviderRequest request,
            Authentication authentication) {
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check permission
        if (!permissionChecker.canManageShippingProviders(user.getRole())) {
            throw new AccessDeniedException("You don't have permission to manage shipping providers");
        }
        
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID storeId = UUID.fromString(id);
        
        // Verify store belongs to account and user has access
        Store store = storeService.findByAccountIdAndId(accountId, storeId)
                .orElseThrow(() -> new RuntimeException("Store not found or does not belong to your account"));
        
        if (!permissionChecker.canViewStore(user.getRole(), userId, store)) {
            throw new AccessDeniedException("You don't have permission to access this store");
        }
        
        ShippingProvider provider = shippingProviderService.createProviderForStore(
                accountId,
                storeId,
                ProviderType.OZON_EXPRESS,
                request.getCustomerId(),
                request.getApiKey(),
                request.getDisplayName()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(provider);
    }

    @GetMapping("/{id}/shipping-providers")
    public ResponseEntity<List<ShippingProvider>> getStoreShippingProviders(
            @PathVariable String id,
            Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID storeId = UUID.fromString(id);
        
        // Verify store belongs to account
        storeService.findByAccountIdAndId(accountId, storeId)
                .orElseThrow(() -> new RuntimeException("Store not found or does not belong to your account"));
        
        List<ShippingProvider> providers = shippingProviderService.getStoreProviders(storeId);
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/{id}/whatsapp-settings")
    public ResponseEntity<WhatsAppSettingsResponse> getWhatsAppSettings(@PathVariable String id, Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        Store store = storeService.findByAccountIdAndId(accountId, UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Store not found"));
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!permissionChecker.canViewStore(user.getRole(), userId, store)) {
            throw new AccessDeniedException("You don't have permission to access this store");
        }
        StoreSettings settings = storeService.getOrCreateSettings(store);
        WhatsAppSettingsResponse res = new WhatsAppSettingsResponse();
        res.setWhatsappAutomationEnabled(settings.isWhatsappAutomationEnabled());
        res.setWhatsappTemplateConfirmed(settings.getWhatsappTemplateConfirmed() != null ? settings.getWhatsappTemplateConfirmed() : "");
        res.setWhatsappTemplateDelivered(settings.getWhatsappTemplateDelivered() != null ? settings.getWhatsappTemplateDelivered() : "");
        res.setSendingConfigured(whatsAppService.isConfigured());
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/{id}/whatsapp-settings")
    public ResponseEntity<WhatsAppSettingsResponse> updateWhatsAppSettings(
            @PathVariable String id,
            @RequestBody WhatsAppSettingsRequest request,
            Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        Store store = storeService.findByAccountIdAndId(accountId, UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Store not found"));
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!permissionChecker.canUpdateStore(user.getRole(), userId, store)) {
            throw new AccessDeniedException("You don't have permission to update this store");
        }
        UUID storeUuid = UUID.fromString(id);
        storeService.updateWhatsAppAutomation(
                storeUuid,
                request.getWhatsappAutomationEnabled(),
                request.getWhatsappTemplateConfirmed(),
                request.getWhatsappTemplateDelivered()
        );
        Store updated = storeService.findById(storeUuid).orElse(store);
        StoreSettings settings = storeService.getOrCreateSettings(updated);
        WhatsAppSettingsResponse res = new WhatsAppSettingsResponse();
        res.setWhatsappAutomationEnabled(settings.isWhatsappAutomationEnabled());
        res.setWhatsappTemplateConfirmed(settings.getWhatsappTemplateConfirmed() != null ? settings.getWhatsappTemplateConfirmed() : "");
        res.setWhatsappTemplateDelivered(settings.getWhatsappTemplateDelivered() != null ? settings.getWhatsappTemplateDelivered() : "");
        res.setSendingConfigured(whatsAppService.isConfigured());
        return ResponseEntity.ok(res);
    }

    /**
     * WhatsApp link status for a specific store.
     * This lets each store have its own WhatsApp session (phone/account) when using the free bridge.
     */
    @GetMapping("/{id}/whatsapp-link-status")
    public ResponseEntity<Map<String, Object>> getStoreWhatsAppLinkStatus(
            @PathVariable String id,
            Authentication authentication
    ) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID storeUuid = UUID.fromString(id);
        Store store = storeService.findByAccountIdAndId(accountId, storeUuid)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!permissionChecker.canViewStore(user.getRole(), userId, store)) {
            throw new AccessDeniedException("You don't have permission to access this store");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("configured", whatsAppService.isConfigured());
        if (whatsAppService.isTwilioConfigured()) {
            body.put("provider", "twilio");
            body.put("ready", true);
            return ResponseEntity.ok(body);
        }
        if (whatsAppService.isLocalBridgeConfigured()) {
            body.put("provider", "bridge");
            Map<String, Object> bridge = whatsAppService.getBridgeLinkStatus(storeUuid);
            body.put("ready", bridge.getOrDefault("ready", false));
            if (bridge.containsKey("qr")) {
                body.put("qr", bridge.get("qr"));
            }
            body.put("initializing", bridge.getOrDefault("initializing", false));
            if (bridge.containsKey("error")) {
                body.put("error", bridge.get("error"));
            }
            if (bridge.containsKey("bridgeError")) {
                body.put("bridgeError", bridge.get("bridgeError"));
            }
            if (bridge.containsKey("reason")) {
                body.put("reason", bridge.get("reason"));
            }
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.ok(body);
    }

    /** Send a promotion message to all customers who have ordered from this store. */
    @PostMapping("/{id}/whatsapp-broadcast")
    public ResponseEntity<Map<String, Object>> whatsappBroadcast(
            @PathVariable String id,
            @RequestBody WhatsAppBroadcastRequest request,
            Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        Store store = storeService.findByAccountIdAndId(accountId, UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Store not found"));
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!permissionChecker.canUpdateStore(user.getRole(), userId, store)) {
            throw new AccessDeniedException("You don't have permission to update this store");
        }
        if (!whatsAppService.isConfigured()) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "WhatsApp is not configured. Set Twilio credentials or whatsapp.local.url (free bridge).");
            return ResponseEntity.badRequest().body(err);
        }
        String message = request.getMessage();
        if (message == null || message.isBlank()) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Message is required.");
            return ResponseEntity.badRequest().body(err);
        }
        UUID storeUuid = UUID.fromString(id);
        List<String> phones = orderRepository.findDistinctCustomerPhonesByStoreId(storeUuid);
        int sent = 0;
        int failed = 0;
        java.util.List<String> failedReasons = new java.util.ArrayList<>();
        for (String phone : phones) {
                try {
                String err = whatsAppService.sendWithReason(storeUuid, phone, message);
                if (err == null) {
                    sent++;
                } else {
                    failed++;
                    failedReasons.add(phone + ": " + err);
                }
            } catch (Exception e) {
                failed++;
                failedReasons.add(phone + ": " + (e.getMessage() != null ? e.getMessage() : "Error"));
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", phones.size());
        result.put("sent", sent);
        result.put("failed", failed);
        if (!failedReasons.isEmpty()) {
            result.put("failedReasons", failedReasons);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/support-revenue-settings")
    public ResponseEntity<Map<String, Object>> getSupportRevenueSettings(
            @PathVariable String id,
            Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        Store store = storeService.findByAccountIdAndId(accountId, UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Store not found"));
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!permissionChecker.canViewStore(user.getRole(), userId, store)) {
            throw new AccessDeniedException("You don't have permission to view this store");
        }
        StoreSettings settings = storeService.getOrCreateSettings(store);
        Map<String, Object> body = new HashMap<>();
        body.put("pricePerOrderConfirmedMad", settings.getPricePerOrderConfirmedMad());
        body.put("pricePerOrderDeliveredMad", settings.getPricePerOrderDeliveredMad());
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/{id}/support-revenue-settings")
    public ResponseEntity<Map<String, Object>> updateSupportRevenueSettings(
            @PathVariable String id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        Store store = storeService.findByAccountIdAndId(accountId, UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Store not found"));
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!permissionChecker.canUpdateStore(user.getRole(), userId, store)) {
            throw new AccessDeniedException("You don't have permission to update this store");
        }
        BigDecimal priceConfirmed = request.get("pricePerOrderConfirmedMad") != null
                ? new BigDecimal(request.get("pricePerOrderConfirmedMad").toString()) : null;
        BigDecimal priceDelivered = request.get("pricePerOrderDeliveredMad") != null
                ? new BigDecimal(request.get("pricePerOrderDeliveredMad").toString()) : null;
        storeService.updateSupportRevenuePrices(UUID.fromString(id), priceConfirmed, priceDelivered);
        Store updated = storeService.findById(UUID.fromString(id)).orElse(store);
        StoreSettings settings = storeService.getOrCreateSettings(updated);
        Map<String, Object> body = new HashMap<>();
        body.put("pricePerOrderConfirmedMad", settings.getPricePerOrderConfirmedMad());
        body.put("pricePerOrderDeliveredMad", settings.getPricePerOrderDeliveredMad());
        return ResponseEntity.ok(body);
    }

    // Inner classes for request DTOs
    public static class CreateStoreRequest {
        private String name;
        private String managerId;
        private String logoUrl;
        private String color;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getManagerId() {
            return managerId;
        }

        public void setManagerId(String managerId) {
            this.managerId = managerId;
        }

        public String getLogoUrl() {
            return logoUrl;
        }

        public void setLogoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    public static class UpdateStoreRequest {
        private String name;
        private String timezone;
        private String logoUrl;
        private String color;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }

        public String getLogoUrl() {
            return logoUrl;
        }

        public void setLogoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    public static class CreateShippingProviderRequest {
        private String customerId;
        private String apiKey;
        private String displayName;

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }

    public static class WhatsAppBroadcastRequest {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class WhatsAppSettingsRequest {
        private Boolean whatsappAutomationEnabled;
        private String whatsappTemplateConfirmed;
        private String whatsappTemplateDelivered;

        public Boolean getWhatsappAutomationEnabled() { return whatsappAutomationEnabled; }
        public void setWhatsappAutomationEnabled(Boolean whatsappAutomationEnabled) { this.whatsappAutomationEnabled = whatsappAutomationEnabled; }
        public String getWhatsappTemplateConfirmed() { return whatsappTemplateConfirmed; }
        public void setWhatsappTemplateConfirmed(String whatsappTemplateConfirmed) { this.whatsappTemplateConfirmed = whatsappTemplateConfirmed; }
        public String getWhatsappTemplateDelivered() { return whatsappTemplateDelivered; }
        public void setWhatsappTemplateDelivered(String whatsappTemplateDelivered) { this.whatsappTemplateDelivered = whatsappTemplateDelivered; }
    }

    public static class WhatsAppSettingsResponse {
        private boolean whatsappAutomationEnabled;
        private String whatsappTemplateConfirmed;
        private String whatsappTemplateDelivered;
        private boolean sendingConfigured;

        public boolean isWhatsappAutomationEnabled() { return whatsappAutomationEnabled; }
        public void setWhatsappAutomationEnabled(boolean whatsappAutomationEnabled) { this.whatsappAutomationEnabled = whatsappAutomationEnabled; }
        public String getWhatsappTemplateConfirmed() { return whatsappTemplateConfirmed; }
        public void setWhatsappTemplateConfirmed(String whatsappTemplateConfirmed) { this.whatsappTemplateConfirmed = whatsappTemplateConfirmed; }
        public String getWhatsappTemplateDelivered() { return whatsappTemplateDelivered; }
        public void setWhatsappTemplateDelivered(String whatsappTemplateDelivered) { this.whatsappTemplateDelivered = whatsappTemplateDelivered; }
        public boolean isSendingConfigured() { return sendingConfigured; }
        public void setSendingConfigured(boolean sendingConfigured) { this.sendingConfigured = sendingConfigured; }
    }
}


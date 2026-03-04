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
import ma.talabaty.talabaty.domain.stores.service.StoreService;
import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.model.UserRole;
import ma.talabaty.talabaty.domain.users.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;
    private final StoreMapper storeMapper;
    private final JwtTokenProvider tokenProvider;
    private final ShippingProviderService shippingProviderService;
    private final UserRepository userRepository;
    private final PermissionChecker permissionChecker;

    public StoreController(StoreService storeService, StoreMapper storeMapper, JwtTokenProvider tokenProvider,
                          ShippingProviderService shippingProviderService, UserRepository userRepository,
                          PermissionChecker permissionChecker) {
        this.storeService = storeService;
        this.storeMapper = storeMapper;
        this.tokenProvider = tokenProvider;
        this.shippingProviderService = shippingProviderService;
        this.userRepository = userRepository;
        this.permissionChecker = permissionChecker;
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
}


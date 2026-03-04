package ma.talabaty.talabaty.api.controllers;

import ma.talabaty.talabaty.core.security.AuthenticationHelper;
import ma.talabaty.talabaty.domain.youcan.model.YouCanStore;
import ma.talabaty.talabaty.domain.youcan.service.YouCanOAuthService;
import ma.talabaty.talabaty.domain.youcan.service.YouCanOrderSyncService;
import ma.talabaty.talabaty.domain.youcan.repository.YouCanStoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/youcan")
public class YouCanController {

    private final YouCanOAuthService youCanOAuthService;
    private final YouCanOrderSyncService youCanOrderSyncService;
    private final YouCanStoreRepository youCanStoreRepository;

    public YouCanController(
            YouCanOAuthService youCanOAuthService,
            YouCanOrderSyncService youCanOrderSyncService,
            YouCanStoreRepository youCanStoreRepository) {
        this.youCanOAuthService = youCanOAuthService;
        this.youCanOrderSyncService = youCanOrderSyncService;
        this.youCanStoreRepository = youCanStoreRepository;
    }

    /**
     * Initiate OAuth flow - redirect merchant to YouCan authorization page
     */
    @GetMapping("/connect/{storeId}")
    public ResponseEntity<Map<String, Object>> initiateConnection(
            @PathVariable String storeId,
            Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID storeUuid;
        
        try {
            storeUuid = UUID.fromString(storeId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid storeId format: '" + storeId + "'. Please provide a valid UUID.", e);
        }

        String authorizationUrl = youCanOAuthService.getAuthorizationUrl(accountId, storeUuid);

        Map<String, Object> response = new HashMap<>();
        response.put("authorizationUrl", authorizationUrl);
        response.put("message", "Redirect user to this URL to authorize YouCan store connection");

        return ResponseEntity.ok(response);
    }

    /**
     * OAuth callback endpoint - receives authorization code from YouCan
     */
    @GetMapping("/oauth/callback")
    public ResponseEntity<?> handleOAuthCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description) {
        
        // Handle OAuth errors from YouCan (e.g., invalid_scope, access_denied)
        if (error != null) {
            String[] stateParts = state != null ? state.split(":") : new String[0];
            String storeId = stateParts.length > 1 ? stateParts[1] : null;
            
            String errorMessage = error_description != null ? error_description : 
                (error.equals("access_denied") ? "Authorization was denied by user" : "OAuth error: " + error);
            
            if (storeId != null) {
                try {
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .header("Location", "http://localhost:3000/stores/" + storeId + "?youcan=error&message=" + 
                                    java.net.URLEncoder.encode(errorMessage, java.nio.charset.StandardCharsets.UTF_8))
                            .build();
                } catch (Exception e) {
                    // Fall through to JSON response
                }
            }
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", errorMessage);
            errorResponse.put("error_code", error);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        // Validate required parameters
        if (code == null || state == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Missing required parameters: code or state");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        try {
            YouCanStore youCanStore = youCanOAuthService.handleOAuthCallback(code, state);
            
            // Extract store ID from state to redirect to frontend
            String[] stateParts = state.split(":");
            String storeId = stateParts.length > 1 ? stateParts[1] : null;
            
            // Redirect to frontend store detail page with success message
            if (storeId != null) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", "http://localhost:3000/stores/" + storeId + "?youcan=connected")
                        .build();
            }
            
            // Fallback: return JSON response if redirect not possible
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "YouCan store connected successfully");
            response.put("youcanStoreId", youCanStore.getId());
            response.put("youcanStoreName", youCanStore.getYoucanStoreName());
            response.put("youcanStoreDomain", youCanStore.getYoucanStoreDomain());
            response.put("storeId", storeId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Handle OAuth errors from YouCan (e.g., invalid_scope, access_denied)
            String[] stateParts = state.split(":");
            String storeId = stateParts.length > 1 ? stateParts[1] : null;
            
            // Check if this is an OAuth error (from query params)
            String errorParam = null;
            String errorDescription = null;
            try {
                errorParam = ((org.springframework.web.context.request.ServletWebRequest) 
                    org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes())
                    .getRequest().getParameter("error");
                errorDescription = ((org.springframework.web.context.request.ServletWebRequest) 
                    org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes())
                    .getRequest().getParameter("error_description");
            } catch (Exception ignored) {
                // If we can't get params, use exception message
            }
            
            String errorMessage = errorDescription != null ? errorDescription : e.getMessage();
            
            if (storeId != null) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", "http://localhost:3000/stores/" + storeId + "?youcan=error&message=" + 
                                java.net.URLEncoder.encode(errorMessage, java.nio.charset.StandardCharsets.UTF_8))
                        .build();
            }
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", errorMessage);
            if (errorParam != null) {
                errorResponse.put("error_code", errorParam);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * List all connected YouCan stores for the authenticated account
     */
    @GetMapping("/stores")
    public ResponseEntity<List<Map<String, Object>>> listConnectedStores(Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        
        List<YouCanStore> youCanStores = youCanStoreRepository.findActiveByAccountId(accountId);

        List<Map<String, Object>> stores = youCanStores.stream()
                .map(store -> {
                    Map<String, Object> storeMap = new HashMap<>();
                    storeMap.put("id", store.getId());
                    storeMap.put("youcanStoreId", store.getYoucanStoreId());
                    storeMap.put("youcanStoreName", store.getYoucanStoreName());
                    storeMap.put("youcanStoreDomain", store.getYoucanStoreDomain());
                    storeMap.put("storeId", store.getStore().getId());
                    storeMap.put("storeName", store.getStore().getName());
                    storeMap.put("active", store.isActive());
                    storeMap.put("lastSyncAt", store.getLastSyncAt());
                    storeMap.put("createdAt", store.getCreatedAt());
                    return storeMap;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(stores);
    }

    /**
     * Sync orders from a connected YouCan store
     */
    @PostMapping("/stores/{youcanStoreId}/sync")
    public ResponseEntity<Map<String, Object>> syncOrders(
            @PathVariable String youcanStoreId,
            Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID youcanStoreUuid;
        
        try {
            youcanStoreUuid = UUID.fromString(youcanStoreId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid youcanStoreId format: '" + youcanStoreId + "'. Please provide a valid UUID.", e);
        }

        // Verify the YouCan store belongs to the authenticated account
        YouCanStore youCanStore = youCanStoreRepository.findById(youcanStoreUuid)
                .orElseThrow(() -> new RuntimeException("YouCan store not found"));

        if (!youCanStore.getAccount().getId().equals(accountId)) {
            throw new RuntimeException("YouCan store does not belong to your account");
        }

        try {
            int syncedCount = youCanOrderSyncService.syncOrdersFromYouCanStore(youcanStoreUuid);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Orders synced successfully");
            response.put("syncedCount", syncedCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to sync orders: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Disconnect a YouCan store
     */
    @DeleteMapping("/stores/{youcanStoreId}")
    public ResponseEntity<Map<String, Object>> disconnectStore(
            @PathVariable String youcanStoreId,
            Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID youcanStoreUuid;
        
        try {
            youcanStoreUuid = UUID.fromString(youcanStoreId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid youcanStoreId format: '" + youcanStoreId + "'. Please provide a valid UUID.", e);
        }

        // Verify the YouCan store belongs to the authenticated account
        YouCanStore youCanStore = youCanStoreRepository.findById(youcanStoreUuid)
                .orElseThrow(() -> new RuntimeException("YouCan store not found"));

        if (!youCanStore.getAccount().getId().equals(accountId)) {
            throw new RuntimeException("YouCan store does not belong to your account");
        }

        youCanStore.setActive(false);
        youCanStoreRepository.save(youCanStore);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "YouCan store disconnected successfully");

        return ResponseEntity.ok(response);
    }
}


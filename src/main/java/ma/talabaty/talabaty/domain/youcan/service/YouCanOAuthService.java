package ma.talabaty.talabaty.domain.youcan.service;

import ma.talabaty.talabaty.domain.accounts.model.Account;
import ma.talabaty.talabaty.domain.accounts.repository.AccountRepository;
import ma.talabaty.talabaty.domain.stores.model.Store;
import ma.talabaty.talabaty.domain.stores.repository.StoreRepository;
import ma.talabaty.talabaty.domain.youcan.model.YouCanStore;
import ma.talabaty.talabaty.domain.youcan.repository.YouCanStoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class YouCanOAuthService {

    private final YouCanStoreRepository youCanStoreRepository;
    private final AccountRepository accountRepository;
    private final StoreRepository storeRepository;
    private final RestTemplate restTemplate;

    @Value("${youcan.oauth.client-id}")
    private String clientId;

    @Value("${youcan.oauth.client-secret}")
    private String clientSecret;

    @Value("${youcan.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${youcan.oauth.authorize-url:https://youcan.shop/oauth/authorize}")
    private String authorizeUrl;

    @Value("${youcan.oauth.token-url:https://youcan.shop/oauth/token}")
    private String tokenUrl;

    @Value("${youcan.oauth.scopes:orders:read orders:write}")
    private String scopes;

    public YouCanOAuthService(
            YouCanStoreRepository youCanStoreRepository,
            AccountRepository accountRepository,
            StoreRepository storeRepository,
            RestTemplate restTemplate) {
        this.youCanStoreRepository = youCanStoreRepository;
        this.accountRepository = accountRepository;
        this.storeRepository = storeRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Generate the OAuth authorization URL for a merchant to connect their YouCan store
     */
    public String getAuthorizationUrl(UUID accountId, UUID storeId) {
        String state = accountId.toString() + ":" + storeId.toString(); // Encode account and store IDs in state
        
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(authorizeUrl)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", state);
        
        // YouCan uses scope[] array format (as per documentation: scope[]=*)
        // Only add scope parameter if scopes are configured and not empty
        if (scopes != null && !scopes.trim().isEmpty()) {
            String[] scopeArray = scopes.trim().split("\\s+");
            for (String scope : scopeArray) {
                if (!scope.isEmpty()) {
                    uriBuilder.queryParam("scope[]", scope);
                }
            }
        }
        
        return uriBuilder.build().toUriString();
    }

    /**
     * Exchange authorization code for access token and store credentials
     */
    public YouCanStore handleOAuthCallback(String code, String state) {
        System.out.println("=== Starting OAuth callback handling ===");
        System.out.println("State: " + state);
        
        // Extract account and store IDs from state
        String[] stateParts = state.split(":");
        if (stateParts.length != 2) {
            throw new IllegalArgumentException("Invalid state parameter");
        }
        
        UUID accountId = UUID.fromString(stateParts[0]);
        UUID storeId = UUID.fromString(stateParts[1]);
        System.out.println("Account ID: " + accountId);
        System.out.println("Store ID: " + storeId);

        // Exchange code for token
        System.out.println("Exchanging code for token...");
        Map<String, Object> tokenResponse = exchangeCodeForToken(code);
        System.out.println("Token exchange completed");
        System.out.println("Token response keys: " + tokenResponse.keySet());

        // Extract token information
        // Response format: { "token_type": "Bearer", "expires_in": 1295999, "access_token": "...", "refresh_token": "..." }
        System.out.println("Extracting tokens from response...");
        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        
        // Handle expires_in which can be Integer or Long
        // Use Long to avoid integer overflow (tokens can last up to 1 year = ~31 million seconds)
        Long expiresInLong = null;
        Object expiresInObj = tokenResponse.get("expires_in");
        if (expiresInObj != null) {
            if (expiresInObj instanceof Integer) {
                expiresInLong = ((Integer) expiresInObj).longValue();
            } else if (expiresInObj instanceof Long) {
                expiresInLong = (Long) expiresInObj;
            } else if (expiresInObj instanceof Number) {
                expiresInLong = ((Number) expiresInObj).longValue();
            } else {
                System.err.println("WARNING: expires_in is not a number: " + expiresInObj.getClass().getName());
            }
        }
        
        // Convert to Integer for OffsetDateTime calculation (but handle overflow)
        Integer expiresIn = null;
        if (expiresInLong != null) {
            // Check if value fits in Integer range
            if (expiresInLong > Integer.MAX_VALUE) {
                System.err.println("WARNING: expires_in (" + expiresInLong + ") exceeds Integer.MAX_VALUE, using MAX_VALUE");
                expiresIn = Integer.MAX_VALUE;
            } else if (expiresInLong < Integer.MIN_VALUE) {
                System.err.println("WARNING: expires_in (" + expiresInLong + ") is less than Integer.MIN_VALUE, using 0");
                expiresIn = 0;
            } else {
                expiresIn = expiresInLong.intValue();
            }
        }
        
        if (accessToken == null) {
            System.err.println("ERROR: access_token is null in response!");
            System.err.println("Full response: " + tokenResponse);
            throw new RuntimeException("Access token not found in token response");
        }
        
        System.out.println("Access token extracted (length: " + accessToken.length() + ")");
        System.out.println("Refresh token: " + (refreshToken != null ? "present" : "null"));
        System.out.println("Expires in: " + expiresIn + " seconds");
        
        // Note: YouCan API response doesn't include scope field, use requested scopes instead
        String grantedScopes = scopes; // Use the requested scopes since response doesn't include them
        System.out.println("Using scopes: " + grantedScopes);
        
        // Get store information from YouCan API
        System.out.println("Attempting to get store info from YouCan API...");
        Map<String, Object> storeInfo;
        try {
            storeInfo = getStoreInfo(accessToken);
            System.out.println("Successfully retrieved store info from YouCan API");
            System.out.println("Store info: " + storeInfo);
        } catch (Exception e) {
            System.err.println("WARNING: Failed to get store info from YouCan API: " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getName());
            e.printStackTrace();
            // If we can't get store info, we can still save the connection with minimal info
            // This allows the connection to be saved even if the API call fails
            System.out.println("Using fallback store info (connection will still be saved)");
            storeInfo = new HashMap<>();
            storeInfo.put("id", "unknown_" + System.currentTimeMillis());
            storeInfo.put("domain", null);
            storeInfo.put("name", "YouCan Store");
        }
        
        String youcanStoreId = String.valueOf(storeInfo.get("id"));
        String youcanStoreDomain = storeInfo.get("domain") != null ? String.valueOf(storeInfo.get("domain")) : null;
        String youcanStoreName = storeInfo.get("name") != null ? String.valueOf(storeInfo.get("name")) : "YouCan Store";

        // Calculate token expiration
        OffsetDateTime tokenExpiresAt = null;
        if (expiresIn != null) {
            tokenExpiresAt = OffsetDateTime.now().plusSeconds(expiresIn);
        }

        // Get account and store entities
        System.out.println("Looking up account and store...");
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    System.err.println("Account not found: " + accountId);
                    return new RuntimeException("Account not found: " + accountId);
                });
        System.out.println("Account found: " + account.getName());
        
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> {
                    System.err.println("Store not found: " + storeId);
                    return new RuntimeException("Store not found: " + storeId);
                });
        System.out.println("Store found: " + store.getName());

        // Check if YouCan store already exists for this store
        System.out.println("Checking for existing YouCan store connection...");
        YouCanStore youCanStore = youCanStoreRepository.findByStoreId(storeId)
                .orElse(new YouCanStore());
        
        if (youCanStore.getId() != null) {
            System.out.println("Updating existing YouCan store connection: " + youCanStore.getId());
        } else {
            System.out.println("Creating new YouCan store connection");
        }

        // Update or create YouCan store credentials
        System.out.println("Setting YouCan store properties...");
        youCanStore.setAccount(account);
        youCanStore.setStore(store);
        youCanStore.setYoucanStoreId(youcanStoreId);
        youCanStore.setYoucanStoreDomain(youcanStoreDomain);
        youCanStore.setYoucanStoreName(youcanStoreName);
        youCanStore.setAccessToken(accessToken);
        youCanStore.setRefreshToken(refreshToken);
        youCanStore.setTokenExpiresAt(tokenExpiresAt);
        youCanStore.setScopes(grantedScopes);
        youCanStore.setActive(true);
        System.out.println("Properties set. Saving to database...");

        try {
            YouCanStore savedStore = youCanStoreRepository.save(youCanStore);
            System.out.println("=== YouCan store saved successfully ===");
            System.out.println("  - YouCan Store ID: " + savedStore.getId());
            System.out.println("  - YouCan Store Name: " + savedStore.getYoucanStoreName());
            System.out.println("  - Talabaty Store ID: " + savedStore.getStore().getId());
            System.out.println("  - Account ID: " + savedStore.getAccount().getId());
            System.out.println("  - Active: " + savedStore.isActive());
            System.out.println("========================================");
            
            return savedStore;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to save YouCan store to database!");
            System.err.println("Exception: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save YouCan store: " + e.getMessage(), e);
        }
    }

    /**
     * Exchange authorization code for access token
     */
    private Map<String, Object> exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                System.err.println("Token exchange failed. Status: " + response.getStatusCode());
                throw new RuntimeException("Failed to exchange authorization code for token. Status: " + response.getStatusCode());
            }

            System.out.println("Token exchange successful");
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Exception during token exchange: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to exchange authorization code for token: " + e.getMessage(), e);
        }
    }

    /**
     * Get store information from YouCan API
     */
    private Map<String, Object> getStoreInfo(String accessToken) {
        System.out.println("Calling YouCan API: https://api.youcan.shop/stores/me");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.youcan.shop/stores/me",
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            System.out.println("API Response Status: " + response.getStatusCode());
            
            if (response.getStatusCode() != HttpStatus.OK) {
                System.err.println("API returned non-OK status: " + response.getStatusCode());
                throw new RuntimeException("Failed to get store information from YouCan. Status: " + response.getStatusCode());
            }
            
            if (response.getBody() == null) {
                System.err.println("API returned null body");
                throw new RuntimeException("Failed to get store information from YouCan: null response body");
            }

            System.out.println("API Response Body: " + response.getBody());
            return response.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("HTTP Client Error: " + e.getStatusCode());
            System.err.println("Response Body: " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to get store information from YouCan: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Exception calling YouCan API: " + e.getClass().getName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to get store information from YouCan: " + e.getMessage(), e);
        }
    }

    /**
     * Refresh access token using refresh token
     */
    public YouCanStore refreshToken(YouCanStore youCanStore) {
        if (youCanStore.getRefreshToken() == null) {
            throw new RuntimeException("No refresh token available");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", youCanStore.getRefreshToken());
        body.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
        
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Failed to refresh access token");
        }

        Map<String, Object> tokenResponse = response.getBody();
        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        Integer expiresIn = (Integer) tokenResponse.get("expires_in");

        youCanStore.setAccessToken(accessToken);
        if (refreshToken != null) {
            youCanStore.setRefreshToken(refreshToken);
        }
        if (expiresIn != null) {
            youCanStore.setTokenExpiresAt(OffsetDateTime.now().plusSeconds(expiresIn));
        }

        return youCanStoreRepository.save(youCanStore);
    }

    /**
     * Get valid access token, refreshing if necessary
     */
    public String getValidAccessToken(YouCanStore youCanStore) {
        if (youCanStore.isTokenExpired() && youCanStore.getRefreshToken() != null) {
            refreshToken(youCanStore);
        }
        return youCanStore.getAccessToken();
    }
}


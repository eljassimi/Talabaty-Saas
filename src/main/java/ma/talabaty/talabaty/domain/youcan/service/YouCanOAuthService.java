package ma.talabaty.talabaty.domain.youcan.service;

import ma.talabaty.talabaty.domain.accounts.model.Account;
import ma.talabaty.talabaty.domain.accounts.repository.AccountRepository;
import ma.talabaty.talabaty.domain.stores.model.Store;
import ma.talabaty.talabaty.domain.stores.repository.StoreRepository;
import ma.talabaty.talabaty.domain.youcan.model.YouCanStore;
import ma.talabaty.talabaty.domain.youcan.repository.YouCanStoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class YouCanOAuthService {

    private static final Logger log = LoggerFactory.getLogger(YouCanOAuthService.class);

    private static final String STORES_ME_URL = "https://api.youcan.shop/stores/me";

    private final YouCanStoreRepository youCanStoreRepository;
    private final AccountRepository accountRepository;
    private final StoreRepository storeRepository;
    private final RestTemplate restTemplate;

    @Value("${youcan.oauth.client-id:2070}")
    private String clientId;

    @Value("${youcan.oauth.client-secret:Pt0aKugGGgp6BdBBKW1Y0i2rjRyZPTgU3vHeLAyX}")
    private String clientSecret;

    @Value("${youcan.oauth.redirect-uri:http://localhost:8080/api/youcan/oauth/callback}")
    private String redirectUri;

    @Value("${youcan.oauth.authorize-url:https://seller-area.youcan.shop/admin/oauth/authorize}")
    private String authorizeUrl;

    @Value("${youcan.oauth.token-url:https://api.youcan.shop/oauth/token}")
    private String tokenUrl;

    @Value("${youcan.oauth.scopes:*}")
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

    public String getAuthorizationUrl(UUID accountId, UUID storeId) {
        String state = accountId + ":" + storeId;

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(resolveAuthorizeUrl())
                .queryParam("client_id", resolveClientId())
                .queryParam("redirect_uri", resolveRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("state", state);

        if (scopes != null && !scopes.trim().isEmpty()) {
            for (String scope : scopes.trim().split("\\s+")) {
                if (!scope.isEmpty()) {
                    uriBuilder.queryParam("scope[]", scope);
                }
            }
        }

        return uriBuilder.build().toUriString();
    }

    public YouCanStore handleOAuthCallback(String code, String state) {
        String[] stateParts = state.split(":");
        if (stateParts.length != 2) {
            throw new IllegalArgumentException("Invalid state parameter");
        }

        UUID accountId = UUID.fromString(stateParts[0]);
        UUID storeId = UUID.fromString(stateParts[1]);

        Map<String, Object> tokenResponse = exchangeCodeForToken(code);
        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        Integer expiresIn = parseExpiresInSeconds(tokenResponse.get("expires_in"));

        if (accessToken == null) {
            log.error("YouCan token response missing access_token: keys={}", tokenResponse.keySet());
            throw new RuntimeException("Access token not found in token response");
        }

        String grantedScopes = scopes;
        Map<String, Object> storeInfo;
        try {
            storeInfo = getStoreInfo(accessToken);
        } catch (Exception e) {
            log.warn("Could not load YouCan store profile (stores/me); saving connection with placeholder id: {}", e.getMessage());
            storeInfo = new HashMap<>();
            storeInfo.put("id", "unknown_" + System.currentTimeMillis());
            storeInfo.put("domain", null);
            storeInfo.put("name", "YouCan Store");
        }

        String youcanStoreId = String.valueOf(storeInfo.get("id"));
        String youcanStoreDomain = storeInfo.get("domain") != null ? String.valueOf(storeInfo.get("domain")) : null;
        String youcanStoreName = storeInfo.get("name") != null ? String.valueOf(storeInfo.get("name")) : "YouCan Store";

        OffsetDateTime tokenExpiresAt = expiresIn != null ? OffsetDateTime.now().plusSeconds(expiresIn) : null;

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));

        YouCanStore youCanStore = youCanStoreRepository.findByStoreId(storeId).orElse(new YouCanStore());

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

        try {
            return youCanStoreRepository.save(youCanStore);
        } catch (Exception e) {
            log.error("Failed to persist YouCan store connection", e);
            throw new RuntimeException("Failed to save YouCan store: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", resolveClientId());
        body.add("client_secret", resolveClientSecret());
        body.add("redirect_uri", resolveRedirectUri());
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(resolveTokenUrl(), request, Map.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RuntimeException("Failed to exchange authorization code for token. Status: " + response.getStatusCode());
            }
            return response.getBody();
        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            log.warn("YouCan token exchange HTTP {}: {}", e.getStatusCode(), responseBody);
            throw new RuntimeException("YouCan token exchange failed: "
                    + (responseBody != null && !responseBody.isEmpty() ? responseBody : e.getMessage()), e);
        } catch (Exception e) {
            log.error("YouCan token exchange failed", e);
            throw new RuntimeException("Failed to exchange authorization code for token: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> getStoreInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    STORES_ME_URL,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RuntimeException("Failed to get store information from YouCan. Status: " + response.getStatusCode());
            }
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.warn("YouCan stores/me HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to get store information from YouCan: " + e.getMessage(), e);
        }
    }

    public YouCanStore refreshToken(YouCanStore youCanStore) {
        if (youCanStore.getRefreshToken() == null) {
            throw new RuntimeException("No refresh token available");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", resolveClientId());
        body.add("client_secret", resolveClientSecret());
        body.add("refresh_token", youCanStore.getRefreshToken());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(resolveTokenUrl(), request, Map.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Failed to refresh access token");
        }

        Map<String, Object> tokenResponse = response.getBody();
        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        Integer expiresIn = parseExpiresInSeconds(tokenResponse.get("expires_in"));

        youCanStore.setAccessToken(accessToken);
        if (refreshToken != null) {
            youCanStore.setRefreshToken(refreshToken);
        }
        if (expiresIn != null) {
            youCanStore.setTokenExpiresAt(OffsetDateTime.now().plusSeconds(expiresIn));
        }

        return youCanStoreRepository.save(youCanStore);
    }

    public String getValidAccessToken(YouCanStore youCanStore) {
        if (youCanStore.isTokenExpired() && youCanStore.getRefreshToken() != null) {
            refreshToken(youCanStore);
        }
        return youCanStore.getAccessToken();
    }

    private String resolveClientId() {
        return (clientId != null && !clientId.isBlank()) ? clientId.trim() : "2070";
    }

    private String resolveClientSecret() {
        return (clientSecret != null && !clientSecret.isBlank()) ? clientSecret.trim()
                : "Pt0aKugGGgp6BdBBKW1Y0i2rjRyZPTgU3vHeLAyX";
    }

    private String resolveRedirectUri() {
        String r = (redirectUri != null && !redirectUri.isBlank()) ? redirectUri.trim().replaceAll("/$", "")
                : "http://localhost:8080/api/youcan/oauth/callback";
        return r;
    }

    private String resolveAuthorizeUrl() {
        return (authorizeUrl != null && !authorizeUrl.isBlank()) ? authorizeUrl.trim()
                : "https://seller-area.youcan.shop/admin/oauth/authorize";
    }

    private String resolveTokenUrl() {
        return (tokenUrl != null && !tokenUrl.isBlank()) ? tokenUrl.trim() : "https://api.youcan.shop/oauth/token";
    }

    private static Integer parseExpiresInSeconds(Object expiresInObj) {
        if (expiresInObj == null) {
            return null;
        }
        if (expiresInObj instanceof Number n) {
            long v = n.longValue();
            if (v > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            if (v < 0) {
                return 0;
            }
            return (int) v;
        }
        return null;
    }
}

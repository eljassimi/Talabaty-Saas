package ma.talabaty.talabaty.domain.youcan.service;

import ma.talabaty.talabaty.domain.youcan.model.YouCanStore;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class YouCanApiService {

    private final RestTemplate restTemplate;
    private final YouCanOAuthService youCanOAuthService;

    private static final String BASE_URL = "https://api.youcan.shop";

    public YouCanApiService(RestTemplate restTemplate, YouCanOAuthService youCanOAuthService) {
        this.restTemplate = restTemplate;
        this.youCanOAuthService = youCanOAuthService;
    }

    /**
     * Get a single order by ID
     */
    public Map<String, Object> getOrder(YouCanStore youCanStore, String orderId) {
        String accessToken = youCanOAuthService.getValidAccessToken(youCanStore);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/orders/" + orderId,
                HttpMethod.GET,
                request,
                Map.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Failed to get order from YouCan: " + response.getStatusCode());
        }

        return response.getBody();
    }

    /**
     * List orders with pagination and filters
     */
    public Map<String, Object> listOrders(YouCanStore youCanStore, Map<String, String> filters) {
        String accessToken = youCanOAuthService.getValidAccessToken(youCanStore);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> request = new HttpEntity<>(headers);

        // Build query parameters
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(BASE_URL + "/orders");
        
        // Include subresources: customer, variants, payment, shipping for complete order data
        uriBuilder.queryParam("include", "customer,variants,payment,shipping");
        
        if (filters != null) {
            filters.forEach(uriBuilder::queryParam);
        }

        ResponseEntity<Map> response = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                request,
                Map.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Failed to list orders from YouCan: " + response.getStatusCode());
        }

        return response.getBody();
    }

    /**
     * Update order status
     */
    public Map<String, Object> updateOrderStatus(YouCanStore youCanStore, String orderId, String status) {
        String accessToken = youCanOAuthService.getValidAccessToken(youCanStore);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/orders/" + orderId + "/status/" + status,
                HttpMethod.PUT,
                request,
                Map.class
        );

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to update order status in YouCan: " + response.getStatusCode());
        }

        return response.getBody() != null ? response.getBody() : new HashMap<>();
    }

    /**
     * Mark order as paid
     */
    public Map<String, Object> markOrderPaid(YouCanStore youCanStore, String orderId) {
        String accessToken = youCanOAuthService.getValidAccessToken(youCanStore);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/orders/" + orderId + "/pay",
                HttpMethod.PUT,
                request,
                Map.class
        );

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to mark order as paid in YouCan: " + response.getStatusCode());
        }

        return response.getBody() != null ? response.getBody() : new HashMap<>();
    }

    /**
     * Fulfill order
     */
    public Map<String, Object> fulfillOrder(YouCanStore youCanStore, String orderId) {
        String accessToken = youCanOAuthService.getValidAccessToken(youCanStore);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/orders/" + orderId + "/fulfill",
                HttpMethod.PUT,
                request,
                Map.class
        );

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to fulfill order in YouCan: " + response.getStatusCode());
        }

        return response.getBody() != null ? response.getBody() : new HashMap<>();
    }

    /**
     * Close order
     */
    public Map<String, Object> closeOrder(YouCanStore youCanStore, String orderId) {
        String accessToken = youCanOAuthService.getValidAccessToken(youCanStore);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/orders/" + orderId + "/close",
                HttpMethod.PUT,
                request,
                Map.class
        );

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to close order in YouCan: " + response.getStatusCode());
        }

        return response.getBody() != null ? response.getBody() : new HashMap<>();
    }

    /**
     * Get customer by ID
     */
    public Map<String, Object> getCustomer(YouCanStore youCanStore, String customerId) {
        String accessToken = youCanOAuthService.getValidAccessToken(youCanStore);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> request = new HttpEntity<>(headers);

        // Include address subresource to get customer addresses
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(BASE_URL + "/customers/" + customerId);
        uriBuilder.queryParam("include", "address");

        ResponseEntity<Map> response = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                request,
                Map.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Failed to get customer from YouCan: " + response.getStatusCode());
        }

        return response.getBody();
    }
}


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

    
    public Map<String, Object> getOrder(YouCanStore youCanStore, String orderId) {
        String accessToken = youCanOAuthService.getValidAccessToken(youCanStore);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> request = new HttpEntity<>(headers);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(BASE_URL + "/orders/" + orderId);
        uriBuilder.queryParam("include", "customer,variants,payment,shipping");

        ResponseEntity<Map> response = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                request,
                Map.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Failed to get order from YouCan: " + response.getStatusCode());
        }

        return response.getBody();
    }

    
    public Map<String, Object> listOrders(YouCanStore youCanStore, Map<String, String> filters) {
        String accessToken = youCanOAuthService.getValidAccessToken(youCanStore);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> request = new HttpEntity<>(headers);

        
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(BASE_URL + "/orders");
        
        
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

    public Map<String, Object> getCustomer(YouCanStore youCanStore, String customerId) {
        String accessToken = youCanOAuthService.getValidAccessToken(youCanStore);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> request = new HttpEntity<>(headers);

        
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


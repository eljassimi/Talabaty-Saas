package ma.talabaty.talabaty.domain.shipping.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;

@Service
public class OzonExpressService {

    
    private static final String BASE_URL = "https://api.ozonexpress.ma";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OzonExpressService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    
    public Map<String, Object> createParcel(String customerId, String apiKey, CreateParcelRequest request) {
        String url = BASE_URL + "/customers/" + customerId + "/" + apiKey + "/add-parcel";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        if (request.getTrackingNumber() != null) {
            body.add("tracking-number", request.getTrackingNumber());
        }
        body.add("parcel-receiver", request.getReceiver());
        body.add("parcel-phone", request.getPhone());
        body.add("parcel-city", request.getCityId());
        body.add("parcel-address", request.getAddress());
        if (request.getNote() != null) {
            body.add("parcel-note", request.getNote());
        }
        
        
        String priceStr;
        System.out.println("========================================");
        System.out.println("DEBUG OzonExpressService.createParcel:");
        System.out.println("  request.getPrice() = " + request.getPrice());
        System.out.println("  request.getPrice() != null? " + (request.getPrice() != null));
        System.out.println("  request.getPrice() == 0.0? " + (request.getPrice() != null && request.getPrice() == 0.0));
        System.out.println("  request.getPrice() > 0? " + (request.getPrice() != null && request.getPrice() > 0));
        
        if (request.getPrice() != null && request.getPrice() > 0) {
            
            
            long priceRounded = Math.round(request.getPrice());
            priceStr = String.valueOf(priceRounded);
            System.out.println("DEBUG OzonExpressService: Formatted price string (integer) = '" + priceStr + "'");
            System.out.println("DEBUG OzonExpressService: Original price: " + request.getPrice() + " -> Rounded to integer: " + priceRounded);
        } else if (request.getPrice() != null) {
            
            priceStr = "0";
            System.out.println("DEBUG OzonExpressService: Price is 0, using '0'");
        } else {
            priceStr = "0";
            System.out.println("DEBUG OzonExpressService: Price is null, using default '0'");
        }
        System.out.println("========================================");
        
        body.add("parcel-price", priceStr);
        System.out.println("DEBUG OzonExpressService: Sending parcel-price = '" + priceStr + "' to Ozon Express API");
        if (request.getNature() != null) {
            body.add("parcel-nature", request.getNature());
        }
        body.add("parcel-stock", String.valueOf(request.getStock()));
        if (request.getOpen() != null) {
            body.add("parcel-open", String.valueOf(request.getOpen()));
        }
        if (request.getFragile() != null) {
            body.add("parcel-fragile", String.valueOf(request.getFragile()));
        }
        if (request.getReplace() != null) {
            body.add("parcel-replace", String.valueOf(request.getReplace()));
        }
        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            body.add("products", request.getProducts());
        }
        
        
        System.out.println("DEBUG OzonExpressService: All parameters being sent to Ozon Express:");
        System.out.println("  parcel-receiver: " + body.getFirst("parcel-receiver"));
        System.out.println("  parcel-phone: " + body.getFirst("parcel-phone"));
        System.out.println("  parcel-city: " + body.getFirst("parcel-city"));
        System.out.println("  parcel-address: " + body.getFirst("parcel-address"));
        System.out.println("  parcel-price: " + body.getFirst("parcel-price"));
        System.out.println("  parcel-stock: " + body.getFirst("parcel-stock"));
        System.out.println("  parcel-open: " + body.getFirst("parcel-open"));
        System.out.println("  parcel-fragile: " + body.getFirst("parcel-fragile"));
        System.out.println("  parcel-replace: " + body.getFirst("parcel-replace"));
        if (body.getFirst("parcel-note") != null) {
            System.out.println("  parcel-note: " + body.getFirst("parcel-note"));
        }
        if (body.getFirst("parcel-nature") != null) {
            System.out.println("  parcel-nature: " + body.getFirst("parcel-nature"));
        }
        System.out.println("DEBUG OzonExpressService: URL = " + url);
        
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            String responseBody = response.getBody();
            
            
            System.out.println("========================================");
            System.out.println("DEBUG OzonExpressService: Raw response from Ozon Express API:");
            System.out.println("  HTTP Status: " + response.getStatusCode());
            System.out.println("  Response Body: " + responseBody);
            System.out.println("========================================");
            
            
            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new RuntimeException("Ozon Express API returned an empty response. URL: " + url);
            }
            
            Map<String, Object> result;
            try {
                result = parseResponse(responseBody);
            } catch (RuntimeException parseException) {
                
                throw new RuntimeException("Ozon Express API error: Failed to parse response. Raw response: " + responseBody + ". URL: " + url, parseException);
            }
            
            
            if (result.containsKey("RESULT") && "ERROR".equalsIgnoreCase(String.valueOf(result.get("RESULT")))) {
                String errorMessage = result.containsKey("MESSAGE") ? String.valueOf(result.get("MESSAGE")) : "Unknown error from Ozon Express API";
                
                
                if (errorMessage.contains("City Not Found") || errorMessage.contains("City")) {
                    errorMessage += ". Please use GET /api/shipping/ozon-express/cities to get the list of available cities and their IDs.";
                }
                
                
                String fullResponse = responseBody != null ? responseBody : result.toString();
                throw new RuntimeException("Ozon Express API error: " + errorMessage + ". Full response: " + fullResponse + ". URL: " + url);
            }
            
            
            if (result.containsKey("ADD-PARCEL")) {
                Object addParcelValue = result.get("ADD-PARCEL");
                String addParcelStr = null;
                
                
                if (addParcelValue instanceof String) {
                    addParcelStr = (String) addParcelValue;
                } else if (addParcelValue instanceof Map) {
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> addParcelMap = (Map<String, Object>) addParcelValue;
                    
                    
                    if (addParcelMap.containsKey("RESULT") && "ERROR".equalsIgnoreCase(String.valueOf(addParcelMap.get("RESULT")))) {
                        String errorMessage = addParcelMap.containsKey("MESSAGE") ? String.valueOf(addParcelMap.get("MESSAGE")) : "Unknown error from Ozon Express API";
                        if (errorMessage.contains("City Not Found") || errorMessage.contains("City")) {
                            errorMessage += ". Please use GET /api/shipping/ozon-express/cities to get the list of available cities and their IDs.";
                        }
                        
                        String fullResponse = responseBody != null ? responseBody : result.toString();
                        throw new RuntimeException("Ozon Express API error: " + errorMessage + ". Full response: " + fullResponse + ". URL: " + url);
                    }
                    
                    
                    for (Map.Entry<String, Object> entry : addParcelMap.entrySet()) {
                        Object value = entry.getValue();
                        if (value instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> nestedMap = (Map<String, Object>) value;
                            if (nestedMap.containsKey("RESULT") && "ERROR".equalsIgnoreCase(String.valueOf(nestedMap.get("RESULT")))) {
                                String errorMessage = nestedMap.containsKey("MESSAGE") ? String.valueOf(nestedMap.get("MESSAGE")) : "Unknown error from Ozon Express API";
                                if (errorMessage.contains("City Not Found") || errorMessage.contains("City")) {
                                    errorMessage += ". Please use GET /api/shipping/ozon-express/cities to get the list of available cities and their IDs.";
                                }
                                String fullResponse = responseBody != null ? responseBody : result.toString();
                                throw new RuntimeException("Ozon Express API error: " + errorMessage + ". Full response: " + fullResponse + ". URL: " + url);
                            }
                        }
                    }
                    
                    
                }
                
                
                if (addParcelStr != null) {
                    
                    boolean hasError = addParcelStr.contains("\"RESULT\":\"ERROR\"") || 
                                      (addParcelStr.contains("RESULT") && addParcelStr.contains("ERROR")) ||
                                      addParcelStr.contains("\"RESULT\":\"ERROR\"");
                    
                    if (hasError) {
                        try {
                            JsonNode addParcelNode = objectMapper.readTree(addParcelStr);
                            if (addParcelNode.has("RESULT") && "ERROR".equalsIgnoreCase(addParcelNode.get("RESULT").asText())) {
                                String errorMessage = addParcelNode.has("MESSAGE") ? addParcelNode.get("MESSAGE").asText() : "Unknown error from Ozon Express API";
                                
                                
                                if (errorMessage.contains("City Not Found") || errorMessage.contains("City")) {
                                    errorMessage += ". Please use GET /api/shipping/ozon-express/cities to get the list of available cities and their IDs.";
                                }
                                
                                
                                String fullResponse = responseBody != null ? responseBody : result.toString();
                                throw new RuntimeException("Ozon Express API error: " + errorMessage + ". Full response: " + fullResponse + ". URL: " + url);
                            }
                        } catch (Exception parseException) {
                            
                            String errorMessage = extractErrorMessageFromString(addParcelStr);
                            
                            String fullResponse = responseBody != null ? responseBody : result.toString();
                            throw new RuntimeException("Ozon Express API error: " + errorMessage + ". Full response: " + fullResponse + ". URL: " + url);
                        }
                    }
                }
            }
            
            
            for (Map.Entry<String, Object> entry : result.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                
                if (key.toUpperCase().contains("ERROR") || key.toUpperCase().contains("FAIL")) {
                    String errorMessage = value != null ? value.toString() : "Unknown error from Ozon Express API";
                    
                    String fullResponse = responseBody != null ? responseBody : result.toString();
                    throw new RuntimeException("Ozon Express API error: " + errorMessage + ". Full response: " + fullResponse + ". URL: " + url);
                }
                
                
                if (value instanceof String) {
                    String valueStr = (String) value;
                    if (valueStr.contains("\"RESULT\":\"ERROR\"") || 
                        (valueStr.contains("RESULT") && valueStr.contains("ERROR")) ||
                        valueStr.contains("\"MESSAGE\"")) {
                        String extractedError = extractErrorMessageFromString(valueStr);
                        if (!extractedError.equals("Unknown error from Ozon Express API")) {
                            
                            String fullResponse = responseBody != null ? responseBody : result.toString();
                            throw new RuntimeException("Ozon Express API error: " + extractedError + ". Full response: " + fullResponse + ". URL: " + url);
                        }
                    }
                }
            }
            
            
            boolean hasSuccess = result.containsKey("TRACKING-NUMBER") || 
                                result.containsKey("SUCCESS") ||
                                (result.containsKey("ADD-PARCEL") && 
                                 String.valueOf(result.get("ADD-PARCEL")).contains("SUCCESS") &&
                                 !String.valueOf(result.get("ADD-PARCEL")).contains("ERROR"));
            
            
            if (!hasSuccess) {
                
                String fullResponse = responseBody != null ? responseBody : result.toString();
                
                
                String extractedError = null;
                
                
                if (result.containsKey("ADD-PARCEL")) {
                    Object addParcelValue = result.get("ADD-PARCEL");
                    if (addParcelValue instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> addParcelMap = (Map<String, Object>) addParcelValue;
                        if (addParcelMap.containsKey("RESULT") && "ERROR".equalsIgnoreCase(String.valueOf(addParcelMap.get("RESULT")))) {
                            extractedError = addParcelMap.containsKey("MESSAGE") ? String.valueOf(addParcelMap.get("MESSAGE")) : null;
                        }
                    } else if (addParcelValue instanceof String) {
                        
                        extractedError = extractErrorMessageFromString((String) addParcelValue);
                        if (extractedError.equals("Unknown error from Ozon Express API")) {
                            extractedError = null;
                        }
                    }
                }
                
                
                if (extractedError == null || extractedError.isEmpty()) {
                    extractedError = extractErrorMessageFromString(fullResponse);
                    if (extractedError.equals("Unknown error from Ozon Express API")) {
                        extractedError = null;
                    }
                }
                
                
                if (extractedError != null && !extractedError.isEmpty()) {
                    
                    if (extractedError.contains("City Not Found") || extractedError.contains("City")) {
                        extractedError += ". Please use GET /api/shipping/ozon-express/cities to get the list of available cities and their IDs.";
                    }
                    throw new RuntimeException("Ozon Express API error: " + extractedError + ". Full response: " + fullResponse + ". URL: " + url);
                } else {
                    
                    throw new RuntimeException("Ozon Express API error: No success indicator found in response. Full response: " + fullResponse + ". Parsed result: " + result.toString() + ". URL: " + url);
                }
            }
            
            return result;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            String errorMessage = extractErrorMessage(errorBody, url);
            throw new RuntimeException("Ozon Express API error: " + errorMessage, e);
        } catch (RestClientException e) {
            throw new RuntimeException("Error connecting to Ozon Express API: " + e.getMessage() + ". URL: " + url, e);
        } catch (RuntimeException e) {
            
            throw e;
        } catch (Exception e) {
            
            throw new RuntimeException("Error creating parcel in Ozon Express: " + e.getMessage() + ". URL: " + url, e);
        }
    }

    
    public Map<String, Object> getParcelInfo(String customerId, String apiKey, String trackingNumber) {
        String url = BASE_URL + "/customers/" + customerId + "/" + apiKey + "/parcel-info";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("tracking-number", trackingNumber);
        
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            Map<String, Object> result = parseResponse(response.getBody());
            
            
            if (result.containsKey("RESULT") && "ERROR".equalsIgnoreCase(String.valueOf(result.get("RESULT")))) {
                String errorMessage = result.containsKey("MESSAGE") ? String.valueOf(result.get("MESSAGE")) : "Unknown error from Ozon Express API";
                throw new RuntimeException("Ozon Express API error: " + errorMessage + ". URL: " + url);
            }
            
            return result;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            String errorMessage = extractErrorMessage(errorBody, url);
            throw new RuntimeException("Ozon Express API error: " + errorMessage, e);
        } catch (RestClientException e) {
            throw new RuntimeException("Error connecting to Ozon Express API: " + e.getMessage() + ". URL: " + url, e);
        } catch (Exception e) {
            throw new RuntimeException("Error getting parcel info from Ozon Express: " + e.getMessage() + ". URL: " + url, e);
        }
    }

    
    public Map<String, Object> trackParcel(String customerId, String apiKey, String trackingNumber) {
        String url = BASE_URL + "/customers/" + customerId + "/" + apiKey + "/tracking";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("tracking-number", trackingNumber);
        
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            Map<String, Object> result = parseResponse(response.getBody());
            
            
            if (result.containsKey("RESULT") && "ERROR".equalsIgnoreCase(String.valueOf(result.get("RESULT")))) {
                String errorMessage = result.containsKey("MESSAGE") ? String.valueOf(result.get("MESSAGE")) : "Unknown error from Ozon Express API";
                throw new RuntimeException("Ozon Express API error: " + errorMessage + ". URL: " + url);
            }
            
            return result;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            String errorMessage = extractErrorMessage(errorBody, url);
            throw new RuntimeException("Ozon Express API error: " + errorMessage, e);
        } catch (RestClientException e) {
            throw new RuntimeException("Error connecting to Ozon Express API: " + e.getMessage() + ". URL: " + url, e);
        } catch (Exception e) {
            throw new RuntimeException("Error tracking parcel in Ozon Express: " + e.getMessage() + ". URL: " + url, e);
        }
    }

    
    public Map<String, Object> trackMultipleParcels(String customerId, String apiKey, List<String> trackingNumbers) {
        String url = BASE_URL + "/customers/" + customerId + "/" + apiKey + "/tracking";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> body = new HashMap<>();
        body.put("tracking-number", trackingNumbers);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return parseResponse(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Error tracking multiple parcels in Ozon Express: " + e.getMessage(), e);
        }
    }

    
    public Map<String, Object> createDeliveryNote(String customerId, String apiKey) {
        String url = BASE_URL + "/customers/" + customerId + "/" + apiKey + "/add-delivery-note";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return parseResponse(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Error creating delivery note in Ozon Express: " + e.getMessage(), e);
        }
    }

    
    public Map<String, Object> addParcelsToDeliveryNote(String customerId, String apiKey, String deliveryNoteRef, List<String> trackingNumbers) {
        String url = BASE_URL + "/customers/" + customerId + "/" + apiKey + "/add-parcel-to-delivery-note";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("Ref", deliveryNoteRef);
        
        int idx = 0;
        for (String code : trackingNumbers) {
            if (code != null && !code.trim().isEmpty()) {
                body.add("Codes[" + idx + "]", code.trim());
                idx++;
            }
        }
        if (idx == 0) {
            throw new RuntimeException("No valid tracking numbers to add to delivery note");
        }
        
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return parseResponse(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Error adding parcels to delivery note in Ozon Express: " + e.getMessage(), e);
        }
    }

    
    public Map<String, Object> saveDeliveryNote(String customerId, String apiKey, String deliveryNoteRef) {
        String url = BASE_URL + "/customers/" + customerId + "/" + apiKey + "/save-delivery-note";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("Ref", deliveryNoteRef);
        
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return parseResponse(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Error saving delivery note in Ozon Express: " + e.getMessage(), e);
        }
    }

    private static final String CLIENT_BASE = "https://client.ozoneexpress.ma";

    
    public byte[] fetchDeliveryNotePdf(String ref, String type, String customerId, String apiKey) {
        String path;
        switch (type != null ? type : "standard") {
            case "tickets":
                path = "/pdf-delivery-note-tickets";
                break;
            case "tickets-4-4":
                path = "/pdf-delivery-note-tickets-4-4";
                break;
            default:
                path = "/pdf-delivery-note";
        }
        String urlWithRef = CLIENT_BASE + path + "?dn-ref=" + ref;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Talabaty/1.0");
            headers.set("Accept", "application/pdf,text/html,*/*");
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    urlWithRef,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                byte[] body = response.getBody();
                if (body.length > 0 && !isHtml(body)) {
                    return body;
                }
            }
        } catch (Exception ignored) {
        }
        String withCreds = urlWithRef + "&customer_id=" + customerId + "&api_key=" + apiKey;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Talabaty/1.0");
            headers.set("Accept", "application/pdf");
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    withCreds,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    byte[].class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                byte[] body = response.getBody();
                if (body.length > 0 && !isHtml(body)) {
                    return body;
                }
            }
        } catch (Exception ignored) {
        }
        throw new RuntimeException("PDF non disponible via l’API. Téléchargez-le depuis client.ozoneexpress.ma (connexion requise).");
    }

    private boolean isHtml(byte[] body) {
        if (body.length < 100) return false;
        String start = new String(body, 0, Math.min(200, body.length), StandardCharsets.UTF_8).toLowerCase();
        return start.contains("<!DOCTYPE") || start.contains("<html");
    }

    
    public List<Map<String, Object>> getCities() {
        String url = BASE_URL + "/cities";
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            List<Map<String, Object>> cities = new ArrayList<>();
            if (jsonNode.isArray()) {
                for (JsonNode city : jsonNode) {
                    Map<String, Object> cityMap = new HashMap<>();
                    Collection<String> fieldNames = city.propertyNames();
                    for (String fieldName : fieldNames) {
                        JsonNode fieldValue = city.get(fieldName);
                        if (fieldValue != null) {
                            if (fieldValue.isTextual()) {
                                cityMap.put(fieldName, fieldValue.asText());
                            } else if (fieldValue.isNumber()) {
                                cityMap.put(fieldName, fieldValue.asDouble());
                            } else if (fieldValue.isBoolean()) {
                                cityMap.put(fieldName, fieldValue.asBoolean());
                            } else {
                                cityMap.put(fieldName, fieldValue.toString());
                            }
                        }
                    }
                    cities.add(cityMap);
                }
            }
            return cities;
        } catch (Exception e) {
            throw new RuntimeException("Error getting cities from Ozon Express: " + e.getMessage(), e);
        }
    }

    private String extractErrorMessageFromString(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return "Unknown error from Ozon Express API";
        }
        
        
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            if (jsonNode.isObject() && jsonNode.has("MESSAGE")) {
                String message = jsonNode.get("MESSAGE").asText();
                if (message != null && !message.trim().isEmpty()) {
                    return message;
                }
            }
            
            if (jsonNode.isObject()) {
                Collection<String> fieldNames = jsonNode.propertyNames();
                for (String fieldName : fieldNames) {
                    JsonNode fieldValue = jsonNode.get(fieldName);
                    if (fieldValue != null && fieldValue.isObject()) {
                        
                        if (fieldValue.has("MESSAGE")) {
                            String message = fieldValue.get("MESSAGE").asText();
                            if (message != null && !message.trim().isEmpty()) {
                                return message;
                            }
                        }
                        
                        if (fieldValue.has("RESULT") && "ERROR".equalsIgnoreCase(fieldValue.get("RESULT").asText())) {
                            if (fieldValue.has("MESSAGE")) {
                                String message = fieldValue.get("MESSAGE").asText();
                                if (message != null && !message.trim().isEmpty()) {
                                    return message;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            
        }
        
        
        String errorMessage = "Unknown error from Ozon Express API";
        
        
        int messageIndex = jsonString.indexOf("\"MESSAGE\":\"");
        if (messageIndex >= 0) {
            int start = messageIndex + 10; 
            int end = jsonString.indexOf("\"", start);
            if (end > start) {
                errorMessage = jsonString.substring(start, end);
            }
        } else {
            
            messageIndex = jsonString.indexOf("\"MESSAGE\"");
            if (messageIndex >= 0) {
                int colonIndex = jsonString.indexOf(":", messageIndex);
                if (colonIndex >= 0) {
                    
                    int quoteStart = jsonString.indexOf("\"", colonIndex);
                    if (quoteStart >= 0) {
                        int quoteEnd = jsonString.indexOf("\"", quoteStart + 1);
                        if (quoteEnd > quoteStart) {
                            errorMessage = jsonString.substring(quoteStart + 1, quoteEnd);
                        }
                    }
                }
            }
        }
        
        
        if (errorMessage.contains("City Not Found") || errorMessage.contains("City")) {
            errorMessage += ". Please use GET /api/shipping/ozon-express/cities to get the list of available cities and their IDs.";
        }
        
        return errorMessage;
    }

    private String extractErrorMessage(String errorBody, String url) {
        if (errorBody == null || errorBody.trim().isEmpty()) {
            return "Unknown error from Ozon Express API. Please verify your Customer ID and API Key are correct. URL: " + url;
        }
        
        try {
            Map<String, Object> errorResponse = parseResponse(errorBody);
            if (errorResponse.containsKey("MESSAGE")) {
                return String.valueOf(errorResponse.get("MESSAGE")) + ". URL: " + url;
            } else if (errorResponse.containsKey("RESULT")) {
                return "Ozon Express API error: " + errorResponse.get("RESULT") + ". URL: " + url;
            }
        } catch (Exception e) {
            
            String extracted = extractErrorMessageFromString(errorBody);
            if (!extracted.equals("Unknown error from Ozon Express API")) {
                return extracted + ". URL: " + url;
            }
        }
        
        return "Ozon Express API error: " + errorBody + ". URL: " + url;
    }

    private Map<String, Object> parseResponse(String responseBody) {
        try {
            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new RuntimeException("Empty response from Ozon Express API");
            }
            
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return parseJsonNode(jsonNode);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing Ozon Express response: " + e.getMessage() + ". Response body: " + responseBody, e);
        }
    }
    
    private Map<String, Object> parseJsonNode(JsonNode jsonNode) {
        Map<String, Object> result = new HashMap<>();
        if (jsonNode.isObject()) {
            Collection<String> fieldNames = jsonNode.propertyNames();
            for (String fieldName : fieldNames) {
                JsonNode fieldValue = jsonNode.get(fieldName);
                if (fieldValue != null) {
                    if (fieldValue.isTextual()) {
                        result.put(fieldName, fieldValue.asText());
                    } else if (fieldValue.isNumber()) {
                        result.put(fieldName, fieldValue.asDouble());
                    } else if (fieldValue.isBoolean()) {
                        result.put(fieldName, fieldValue.asBoolean());
                    } else if (fieldValue.isObject()) {
                        
                        result.put(fieldName, parseJsonNode(fieldValue));
                    } else if (fieldValue.isArray()) {
                        
                        List<Object> arrayList = new ArrayList<>();
                        for (JsonNode arrayItem : fieldValue) {
                            if (arrayItem.isObject()) {
                                arrayList.add(parseJsonNode(arrayItem));
                            } else if (arrayItem.isTextual()) {
                                arrayList.add(arrayItem.asText());
                            } else if (arrayItem.isNumber()) {
                                arrayList.add(arrayItem.asDouble());
                            } else if (arrayItem.isBoolean()) {
                                arrayList.add(arrayItem.asBoolean());
                            } else {
                                arrayList.add(arrayItem.toString());
                            }
                        }
                        result.put(fieldName, arrayList);
                    } else {
                        result.put(fieldName, fieldValue.toString());
                    }
                }
            }
        }
        return result;
    }

    
    public static class CreateParcelRequest {
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
}


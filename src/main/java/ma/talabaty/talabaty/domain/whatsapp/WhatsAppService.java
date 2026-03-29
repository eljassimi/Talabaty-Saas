package ma.talabaty.talabaty.domain.whatsapp;

import ma.talabaty.talabaty.domain.orders.model.Order;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Service
public class WhatsAppService {

    private static final String TWILIO_MESSAGES_URL = "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json";

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.whatsapp.from:}")
    private String whatsappFrom;

    @Value("${whatsapp.local.url:}")
    private String localBridgeUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile Map<String, Object> lastBridgeStatus = null;
    private volatile long lastBridgeStatusAtMs = 0L;

    
    public boolean isConfigured() {
        return isTwilioConfigured() || isLocalBridgeConfigured();
    }

    public boolean isTwilioConfigured() {
        return accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()
                && whatsappFrom != null && !whatsappFrom.isBlank();
    }

    public boolean isLocalBridgeConfigured() {
        return localBridgeUrl != null && !localBridgeUrl.trim().isBlank();
    }

    
    public String normalizePhone(String toPhone) {
        if (toPhone == null || toPhone.isBlank()) return "";
        String digits = toPhone.trim().replaceAll("\\D", "");
        if (digits.isEmpty()) return "";
        while (digits.startsWith("0")) digits = digits.substring(1);
        if (digits.length() == 9 && (digits.startsWith("6") || digits.startsWith("7"))) {
            digits = "212" + digits; 
        }
        return "+" + digits;
    }

    
    public boolean send(UUID storeId, String toPhone, String message) {
        return sendWithReason(storeId, toPhone, message) == null;
    }

    
    public boolean send(String toPhone, String message) {
        return send(null, toPhone, message);
    }

    
    public String sendWithReason(UUID storeId, String toPhone, String message) {
        if (toPhone == null || toPhone.isBlank() || message == null || message.isBlank()) {
            return "Missing phone or message";
        }
        String to = normalizePhone(toPhone);
        if (to.length() < 10) return "Invalid phone number";

        if (isLocalBridgeConfigured()) {
            return sendViaLocalBridgeWithReason(storeId, to, message);
        }
        if (isTwilioConfigured()) {
            return sendViaTwilio(to, message) ? null : "Twilio send failed";
        }
        return "WhatsApp not configured";
    }

    private boolean sendViaLocalBridge(UUID storeId, String to, String message) {
        return sendViaLocalBridgeWithReason(storeId, to, message) == null;
    }

    private String sendViaLocalBridgeWithReason(UUID storeId, String to, String message) {
        String base = localBridgeUrl.trim().replaceAll("/$", "");
        String url = base + "/send";
        if (storeId != null) {
            url = url + "?sessionId=" + storeId;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            String json = objectMapper.writeValueAsString(Map.of("to", to, "message", message));
            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(json, headers), String.class);
            if (response.getStatusCode().is2xxSuccessful()) return null;
            return "Bridge returned " + response.getStatusCode();
        } catch (HttpServerErrorException e) {
            String body = e.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = objectMapper.readValue(body, Map.class);
                    if (map.containsKey("error")) return String.valueOf(map.get("error"));
                } catch (Exception ignored) { }
            }
            return e.getStatusCode() + ": " + (e.getMessage() != null ? e.getMessage() : "Send failed");
        } catch (Exception e) {
            System.err.println("[WhatsApp local] Send failed: " + e.getMessage());
            return e.getMessage() != null ? e.getMessage() : "Send failed";
        }
    }

    private boolean sendViaTwilio(String to, String message) {
        String toWhatsApp = "whatsapp:" + to;
        String fromWhatsApp = whatsappFrom.trim().startsWith("whatsapp:") ? whatsappFrom.trim() : "whatsapp:" + whatsappFrom.trim();
        String url = String.format(TWILIO_MESSAGES_URL, accountSid);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String auth = accountSid + ":" + authToken;
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8)));
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", toWhatsApp);
        body.add("From", fromWhatsApp);
        body.add("Body", message);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            return response.getStatusCode() == HttpStatus.CREATED;
        } catch (Exception e) {
            System.err.println("[WhatsApp Twilio] Send failed: " + e.getMessage());
            return false;
        }
    }

    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getBridgeLinkStatus(UUID storeId) {
        Map<String, Object> result = new HashMap<>();
        if (!isLocalBridgeConfigured()) {
            return result;
        }
        String base = localBridgeUrl.trim().replaceAll("/$", "");
        String url = base + "/qr";
        if (storeId != null) {
            url = url + "?sessionId=" + storeId;
        }
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
                result.put("ready", Boolean.TRUE.equals(body.get("ready")));
                if (body.containsKey("qr") && body.get("qr") != null) {
                    result.put("qr", body.get("qr").toString());
                }
                result.put("initializing", Boolean.TRUE.equals(body.get("initializing")));
                if (body.containsKey("error") && body.get("error") != null) {
                    result.put("bridgeError", body.get("error").toString());
                }
                if (body.containsKey("reason") && body.get("reason") != null) {
                    result.put("reason", body.get("reason").toString());
                }
                
                lastBridgeStatus = new HashMap<>(result);
                lastBridgeStatusAtMs = System.currentTimeMillis();
            }
        } catch (Exception e) {
            System.err.println("[WhatsApp bridge] Get QR failed: " + e.getMessage());
            long now = System.currentTimeMillis();
            boolean recent = lastBridgeStatus != null && (now - lastBridgeStatusAtMs) < 120_000; 
            if (recent) {
                result.putAll(lastBridgeStatus);
                result.put("ready", false);
                result.put("initializing", true);
                result.putIfAbsent("reason", "logged_out");
            } else {
                result.put("error", "Cannot reach WhatsApp service. Please try again or contact your administrator.");
            }
        }
        return result;
    }

    
    public Map<String, Object> getBridgeLinkStatus() {
        return getBridgeLinkStatus(null);
    }

    
    public String fillTemplate(String template, Order order) {
        if (template == null || template.isBlank()) return "";
        String s = template;
        if (order.getCustomerName() != null) {
            s = s.replace("{{customerName}}", order.getCustomerName());
        }
        if (order.getId() != null) {
            s = s.replace("{{orderId}}", order.getId().toString());
        }
        if (order.getOzonTrackingNumber() != null) {
            s = s.replace("{{trackingNumber}}", order.getOzonTrackingNumber());
        } else {
            s = s.replace("{{trackingNumber}}", "");
        }
        if (order.getTotalAmount() != null) {
            s = s.replace("{{totalAmount}}", order.getTotalAmount().toPlainString());
        } else {
            s = s.replace("{{totalAmount}}", "0");
        }
        if (order.getCurrency() != null) {
            s = s.replace("{{currency}}", order.getCurrency());
        } else {
            s = s.replace("{{currency}}", "MAD");
        }
        if (order.getCity() != null) {
            s = s.replace("{{city}}", order.getCity());
        } else {
            s = s.replace("{{city}}", "");
        }
        return s;
    }
}

package ma.talabaty.talabaty.api.controllers;

import ma.talabaty.talabaty.domain.webhooks.model.WebhookEventType;
import ma.talabaty.talabaty.domain.webhooks.model.WebhookSubscription;
import ma.talabaty.talabaty.domain.webhooks.service.WebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<WebhookSubscription> createSubscription(@RequestBody CreateSubscriptionRequest request) {
        WebhookSubscription subscription = webhookService.createSubscription(
                UUID.fromString(request.getStoreId()),
                request.getTargetUrl(),
                request.getEvents(),
                request.getSecretToken()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<WebhookSubscription>> getStoreSubscriptions(@PathVariable String storeId) {
        List<WebhookSubscription> subscriptions = webhookService.getStoreSubscriptions(UUID.fromString(storeId));
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebhookSubscription> getSubscription(@PathVariable String id) {
        WebhookSubscription subscription = webhookService.findById(UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Webhook subscription not found"));
        return ResponseEntity.ok(subscription);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebhookSubscription> updateSubscription(
            @PathVariable String id,
            @RequestBody UpdateSubscriptionRequest request) {
        WebhookSubscription subscription = webhookService.updateSubscription(
                UUID.fromString(id),
                request.getTargetUrl(),
                request.getEvents(),
                request.getActive()
        );
        return ResponseEntity.ok(subscription);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(@PathVariable String id) {
        webhookService.deleteSubscription(UUID.fromString(id));
        return ResponseEntity.noContent().build();
    }

    public static class CreateSubscriptionRequest {
        private String storeId;
        private String targetUrl;
        private Set<WebhookEventType> events;
        private String secretToken;

        public String getStoreId() { return storeId; }
        public void setStoreId(String storeId) { this.storeId = storeId; }
        public String getTargetUrl() { return targetUrl; }
        public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
        public Set<WebhookEventType> getEvents() { return events; }
        public void setEvents(Set<WebhookEventType> events) { this.events = events; }
        public String getSecretToken() { return secretToken; }
        public void setSecretToken(String secretToken) { this.secretToken = secretToken; }
    }

    public static class UpdateSubscriptionRequest {
        private String targetUrl;
        private Set<WebhookEventType> events;
        private Boolean active;

        public String getTargetUrl() { return targetUrl; }
        public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
        public Set<WebhookEventType> getEvents() { return events; }
        public void setEvents(Set<WebhookEventType> events) { this.events = events; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }
}


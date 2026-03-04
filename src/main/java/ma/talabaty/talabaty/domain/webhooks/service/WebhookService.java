package ma.talabaty.talabaty.domain.webhooks.service;

import ma.talabaty.talabaty.domain.stores.model.Store;
import ma.talabaty.talabaty.domain.stores.repository.StoreRepository;
import ma.talabaty.talabaty.domain.webhooks.model.WebhookEventType;
import ma.talabaty.talabaty.domain.webhooks.model.WebhookSubscription;
import ma.talabaty.talabaty.domain.webhooks.repository.WebhookSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class WebhookService {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final StoreRepository storeRepository;
    private static final SecureRandom random = new SecureRandom();

    public WebhookService(WebhookSubscriptionRepository subscriptionRepository, StoreRepository storeRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.storeRepository = storeRepository;
    }

    public WebhookSubscription createSubscription(UUID storeId, String targetUrl, Set<WebhookEventType> events, String secretToken) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        String token = secretToken != null ? secretToken : generateSecretToken();

        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setStore(store);
        subscription.setTargetUrl(targetUrl);
        subscription.setEvents(events);
        subscription.setSecretToken(token);
        subscription.setActive(true);

        return subscriptionRepository.save(subscription);
    }

    public List<WebhookSubscription> getStoreSubscriptions(UUID storeId) {
        return subscriptionRepository.findByStoreId(storeId);
    }

    public List<WebhookSubscription> getSubscriptionsForEvent(UUID storeId, WebhookEventType eventType) {
        return subscriptionRepository.findByStoreIdAndEventType(storeId, eventType);
    }

    public Optional<WebhookSubscription> findById(UUID id) {
        return subscriptionRepository.findById(id);
    }

    public WebhookSubscription updateSubscription(UUID id, String targetUrl, Set<WebhookEventType> events, Boolean active) {
        WebhookSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Webhook subscription not found"));

        if (targetUrl != null) {
            subscription.setTargetUrl(targetUrl);
        }
        if (events != null) {
            subscription.setEvents(events);
        }
        if (active != null) {
            subscription.setActive(active);
        }

        return subscriptionRepository.save(subscription);
    }

    public void deleteSubscription(UUID id) {
        WebhookSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Webhook subscription not found"));

        subscriptionRepository.delete(subscription);
    }

    private String generateSecretToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}


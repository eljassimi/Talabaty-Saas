package ma.talabaty.talabaty.domain.webhooks.repository;

import ma.talabaty.talabaty.domain.webhooks.model.WebhookEventType;
import ma.talabaty.talabaty.domain.webhooks.model.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, java.util.UUID> {
    @Query("SELECT ws FROM WebhookSubscription ws WHERE ws.store.id = :storeId")
    List<WebhookSubscription> findByStoreId(@Param("storeId") UUID storeId);
    
    @Query("SELECT ws FROM WebhookSubscription ws WHERE ws.store.id = :storeId AND :eventType MEMBER OF ws.events")
    List<WebhookSubscription> findByStoreIdAndEventType(@Param("storeId") UUID storeId, @Param("eventType") WebhookEventType eventType);
}


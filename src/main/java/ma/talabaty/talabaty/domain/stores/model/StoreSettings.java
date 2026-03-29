package ma.talabaty.talabaty.domain.stores.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "store_settings")
public class StoreSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(nullable = false, length = 8)
    private String currency = "USD";

    @Column(nullable = false, length = 10)
    private String locale = "en-US";

    @Column(name = "auto_assign_external_support", nullable = false)
    private boolean autoAssignExternalSupport;

    @Column(name = "webhook_endpoint", length = 255)
    private String webhookEndpoint;

    @Column(name = "notification_emails")
    private String notificationEmails;

    @Column(name = "whatsapp_automation_enabled", nullable = false)
    private boolean whatsappAutomationEnabled = false;

    @Column(name = "whatsapp_template_confirmed", columnDefinition = "text")
    private String whatsappTemplateConfirmed;

    @Column(name = "whatsapp_template_delivered", columnDefinition = "text")
    private String whatsappTemplateDelivered;

    @Column(name = "price_per_order_confirmed_mad", precision = 10, scale = 2)
    private BigDecimal pricePerOrderConfirmedMad = BigDecimal.ZERO;

    @Column(name = "price_per_order_delivered_mad", precision = 10, scale = 2)
    private BigDecimal pricePerOrderDeliveredMad = BigDecimal.ZERO;

    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String preferences;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isAutoAssignExternalSupport() {
        return autoAssignExternalSupport;
    }

    public void setAutoAssignExternalSupport(boolean autoAssignExternalSupport) {
        this.autoAssignExternalSupport = autoAssignExternalSupport;
    }

    public String getWebhookEndpoint() {
        return webhookEndpoint;
    }

    public void setWebhookEndpoint(String webhookEndpoint) {
        this.webhookEndpoint = webhookEndpoint;
    }

    public String getNotificationEmails() {
        return notificationEmails;
    }

    public void setNotificationEmails(String notificationEmails) {
        this.notificationEmails = notificationEmails;
    }

    public boolean isWhatsappAutomationEnabled() {
        return whatsappAutomationEnabled;
    }

    public void setWhatsappAutomationEnabled(boolean whatsappAutomationEnabled) {
        this.whatsappAutomationEnabled = whatsappAutomationEnabled;
    }

    public String getWhatsappTemplateConfirmed() {
        return whatsappTemplateConfirmed;
    }

    public void setWhatsappTemplateConfirmed(String whatsappTemplateConfirmed) {
        this.whatsappTemplateConfirmed = whatsappTemplateConfirmed;
    }

    public String getWhatsappTemplateDelivered() {
        return whatsappTemplateDelivered;
    }

    public void setWhatsappTemplateDelivered(String whatsappTemplateDelivered) {
        this.whatsappTemplateDelivered = whatsappTemplateDelivered;
    }

    public BigDecimal getPricePerOrderConfirmedMad() {
        return pricePerOrderConfirmedMad != null ? pricePerOrderConfirmedMad : BigDecimal.ZERO;
    }

    public void setPricePerOrderConfirmedMad(BigDecimal pricePerOrderConfirmedMad) {
        this.pricePerOrderConfirmedMad = pricePerOrderConfirmedMad;
    }

    public BigDecimal getPricePerOrderDeliveredMad() {
        return pricePerOrderDeliveredMad != null ? pricePerOrderDeliveredMad : BigDecimal.ZERO;
    }

    public void setPricePerOrderDeliveredMad(BigDecimal pricePerOrderDeliveredMad) {
        this.pricePerOrderDeliveredMad = pricePerOrderDeliveredMad;
    }

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}


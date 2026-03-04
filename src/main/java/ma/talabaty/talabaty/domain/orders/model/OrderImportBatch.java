package ma.talabaty.talabaty.domain.orders.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import ma.talabaty.talabaty.domain.stores.model.Store;
import ma.talabaty.talabaty.domain.users.model.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "order_import_batches")
public class OrderImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private User uploader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private StoredFile file;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ImportBatchStatus status = ImportBatchStatus.UPLOADED;

    @Column(name = "source_path", nullable = false, length = 255)
    private String sourcePath;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String summary;

    @Column(name = "last_processed_row", nullable = false)
    private Integer lastProcessedRow = 0;

    @Column(name = "auto_sync_enabled", nullable = false)
    private boolean autoSyncEnabled = true;

    @Column(name = "last_sync_at")
    private OffsetDateTime lastSyncAt;

    @OneToMany(mappedBy = "importBatch")
    private Set<Order> orders = new HashSet<>();

    @OneToMany(mappedBy = "batch")
    private Set<OrderImportRow> rows = new HashSet<>();

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

    public User getUploader() {
        return uploader;
    }

    public void setUploader(User uploader) {
        this.uploader = uploader;
    }

    public StoredFile getFile() {
        return file;
    }

    public void setFile(StoredFile file) {
        this.file = file;
    }

    public ImportBatchStatus getStatus() {
        return status;
    }

    public void setStatus(ImportBatchStatus status) {
        this.status = status;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Set<Order> getOrders() {
        return orders;
    }

    public Set<OrderImportRow> getRows() {
        return rows;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Integer getLastProcessedRow() {
        return lastProcessedRow;
    }

    public void setLastProcessedRow(Integer lastProcessedRow) {
        this.lastProcessedRow = lastProcessedRow;
    }

    public boolean isAutoSyncEnabled() {
        return autoSyncEnabled;
    }

    public void setAutoSyncEnabled(boolean autoSyncEnabled) {
        this.autoSyncEnabled = autoSyncEnabled;
    }

    public OffsetDateTime getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(OffsetDateTime lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }
}



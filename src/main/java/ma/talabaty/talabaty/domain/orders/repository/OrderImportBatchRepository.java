package ma.talabaty.talabaty.domain.orders.repository;

import ma.talabaty.talabaty.domain.orders.model.OrderImportBatch;
import ma.talabaty.talabaty.domain.orders.model.ImportBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderImportBatchRepository extends JpaRepository<OrderImportBatch, java.util.UUID> {
    @Query("SELECT b FROM OrderImportBatch b WHERE b.store.id = :storeId")
    List<OrderImportBatch> findByStoreId(@Param("storeId") UUID storeId);
    
    @Query("SELECT b FROM OrderImportBatch b WHERE b.store.id = :storeId AND b.status = :status")
    List<OrderImportBatch> findByStoreIdAndStatus(@Param("storeId") UUID storeId, @Param("status") ImportBatchStatus status);
}


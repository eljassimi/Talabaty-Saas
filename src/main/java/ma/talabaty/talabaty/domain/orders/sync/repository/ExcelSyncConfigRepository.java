package ma.talabaty.talabaty.domain.orders.sync.repository;

import ma.talabaty.talabaty.domain.orders.sync.model.ExcelSyncConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExcelSyncConfigRepository extends JpaRepository<ExcelSyncConfig, UUID> {
    
    List<ExcelSyncConfig> findByStoreId(UUID storeId);
    
    @Query("SELECT c FROM ExcelSyncConfig c WHERE c.syncEnabled = true")
    List<ExcelSyncConfig> findAllEnabled();
    
    @Query("SELECT c FROM ExcelSyncConfig c WHERE c.store.id = :storeId AND c.syncEnabled = true")
    List<ExcelSyncConfig> findByStoreIdAndSyncEnabled(@Param("storeId") UUID storeId);
}


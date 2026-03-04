package ma.talabaty.talabaty.domain.youcan.repository;

import ma.talabaty.talabaty.domain.youcan.model.YouCanStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface YouCanStoreRepository extends JpaRepository<YouCanStore, UUID> {
    
    @Query("SELECT ycs FROM YouCanStore ycs WHERE ycs.account.id = :accountId")
    List<YouCanStore> findByAccountId(@Param("accountId") UUID accountId);
    
    @Query("SELECT ycs FROM YouCanStore ycs WHERE ycs.store.id = :storeId")
    Optional<YouCanStore> findByStoreId(@Param("storeId") UUID storeId);
    
    @Query("SELECT ycs FROM YouCanStore ycs WHERE ycs.youcanStoreId = :youcanStoreId")
    Optional<YouCanStore> findByYoucanStoreId(@Param("youcanStoreId") String youcanStoreId);
    
    @Query("SELECT ycs FROM YouCanStore ycs WHERE ycs.account.id = :accountId AND ycs.active = true")
    List<YouCanStore> findActiveByAccountId(@Param("accountId") UUID accountId);
}


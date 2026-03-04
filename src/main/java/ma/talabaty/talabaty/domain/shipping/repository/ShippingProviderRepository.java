package ma.talabaty.talabaty.domain.shipping.repository;

import ma.talabaty.talabaty.domain.shipping.model.ProviderType;
import ma.talabaty.talabaty.domain.shipping.model.ShippingProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShippingProviderRepository extends JpaRepository<ShippingProvider, UUID> {
    @Query("SELECT sp FROM ShippingProvider sp WHERE sp.account.id = :accountId")
    List<ShippingProvider> findByAccountId(@Param("accountId") UUID accountId);
    
    @Query("SELECT sp FROM ShippingProvider sp WHERE sp.account.id = :accountId AND sp.providerType = :providerType AND sp.active = true ORDER BY sp.createdAt DESC")
    List<ShippingProvider> findByAccountIdAndProviderType(@Param("accountId") UUID accountId, @Param("providerType") ProviderType providerType);
    
    @Query("SELECT sp FROM ShippingProvider sp WHERE sp.store.id = :storeId")
    List<ShippingProvider> findByStoreId(@Param("storeId") UUID storeId);
    
    @Query("SELECT sp FROM ShippingProvider sp WHERE sp.store.id = :storeId AND sp.providerType = :providerType AND sp.active = true ORDER BY sp.createdAt DESC")
    List<ShippingProvider> findByStoreIdAndProviderType(@Param("storeId") UUID storeId, @Param("providerType") ProviderType providerType);
}


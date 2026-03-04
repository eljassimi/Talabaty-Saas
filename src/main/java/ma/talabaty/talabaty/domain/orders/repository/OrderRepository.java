package ma.talabaty.talabaty.domain.orders.repository;

import ma.talabaty.talabaty.domain.orders.model.Order;
import ma.talabaty.talabaty.domain.orders.model.OrderSource;
import ma.talabaty.talabaty.domain.orders.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, java.util.UUID> {
    @Query("SELECT o FROM Order o WHERE o.store.id = :storeId ORDER BY o.createdAt DESC")
    List<Order> findByStoreId(@Param("storeId") UUID storeId);
    
    @Query("SELECT o FROM Order o WHERE o.store.id = :storeId AND o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findByStoreIdAndStatus(@Param("storeId") UUID storeId, @Param("status") OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.store.id = :storeId AND o.source = :source")
    List<Order> findByStoreIdAndSource(@Param("storeId") UUID storeId, @Param("source") OrderSource source);
    
    @Query("SELECT o FROM Order o WHERE o.store.id = :storeId AND o.externalOrderId = :externalOrderId")
    Optional<Order> findByStoreIdAndExternalOrderId(@Param("storeId") UUID storeId, @Param("externalOrderId") String externalOrderId);
    
    @Query("SELECT o FROM Order o WHERE o.store.account.id = :accountId")
    List<Order> findByAccountId(@Param("accountId") UUID accountId);
    
    // Query to force load totalAmount field explicitly
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithTotalAmount(@Param("id") UUID id);
}


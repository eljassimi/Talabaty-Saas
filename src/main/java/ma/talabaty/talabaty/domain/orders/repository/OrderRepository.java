package ma.talabaty.talabaty.domain.orders.repository;

import ma.talabaty.talabaty.domain.orders.model.Order;
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

    @Query("SELECT o FROM Order o WHERE o.store.id = :storeId AND o.externalOrderId = :externalOrderId")
    Optional<Order> findByStoreIdAndExternalOrderId(@Param("storeId") UUID storeId, @Param("externalOrderId") String externalOrderId);

    
    @Query("SELECT o FROM Order o JOIN FETCH o.store s JOIN FETCH s.account WHERE o.id IN :ids")
    List<Order> findAllByIdWithStoreAndAccount(@Param("ids") List<UUID> ids);

    
    @Query("SELECT DISTINCT o.customerPhone FROM Order o WHERE o.store.id = :storeId AND o.customerPhone IS NOT NULL AND LENGTH(TRIM(o.customerPhone)) > 0")
    List<String> findDistinctCustomerPhonesByStoreId(@Param("storeId") UUID storeId);

    
    @Query("SELECT o FROM Order o WHERE o.store.id = :storeId AND o.assignedTo IS NULL ORDER BY o.createdAt ASC")
    List<Order> findUnassignedByStoreIdOrderByCreatedAtAsc(@Param("storeId") UUID storeId);

    
    @Query("SELECT o FROM Order o WHERE o.store.id = :storeId AND o.assignedTo.id = :userId")
    List<Order> findByStoreIdAndAssignedToUserId(@Param("storeId") UUID storeId, @Param("userId") UUID userId);
}


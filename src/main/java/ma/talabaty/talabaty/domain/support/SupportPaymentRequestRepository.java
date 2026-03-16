package ma.talabaty.talabaty.domain.support;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface SupportPaymentRequestRepository extends JpaRepository<SupportPaymentRequest, UUID> {

    List<SupportPaymentRequest> findByUser_IdAndStore_IdOrderByRequestedAtDesc(UUID userId, UUID storeId);

    @Query("SELECT COALESCE(SUM(r.amountRequested), 0) FROM SupportPaymentRequest r WHERE r.user.id = :userId AND r.store.id = :storeId AND r.status = :status")
    BigDecimal sumAmountByUserAndStoreAndStatus(@Param("userId") UUID userId, @Param("storeId") UUID storeId, @Param("status") SupportPaymentRequest.PaymentRequestStatus status);

    @Query("SELECT r FROM SupportPaymentRequest r JOIN FETCH r.user u JOIN FETCH r.store s JOIN FETCH s.account WHERE s.account.id = :accountId ORDER BY r.requestedAt DESC")
    List<SupportPaymentRequest> findByAccountIdOrderByRequestedAtDesc(@Param("accountId") UUID accountId);
}

package ma.talabaty.talabaty.domain.support;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface SupportRevenueEntryRepository extends JpaRepository<SupportRevenueEntry, UUID> {

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM SupportRevenueEntry e WHERE e.user.id = :userId AND e.store.id = :storeId")
    BigDecimal sumAmountByUserAndStore(@Param("userId") UUID userId, @Param("storeId") UUID storeId);
}

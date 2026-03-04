package ma.talabaty.talabaty.domain.stores.repository;

import ma.talabaty.talabaty.domain.stores.model.Store;
import ma.talabaty.talabaty.domain.stores.model.StoreStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Store, java.util.UUID> {
    @Query("SELECT s FROM Store s WHERE s.account.id = :accountId")
    List<Store> findByAccountId(@Param("accountId") UUID accountId);
    
    @Query("SELECT s FROM Store s WHERE s.manager.id = :managerId")
    List<Store> findByManagerId(@Param("managerId") UUID managerId);
    
    List<Store> findByStatus(StoreStatus status);
    
    @Query("SELECT s FROM Store s WHERE s.account.id = :accountId AND s.id = :id")
    Optional<Store> findByAccountIdAndId(@Param("accountId") UUID accountId, @Param("id") UUID id);
    
    @Query("SELECT COUNT(s) > 0 FROM Store s WHERE s.account.id = :accountId AND s.name = :name")
    boolean existsByAccountIdAndName(@Param("accountId") UUID accountId, @Param("name") String name);
    
    @Query("SELECT COUNT(s) > 0 FROM Store s WHERE s.code = :code")
    boolean existsByCode(@Param("code") String code);
    
    @Query("SELECT DISTINCT s FROM Store s JOIN s.teamMembers tm WHERE tm.user.id = :userId")
    List<Store> findByTeamMemberUserId(@Param("userId") UUID userId);
}


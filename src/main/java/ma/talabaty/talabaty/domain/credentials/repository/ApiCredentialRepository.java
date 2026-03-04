package ma.talabaty.talabaty.domain.credentials.repository;

import ma.talabaty.talabaty.domain.credentials.model.ApiCredential;
import ma.talabaty.talabaty.domain.credentials.model.ApiCredentialStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiCredentialRepository extends JpaRepository<ApiCredential, java.util.UUID> {
    @Query("SELECT c FROM ApiCredential c WHERE c.account.id = :accountId")
    List<ApiCredential> findByAccountId(@Param("accountId") UUID accountId);
    
    Optional<ApiCredential> findByPublicKey(String publicKey);
    
    @Query("SELECT c FROM ApiCredential c WHERE c.account.id = :accountId AND c.status = :status")
    List<ApiCredential> findByAccountIdAndStatus(@Param("accountId") UUID accountId, @Param("status") ApiCredentialStatus status);
}


package ma.talabaty.talabaty.domain.users.repository;

import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.model.UserRole;
import ma.talabaty.talabaty.domain.users.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, java.util.UUID> {
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.account.id = :accountId")
    List<User> findByAccountId(@Param("accountId") UUID accountId);
    
    @Query("SELECT u FROM User u WHERE u.account.id = :accountId AND u.status = :status")
    List<User> findByAccountIdAndStatus(@Param("accountId") UUID accountId, @Param("status") UserStatus status);
    
    @Query("SELECT u FROM User u WHERE u.account.id = :accountId AND u.role != :role")
    List<User> findByAccountIdExcludingRole(@Param("accountId") UUID accountId, @Param("role") UserRole role);
    
    List<User> findByRole(UserRole role);
    List<User> findByStatus(UserStatus status);
    boolean existsByEmail(String email);
}


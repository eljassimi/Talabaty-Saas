package ma.talabaty.talabaty.domain.accounts.repository;

import ma.talabaty.talabaty.domain.accounts.model.Account;
import ma.talabaty.talabaty.domain.accounts.model.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, java.util.UUID> {
    List<Account> findByType(AccountType type);
    List<Account> findByStatus(AccountType.AccountStatus status);
    Optional<Account> findByName(String name);
}


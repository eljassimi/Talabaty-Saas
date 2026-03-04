package ma.talabaty.talabaty.domain.accounts.service;

import ma.talabaty.talabaty.domain.accounts.model.Account;
import ma.talabaty.talabaty.domain.accounts.model.AccountType;
import ma.talabaty.talabaty.domain.accounts.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account createAccount(String name, AccountType type) {
        Account account = new Account();
        account.setName(name);
        account.setType(type);
        account.setStatus(AccountType.AccountStatus.ACTIVE);
        return accountRepository.save(account);
    }

    public Optional<Account> findById(UUID id) {
        return accountRepository.findById(id);
    }

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public Account updateAccount(UUID id, String name, String timezone) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (name != null) {
            account.setName(name);
        }
        if (timezone != null) {
            account.setTimezone(timezone);
        }
        return accountRepository.save(account);
    }
}


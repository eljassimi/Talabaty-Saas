package ma.talabaty.talabaty.domain.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import ma.talabaty.talabaty.domain.accounts.model.Account;
import ma.talabaty.talabaty.domain.accounts.model.AccountType;
import ma.talabaty.talabaty.domain.accounts.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccount_savesWithNameTypeAndActiveStatus() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.createAccount("Acme", AccountType.COMPANY);

        assertThat(result.getName()).isEqualTo("Acme");
        assertThat(result.getType()).isEqualTo(AccountType.COMPANY);
        assertThat(result.getStatus()).isEqualTo(AccountType.AccountStatus.ACTIVE);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void updateAccount_throwsWhenAccountMissing() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.updateAccount(id, "new", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found");
    }
}

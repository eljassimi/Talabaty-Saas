package ma.talabaty.talabaty.domain.credentials.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import ma.talabaty.talabaty.domain.accounts.repository.AccountRepository;
import ma.talabaty.talabaty.domain.credentials.model.ApiCredential;
import ma.talabaty.talabaty.domain.credentials.model.ApiCredentialStatus;
import ma.talabaty.talabaty.domain.credentials.repository.ApiCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiCredentialServiceTest {

    @Mock
    private ApiCredentialRepository credentialRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private ApiCredentialService apiCredentialService;

    @Test
    void validateCredentials_returnsFalseWhenPublicKeyUnknown() {
        when(credentialRepository.findByPublicKey("unknown")).thenReturn(Optional.empty());

        assertThat(apiCredentialService.validateCredentials("unknown", "any")).isFalse();
    }

    @Test
    void validateCredentials_returnsTrueWhenActiveAndSecretMatches() {
        ApiCredential cred = new ApiCredential();
        cred.setPublicKey("pk");
        cred.setSecretKey("secret");
        cred.setStatus(ApiCredentialStatus.ACTIVE);
        when(credentialRepository.findByPublicKey("pk")).thenReturn(Optional.of(cred));

        assertThat(apiCredentialService.validateCredentials("pk", "secret")).isTrue();
    }

    @Test
    void validateCredentials_returnsFalseWhenSecretDoesNotMatch() {
        ApiCredential cred = new ApiCredential();
        cred.setPublicKey("pk");
        cred.setSecretKey("real-secret");
        cred.setStatus(ApiCredentialStatus.ACTIVE);
        when(credentialRepository.findByPublicKey("pk")).thenReturn(Optional.of(cred));

        assertThat(apiCredentialService.validateCredentials("pk", "wrong")).isFalse();
    }
}

package ma.talabaty.talabaty.domain.credentials.service;

import ma.talabaty.talabaty.domain.accounts.model.Account;
import ma.talabaty.talabaty.domain.accounts.repository.AccountRepository;
import ma.talabaty.talabaty.domain.credentials.model.ApiCredential;
import ma.talabaty.talabaty.domain.credentials.model.ApiCredentialStatus;
import ma.talabaty.talabaty.domain.credentials.repository.ApiCredentialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ApiCredentialService {

    private final ApiCredentialRepository credentialRepository;
    private final AccountRepository accountRepository;
    private static final SecureRandom random = new SecureRandom();

    public ApiCredentialService(ApiCredentialRepository credentialRepository, AccountRepository accountRepository) {
        this.credentialRepository = credentialRepository;
        this.accountRepository = accountRepository;
    }

    public ApiCredential createCredentials(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        String publicKey = generateKey(32);
        String secretKey = generateKey(64);

        ApiCredential credential = new ApiCredential();
        credential.setAccount(account);
        credential.setPublicKey(publicKey);
        credential.setSecretKey(secretKey);
        credential.setStatus(ApiCredentialStatus.ACTIVE);

        return credentialRepository.save(credential);
    }

    public List<ApiCredential> getAccountCredentials(UUID accountId) {
        return credentialRepository.findByAccountId(accountId);
    }

    public Optional<ApiCredential> findByPublicKey(String publicKey) {
        return credentialRepository.findByPublicKey(publicKey);
    }

    public void revokeCredential(UUID credentialId) {
        ApiCredential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new RuntimeException("Credential not found"));

        credential.setStatus(ApiCredentialStatus.REVOKED);
        credentialRepository.save(credential);
    }

    public void updateLastUsed(UUID credentialId) {
        ApiCredential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new RuntimeException("Credential not found"));

        credential.setLastUsedAt(OffsetDateTime.now());
        credentialRepository.save(credential);
    }

    public boolean validateCredentials(String publicKey, String secretKey) {
        Optional<ApiCredential> credential = credentialRepository.findByPublicKey(publicKey);
        if (credential.isEmpty()) {
            return false;
        }

        ApiCredential cred = credential.get();
        return cred.getStatus() == ApiCredentialStatus.ACTIVE 
                && cred.getSecretKey().equals(secretKey);
    }

    private String generateKey(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}


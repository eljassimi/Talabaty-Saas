package ma.talabaty.talabaty.api.controllers;

import ma.talabaty.talabaty.domain.credentials.model.ApiCredential;
import ma.talabaty.talabaty.domain.credentials.service.ApiCredentialService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/credentials")
public class ApiCredentialController {

    private final ApiCredentialService credentialService;

    public ApiCredentialController(ApiCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @PostMapping
    public ResponseEntity<ApiCredential> createCredentials(@RequestBody CreateCredentialRequest request) {
        ApiCredential credential = credentialService.createCredentials(UUID.fromString(request.getAccountId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(credential);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<ApiCredential>> getAccountCredentials(@PathVariable String accountId) {
        List<ApiCredential> credentials = credentialService.getAccountCredentials(UUID.fromString(accountId));
        return ResponseEntity.ok(credentials);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiCredential> getCredential(@PathVariable String id) {
        ApiCredential credential = credentialService.findByPublicKey(id)
                .orElseThrow(() -> new RuntimeException("Credential not found"));
        return ResponseEntity.ok(credential);
    }

    @PutMapping("/{id}/revoke")
    public ResponseEntity<Void> revokeCredential(@PathVariable String id) {
        credentialService.revokeCredential(UUID.fromString(id));
        return ResponseEntity.noContent().build();
    }

    public static class CreateCredentialRequest {
        private String accountId;

        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
    }
}


package ma.talabaty.talabaty.api.controllers;

import ma.talabaty.talabaty.api.dtos.ExcelSyncConfigDto;
import ma.talabaty.talabaty.core.security.AuthenticationHelper;
import ma.talabaty.talabaty.core.security.PermissionChecker;
import ma.talabaty.talabaty.domain.orders.sync.model.ExcelSyncConfig;
import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.repository.UserRepository;
import ma.talabaty.talabaty.domain.orders.sync.repository.ExcelSyncConfigRepository;
import ma.talabaty.talabaty.domain.orders.sync.service.GoogleSheetsSyncService;
import ma.talabaty.talabaty.domain.stores.repository.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/excel-sync")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:8080", "http://127.0.0.1:8080", "http://localhost:3001", "http://127.0.0.1:3001", "http://localhost:3002", "http://127.0.0.1:3002"}, maxAge = 3600)
public class ExcelSyncController {

    private final ExcelSyncConfigRepository syncConfigRepository;
    private final GoogleSheetsSyncService syncService;
    private final StoreRepository storeRepository;
    private final PermissionChecker permissionChecker;
    private final UserRepository userRepository;

    public ExcelSyncController(ExcelSyncConfigRepository syncConfigRepository,
                              GoogleSheetsSyncService syncService,
                              StoreRepository storeRepository,
                              PermissionChecker permissionChecker,
                              UserRepository userRepository) {
        this.syncConfigRepository = syncConfigRepository;
        this.syncService = syncService;
        this.storeRepository = storeRepository;
        this.permissionChecker = permissionChecker;
        this.userRepository = userRepository;
    }

    private void requireIntegrationsAccess(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return;
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && !permissionChecker.canAccessIntegrations(user.getRole())) {
            throw new org.springframework.security.access.AccessDeniedException("Support team cannot manage integrations.");
        }
    }

    @PostMapping
    public ResponseEntity<ExcelSyncConfigDto> createSyncConfig(
            @RequestBody CreateSyncConfigRequest request,
            Authentication authentication) {
        requireIntegrationsAccess(authentication);
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        
        // Verify store belongs to account
        var store = storeRepository.findByAccountIdAndId(accountId, UUID.fromString(request.getStoreId()))
                .orElseThrow(() -> new RuntimeException("Store not found or does not belong to your account"));

        ExcelSyncConfig config = new ExcelSyncConfig();
        config.setStore(store);
        config.setSpreadsheetId(request.getSpreadsheetId());
        config.setSheetName(request.getSheetName() != null ? request.getSheetName() : "Sheet1");
        config.setCredentialsJson(request.getCredentialsJson());
        config.setAccessToken(request.getAccessToken());
        config.setRefreshToken(request.getRefreshToken());
        config.setSyncEnabled(request.getSyncEnabled() != null ? request.getSyncEnabled() : true);
        config.setSyncIntervalSeconds(request.getSyncIntervalSeconds() != null ? request.getSyncIntervalSeconds() : 30);
        config.setColumnMapping(request.getColumnMapping());

        config = syncConfigRepository.save(config);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(config));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<ExcelSyncConfigDto>> getSyncConfigs(
            @PathVariable String storeId,
            Authentication authentication) {
        requireIntegrationsAccess(authentication);
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID storeUuid = UUID.fromString(storeId);
        
        // Verify store belongs to account
        storeRepository.findByAccountIdAndId(accountId, storeUuid)
                .orElseThrow(() -> new RuntimeException("Store not found or does not belong to your account"));

        List<ExcelSyncConfig> configs = syncConfigRepository.findByStoreId(storeUuid);
        List<ExcelSyncConfigDto> dtos = configs.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExcelSyncConfigDto> updateSyncConfig(
            @PathVariable String id,
            @RequestBody UpdateSyncConfigRequest request,
            Authentication authentication) {
        requireIntegrationsAccess(authentication);
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID configId = UUID.fromString(id);
        
        ExcelSyncConfig config = syncConfigRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("Sync config not found"));

        // Verify store belongs to account
        storeRepository.findByAccountIdAndId(accountId, config.getStore().getId())
                .orElseThrow(() -> new RuntimeException("Store not found or does not belong to your account"));

        if (request.getSpreadsheetId() != null) {
            config.setSpreadsheetId(request.getSpreadsheetId());
        }
        if (request.getSheetName() != null) {
            config.setSheetName(request.getSheetName());
        }
        if (request.getCredentialsJson() != null) {
            config.setCredentialsJson(request.getCredentialsJson());
        }
        if (request.getAccessToken() != null) {
            config.setAccessToken(request.getAccessToken());
        }
        if (request.getRefreshToken() != null) {
            config.setRefreshToken(request.getRefreshToken());
        }
        if (request.getSyncEnabled() != null) {
            config.setSyncEnabled(request.getSyncEnabled());
        }
        if (request.getSyncIntervalSeconds() != null) {
            config.setSyncIntervalSeconds(request.getSyncIntervalSeconds());
        }
        if (request.getColumnMapping() != null) {
            config.setColumnMapping(request.getColumnMapping());
        }

        config = syncConfigRepository.save(config);
        return ResponseEntity.ok(toDto(config));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSyncConfig(
            @PathVariable String id,
            Authentication authentication) {
        requireIntegrationsAccess(authentication);
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID configId = UUID.fromString(id);
        
        ExcelSyncConfig config = syncConfigRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("Sync config not found"));

        // Verify store belongs to account
        storeRepository.findByAccountIdAndId(accountId, config.getStore().getId())
                .orElseThrow(() -> new RuntimeException("Store not found or does not belong to your account"));

        syncConfigRepository.delete(config);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<SyncResultDto> triggerSync(
            @PathVariable String id,
            Authentication authentication) {
        requireIntegrationsAccess(authentication);
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID configId = UUID.fromString(id);
        
        ExcelSyncConfig config = syncConfigRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("Sync config not found"));

        // Verify store belongs to account
        storeRepository.findByAccountIdAndId(accountId, config.getStore().getId())
                .orElseThrow(() -> new RuntimeException("Store not found or does not belong to your account"));

        GoogleSheetsSyncService.SyncResult result = syncService.syncGoogleSheet(configId);
        return ResponseEntity.ok(new SyncResultDto(
                result.isSuccess(),
                result.getMessage(),
                result.getCreated(),
                result.getUpdated(),
                result.getErrors()
        ));
    }

    @PostMapping("/sync-all")
    public ResponseEntity<String> syncAll(Authentication authentication) {
        requireIntegrationsAccess(authentication);
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        // Only allow platform admins or account owners to sync all
        // For now, allow all authenticated users
        
        syncService.syncAllEnabled();
        return ResponseEntity.ok("Sync started for all enabled configurations");
    }

    private ExcelSyncConfigDto toDto(ExcelSyncConfig config) {
        ExcelSyncConfigDto dto = new ExcelSyncConfigDto();
        dto.setId(config.getId().toString());
        dto.setStoreId(config.getStore().getId().toString());
        dto.setSpreadsheetId(config.getSpreadsheetId());
        dto.setSheetName(config.getSheetName());
        dto.setSyncEnabled(config.getSyncEnabled());
        dto.setSyncIntervalSeconds(config.getSyncIntervalSeconds());
        dto.setColumnMapping(config.getColumnMapping());
        dto.setLastSyncAt(config.getLastSyncAt() != null ? config.getLastSyncAt().toString() : null);
        dto.setLastSyncStatus(config.getLastSyncStatus());
        dto.setLastSyncError(config.getLastSyncError());
        dto.setCreatedAt(config.getCreatedAt().toString());
        dto.setUpdatedAt(config.getUpdatedAt().toString());
        return dto;
    }

    // Request/Response DTOs
    public static class CreateSyncConfigRequest {
        private String storeId;
        private String spreadsheetId;
        private String sheetName;
        private String credentialsJson;
        private String accessToken;
        private String refreshToken;
        private Boolean syncEnabled;
        private Integer syncIntervalSeconds;
        private String columnMapping;

        public String getStoreId() { return storeId; }
        public void setStoreId(String storeId) { this.storeId = storeId; }
        public String getSpreadsheetId() { return spreadsheetId; }
        public void setSpreadsheetId(String spreadsheetId) { this.spreadsheetId = spreadsheetId; }
        public String getSheetName() { return sheetName; }
        public void setSheetName(String sheetName) { this.sheetName = sheetName; }
        public String getCredentialsJson() { return credentialsJson; }
        public void setCredentialsJson(String credentialsJson) { this.credentialsJson = credentialsJson; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public Boolean getSyncEnabled() { return syncEnabled; }
        public void setSyncEnabled(Boolean syncEnabled) { this.syncEnabled = syncEnabled; }
        public Integer getSyncIntervalSeconds() { return syncIntervalSeconds; }
        public void setSyncIntervalSeconds(Integer syncIntervalSeconds) { this.syncIntervalSeconds = syncIntervalSeconds; }
        public String getColumnMapping() { return columnMapping; }
        public void setColumnMapping(String columnMapping) { this.columnMapping = columnMapping; }
    }

    public static class UpdateSyncConfigRequest {
        private String spreadsheetId;
        private String sheetName;
        private String credentialsJson;
        private String accessToken;
        private String refreshToken;
        private Boolean syncEnabled;
        private Integer syncIntervalSeconds;
        private String columnMapping;

        public String getSpreadsheetId() { return spreadsheetId; }
        public void setSpreadsheetId(String spreadsheetId) { this.spreadsheetId = spreadsheetId; }
        public String getSheetName() { return sheetName; }
        public void setSheetName(String sheetName) { this.sheetName = sheetName; }
        public String getCredentialsJson() { return credentialsJson; }
        public void setCredentialsJson(String credentialsJson) { this.credentialsJson = credentialsJson; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public Boolean getSyncEnabled() { return syncEnabled; }
        public void setSyncEnabled(Boolean syncEnabled) { this.syncEnabled = syncEnabled; }
        public Integer getSyncIntervalSeconds() { return syncIntervalSeconds; }
        public void setSyncIntervalSeconds(Integer syncIntervalSeconds) { this.syncIntervalSeconds = syncIntervalSeconds; }
        public String getColumnMapping() { return columnMapping; }
        public void setColumnMapping(String columnMapping) { this.columnMapping = columnMapping; }
    }

    public static class SyncResultDto {
        private boolean success;
        private String message;
        private int created;
        private int updated;
        private int errors;

        public SyncResultDto(boolean success, String message, int created, int updated, int errors) {
            this.success = success;
            this.message = message;
            this.created = created;
            this.updated = updated;
            this.errors = errors;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getCreated() { return created; }
        public void setCreated(int created) { this.created = created; }
        public int getUpdated() { return updated; }
        public void setUpdated(int updated) { this.updated = updated; }
        public int getErrors() { return errors; }
        public void setErrors(int errors) { this.errors = errors; }
    }
}


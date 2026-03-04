package ma.talabaty.talabaty.api.controllers;

import ma.talabaty.talabaty.core.security.JwtUser;
import ma.talabaty.talabaty.core.storage.FileStorageService;
import ma.talabaty.talabaty.domain.orders.excel.ExcelImportService;
import ma.talabaty.talabaty.domain.orders.model.OrderImportBatch;
import ma.talabaty.talabaty.domain.orders.model.StoredFile;
import ma.talabaty.talabaty.domain.orders.repository.StoredFileRepository;
import ma.talabaty.talabaty.domain.stores.repository.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/upload")
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final ExcelImportService excelImportService;
    private final StoredFileRepository storedFileRepository;
    private final StoreRepository storeRepository;

    public FileUploadController(FileStorageService fileStorageService, ExcelImportService excelImportService,
                               StoredFileRepository storedFileRepository, StoreRepository storeRepository) {
        this.fileStorageService = fileStorageService;
        this.excelImportService = excelImportService;
        this.storedFileRepository = storedFileRepository;
        this.storeRepository = storeRepository;
    }

    private UUID getUserIdFromAuth(Authentication authentication) {
        if (authentication.getPrincipal() instanceof JwtUser) {
            JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
            return UUID.fromString(jwtUser.getUserId());
        }
        // Fallback for backward compatibility
        return UUID.fromString(authentication.getName());
    }

    @PostMapping("/excel")
    public ResponseEntity<OrderImportBatch> uploadExcelFile(
            @PathVariable String storeId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        
        UUID uploaderId = getUserIdFromAuth(authentication);
        UUID storeUuid = UUID.fromString(storeId);
        var store = storeRepository.findById(storeUuid)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        // Store file
        String storagePath = fileStorageService.storeFile(file, store.getAccount().getId().toString(), storeId);

        // Save file metadata
        StoredFile storedFile = new StoredFile();
        storedFile.setAccountId(store.getAccount().getId().toString());
        storedFile.setStoreId(storeId);
        storedFile.setOriginalName(file.getOriginalFilename());
        storedFile.setStoragePath(storagePath);
        storedFile.setMimeType(file.getContentType());
        storedFile.setSizeBytes(file.getSize());
        storedFile = storedFileRepository.save(storedFile);

        // Process Excel file
        OrderImportBatch batch = excelImportService.processExcelFile(storeUuid, uploaderId, file, storagePath);

        return ResponseEntity.status(HttpStatus.CREATED).body(batch);
    }
}


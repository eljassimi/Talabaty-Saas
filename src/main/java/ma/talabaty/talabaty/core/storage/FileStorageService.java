package ma.talabaty.talabaty.core.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public String storeFile(MultipartFile file, String accountId, String storeId) throws IOException {
        // Create directory structure: uploads/accountId/storeId/
        Path accountPath = Paths.get(uploadDir, accountId);
        Path storePath = accountPath.resolve(storeId);
        Files.createDirectories(storePath);

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        // Save file
        Path targetPath = storePath.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Return relative path
        return Paths.get(accountId, storeId, filename).toString().replace("\\", "/");
    }

    public Path getFilePath(String relativePath) {
        return Paths.get(uploadDir, relativePath);
    }

    public void deleteFile(String relativePath) throws IOException {
        Path filePath = getFilePath(relativePath);
        Files.deleteIfExists(filePath);
    }
}


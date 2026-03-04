package ma.talabaty.talabaty.domain.orders.repository;

import ma.talabaty.talabaty.domain.orders.model.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, java.util.UUID> {
    Optional<StoredFile> findByStoreIdAndOriginalName(UUID storeId, String originalName);
}


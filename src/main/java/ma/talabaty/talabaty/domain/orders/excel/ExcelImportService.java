package ma.talabaty.talabaty.domain.orders.excel;

import ma.talabaty.talabaty.domain.orders.model.Order;
import ma.talabaty.talabaty.domain.orders.model.OrderImportBatch;
import ma.talabaty.talabaty.domain.orders.model.OrderSource;
import ma.talabaty.talabaty.domain.orders.model.ImportBatchStatus;
import ma.talabaty.talabaty.domain.orders.repository.OrderImportBatchRepository;
import ma.talabaty.talabaty.domain.orders.repository.OrderRepository;
import ma.talabaty.talabaty.domain.orders.repository.StoredFileRepository;
import ma.talabaty.talabaty.domain.stores.model.Store;
import ma.talabaty.talabaty.domain.stores.repository.StoreRepository;
import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ExcelImportService {

    private final OrderRepository orderRepository;
    private final OrderImportBatchRepository batchRepository;
    private final StoredFileRepository storedFileRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    public ExcelImportService(OrderRepository orderRepository, OrderImportBatchRepository batchRepository,
                             StoredFileRepository storedFileRepository, StoreRepository storeRepository,
                             UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.batchRepository = batchRepository;
        this.storedFileRepository = storedFileRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
    }

    public OrderImportBatch processExcelFile(UUID storeId, UUID uploaderId, MultipartFile file, String storagePath) throws IOException {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        User uploader = uploaderId != null ? userRepository.findById(uploaderId).orElse(null) : null;

        // Create import batch
        OrderImportBatch batch = new OrderImportBatch();
        batch.setStore(store);
        batch.setUploader(uploader);
        batch.setSourcePath(storagePath);
        batch.setStatus(ImportBatchStatus.PROCESSING);
        batch.setStartedAt(OffsetDateTime.now());
        batch.setAutoSyncEnabled(true); // Par défaut, le fichier est lié pour les mises à jour automatiques
        batch.setLastProcessedRow(0);
        batch = batchRepository.save(batch);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = sheet.getLastRowNum() + 1;
            int successCount = 0;
            int errorCount = 0;
            List<String> errors = new ArrayList<>();

            // Skip header row (row 0)
            for (int i = 1; i < totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    Order order = parseOrderRow(row, store, batch);
                    if (order != null) {
                        orderRepository.save(order);
                        successCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }

            batch.setStatus(errorCount == 0 ? ImportBatchStatus.COMPLETED : ImportBatchStatus.FAILED);
            batch.setCompletedAt(OffsetDateTime.now());
            batch.setLastProcessedRow(totalRows - 1); // Dernière ligne traitée (sans le header)
            batch.setLastSyncAt(OffsetDateTime.now());

            // Create summary JSON string
            String summary = String.format(
                "{\"totalRows\":%d,\"successCount\":%d,\"errorCount\":%d,\"errors\":%s}",
                totalRows - 1, successCount, errorCount, errors.toString()
            );
            batch.setSummary(summary);

            return batchRepository.save(batch);
        }
    }

    private Order parseOrderRow(Row row, Store store, OrderImportBatch batch) {
        Order order = new Order();
        order.setStore(store);
        order.setSource(OrderSource.EXCEL_UPLOAD);
        order.setImportBatch(batch);

        // Assuming Excel columns: Customer Name, Phone, Address, Amount, Currency, External Order ID
        order.setCustomerName(getCellValue(row, 0));
        order.setCustomerPhone(getCellValue(row, 1));
        order.setDestinationAddress(getCellValue(row, 2));

        String amountStr = getCellValue(row, 3);
        if (amountStr != null && !amountStr.isEmpty()) {
            try {
                order.setTotalAmount(new BigDecimal(amountStr));
            } catch (NumberFormatException e) {
                order.setTotalAmount(BigDecimal.ZERO);
            }
        } else {
            order.setTotalAmount(BigDecimal.ZERO);
        }

        order.setCurrency(getCellValue(row, 4) != null ? getCellValue(row, 4) : "USD");
        order.setExternalOrderId(getCellValue(row, 5));

        return order;
    }

    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return null;
        }

        CellType cellType = cell.getCellType();
        if (cellType == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cellType == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toString();
            } else {
                double numericValue = cell.getNumericCellValue();
                // Check if it's a whole number
                if (numericValue == Math.floor(numericValue)) {
                    return String.valueOf((long) numericValue);
                } else {
                    return String.valueOf(numericValue);
                }
            }
        } else if (cellType == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else if (cellType == CellType.FORMULA) {
            return cell.getCellFormula();
        } else {
            return null;
        }
    }
}


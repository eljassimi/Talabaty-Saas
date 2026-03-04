package ma.talabaty.talabaty.domain.orders.sync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.talabaty.talabaty.domain.orders.model.Order;
import ma.talabaty.talabaty.domain.orders.model.OrderSource;
import ma.talabaty.talabaty.domain.orders.model.OrderStatus;
import ma.talabaty.talabaty.domain.orders.repository.OrderRepository;
import ma.talabaty.talabaty.domain.orders.sync.model.ExcelSyncConfig;
import ma.talabaty.talabaty.domain.orders.sync.repository.ExcelSyncConfigRepository;
import ma.talabaty.talabaty.domain.stores.repository.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@Transactional
public class GoogleSheetsSyncService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsSyncService.class);
    private final ExcelSyncConfigRepository syncConfigRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final GoogleSheetsService googleSheetsService;
    private final ObjectMapper objectMapper;

    public GoogleSheetsSyncService(ExcelSyncConfigRepository syncConfigRepository,
                                  OrderRepository orderRepository,
                                  StoreRepository storeRepository,
                                  GoogleSheetsService googleSheetsService) {
        this.syncConfigRepository = syncConfigRepository;
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
        this.googleSheetsService = googleSheetsService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Synchronize a specific Google Sheet based on its configuration
     */
    public SyncResult syncGoogleSheet(UUID configId) {
        ExcelSyncConfig config = syncConfigRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("Google Sheet sync config not found"));

        if (!config.getSyncEnabled()) {
            return new SyncResult(false, "Sync is disabled for this configuration", 0, 0, 0);
        }

        try {
            // Read data from Google Sheet
            List<List<Object>> sheetData = googleSheetsService.readSheetData(
                    config.getSpreadsheetId(),
                    config.getSheetName(),
                    config.getCredentialsJson(),
                    config.getAccessToken(),
                    config.getRefreshToken()
            );

            if (sheetData.isEmpty()) {
                updateSyncStatus(config, "SUCCESS", null);
                return new SyncResult(true, "Sheet is empty", 0, 0, 0);
            }

            // Check if sheet has changed (compare row count)
            int currentRowCount = sheetData.size();
            if (config.getLastSyncedRowCount() != null && 
                config.getLastSyncedRowCount().equals(currentRowCount) &&
                config.getLastSyncStatus() != null &&
                "SUCCESS".equals(config.getLastSyncStatus())) {
                logger.debug("Sheet {} has not changed (same row count), skipping sync", config.getSpreadsheetId());
                return new SyncResult(true, "No changes detected", 0, 0, 0);
            }

            // Parse column mapping
            Map<String, Integer> columnMap = parseColumnMapping(config.getColumnMapping(), sheetData);

            // Process sheet data
            SyncResult result = processSheetData(sheetData, config, columnMap);

            // Update config
            config.setLastSyncedRowCount(currentRowCount);
            config.setLastSyncAt(OffsetDateTime.now());
            config.setLastSyncStatus(result.isSuccess() ? "SUCCESS" : "ERROR");
            config.setLastSyncError(result.isSuccess() ? null : result.getMessage());
            syncConfigRepository.save(config);

            return result;

        } catch (Exception e) {
            logger.error("Error syncing Google Sheet: {}", config.getSpreadsheetId(), e);
            String error = "Error: " + e.getMessage();
            updateSyncStatus(config, "ERROR", error);
            return new SyncResult(false, error, 0, 0, 0);
        }
    }

    /**
     * Process sheet data and sync orders
     */
    private SyncResult processSheetData(List<List<Object>> sheetData, 
                                       ExcelSyncConfig config, 
                                       Map<String, Integer> columnMap) {
        int created = 0;
        int updated = 0;
        int errors = 0;

        if (sheetData.size() < 2) {
            return new SyncResult(true, "Sheet has no data rows", 0, 0, 0);
        }

        // Process data rows (skip header row at index 0)
        for (int i = 1; i < sheetData.size(); i++) {
            List<Object> row = sheetData.get(i);
            if (row == null || row.isEmpty()) continue;

            try {
                OrderSyncResult rowResult = processOrderRow(row, config, columnMap);
                if (rowResult.isCreated()) {
                    created++;
                } else if (rowResult.isUpdated()) {
                    updated++;
                }
            } catch (Exception e) {
                errors++;
                logger.warn("Error processing row {}: {}", i + 1, e.getMessage());
            }
        }

        String message = String.format("Synced: %d created, %d updated, %d errors", created, updated, errors);
        return new SyncResult(true, message, created, updated, errors);
    }

    /**
     * Process a single order row
     */
    private OrderSyncResult processOrderRow(List<Object> row, 
                                           ExcelSyncConfig config, 
                                           Map<String, Integer> columnMap) {
        // Extract order identifier
        String externalOrderId = getCellValueAsString(row, columnMap.getOrDefault("externalOrderId", -1));
        
        // Try to find existing order
        Order existingOrder = null;
        if (externalOrderId != null && !externalOrderId.trim().isEmpty()) {
            existingOrder = orderRepository.findByStoreIdAndExternalOrderId(
                    config.getStore().getId(), externalOrderId).orElse(null);
        }

        // Extract order data
        String customerName = getCellValueAsString(row, columnMap.getOrDefault("customerName", 0));
        String customerPhone = getCellValueAsString(row, columnMap.getOrDefault("customerPhone", 1));
        String destinationAddress = getCellValueAsString(row, columnMap.getOrDefault("destinationAddress", 2));
        String productName = getCellValueAsString(row, columnMap.getOrDefault("productName", -1));
        String productId = getCellValueAsString(row, columnMap.getOrDefault("productId", -1));
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        Integer amountCol = columnMap.get("totalAmount");
        
        // DEBUG: Log column mapping
        logger.info("DEBUG: Column mapping for order row: {}", columnMap);
        logger.info("DEBUG: Looking for totalAmount column, found index: {}", amountCol);
        
        if (amountCol != null && amountCol >= 0 && amountCol < row.size()) {
            String amountStr = getCellValueAsString(row, amountCol);
            logger.info("DEBUG: Raw amount string from column {}: '{}'", amountCol, amountStr);
            
            if (amountStr != null && !amountStr.isEmpty()) {
                try {
                    // Clean the string: remove spaces, replace comma with dot, remove currency symbols
                    String cleanedAmount = amountStr
                        .trim()
                        .replace(",", ".")
                        .replace(" ", "")
                        .replace("€", "")
                        .replace("$", "")
                        .replace("MAD", "")
                        .replace("USD", "")
                        .replace("EUR", "")
                        .replace("د.م.", "")
                        .replace("د.م", "")
                        .replace("DH", "")
                        .replace("dh", "")
                        .replace("Dh", "");
                    logger.info("DEBUG: Cleaned amount string: '{}'", cleanedAmount);
                    totalAmount = new BigDecimal(cleanedAmount);
                    logger.info("Parsed totalAmount from '{}' to {}", amountStr, totalAmount);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid amount format: '{}'. Error: {}", amountStr, e.getMessage());
                }
            } else {
                logger.warn("Amount column found but value is null or empty. Column index: {}", amountCol);
            }
        } else {
            logger.warn("No totalAmount column found in column mapping. Available columns: {}", columnMap.keySet());
            // Try to find price column manually by checking all columns
            logger.info("DEBUG: Attempting to find price column manually...");
            for (int i = 0; i < row.size(); i++) {
                String cellValue = getCellValueAsString(row, i);
                if (cellValue != null) {
                    // Check if this looks like a price (contains numbers and possibly currency symbols)
                    String cleaned = cellValue.trim().replace(",", ".").replace(" ", "")
                        .replace("€", "").replace("$", "").replace("MAD", "").replace("USD", "")
                        .replace("EUR", "").replace("د.م.", "").replace("د.م", "").replace("DH", "")
                        .replace("dh", "").replace("Dh", "");
                    try {
                        double testValue = Double.parseDouble(cleaned);
                        if (testValue > 0) {
                            logger.info("DEBUG: Found potential price in column {}: '{}' -> {}", i, cellValue, testValue);
                            totalAmount = new BigDecimal(cleaned);
                            logger.info("DEBUG: Using column {} as price column with value: {}", i, totalAmount);
                            break;
                        }
                    } catch (NumberFormatException e) {
                        // Not a number, continue
                    }
                }
            }
        }

        String currency = getCellValueAsString(row, columnMap.getOrDefault("currency", -1));
        if (currency == null || currency.isEmpty()) {
            currency = "USD";
        }

        String statusStr = getCellValueAsString(row, columnMap.getOrDefault("status", -1));
        OrderStatus status = parseOrderStatus(statusStr);

        if (existingOrder != null) {
            // Update existing order
            boolean hasChanges = false;
            
            if (customerName != null && !customerName.equals(existingOrder.getCustomerName())) {
                existingOrder.setCustomerName(customerName);
                hasChanges = true;
            }
            if (customerPhone != null && !customerPhone.equals(existingOrder.getCustomerPhone())) {
                existingOrder.setCustomerPhone(customerPhone);
                hasChanges = true;
            }
            if (destinationAddress != null && !destinationAddress.equals(existingOrder.getDestinationAddress())) {
                existingOrder.setDestinationAddress(destinationAddress);
                hasChanges = true;
            }
            if (productName != null && !productName.equals(existingOrder.getProductName())) {
                existingOrder.setProductName(productName);
                hasChanges = true;
            }
            if (productId != null && !productId.equals(existingOrder.getProductId())) {
                existingOrder.setProductId(productId);
                hasChanges = true;
            }
            if (totalAmount.compareTo(existingOrder.getTotalAmount()) != 0) {
                existingOrder.setTotalAmount(totalAmount);
                hasChanges = true;
            }
            if (currency != null && !currency.equals(existingOrder.getCurrency())) {
                existingOrder.setCurrency(currency);
                hasChanges = true;
            }
            if (status != null && status != existingOrder.getStatus()) {
                existingOrder.setStatus(status);
                hasChanges = true;
            }

            if (hasChanges) {
                orderRepository.save(existingOrder);
                return new OrderSyncResult(false, true);
            }
            return new OrderSyncResult(false, false);
        } else {
            // Create new order
            Order newOrder = new Order();
            newOrder.setStore(config.getStore());
            newOrder.setSource(OrderSource.EXCEL_UPLOAD);
            newOrder.setCustomerName(customerName);
            newOrder.setCustomerPhone(customerPhone);
            newOrder.setDestinationAddress(destinationAddress);
            newOrder.setProductName(productName);
            newOrder.setProductId(productId);
            newOrder.setTotalAmount(totalAmount);
            newOrder.setCurrency(currency);
            newOrder.setExternalOrderId(externalOrderId);
            newOrder.setStatus(status != null ? status : OrderStatus.ENCOURS);
            
            // DEBUG: Log the order being created
            logger.info("Creating new order with totalAmount: {} (BigDecimal: {})", totalAmount, totalAmount);
            logger.info("Order details - Customer: {}, Phone: {}, City: {}, Price: {}", 
                customerName, customerPhone, 
                getCellValueAsString(row, columnMap.getOrDefault("city", -1)), 
                totalAmount);

            Order savedOrder = orderRepository.save(newOrder);
            logger.info("Order saved with ID: {}, totalAmount in DB: {}", savedOrder.getId(), savedOrder.getTotalAmount());
            return new OrderSyncResult(true, false);
        }
    }

    /**
     * Auto-detect column mapping from header row
     */
    private Map<String, Integer> autoDetectColumns(List<Object> headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        for (int i = 0; i < headerRow.size(); i++) {
            Object cellValue = headerRow.get(i);
            if (cellValue == null) continue;
            
            String headerValue = cellValue.toString().toLowerCase().trim();
            
            // Map common column names
            if (headerValue.contains("customer") && headerValue.contains("name")) {
                columnMap.put("customerName", i);
            } else if (headerValue.contains("phone") || headerValue.contains("tel")) {
                columnMap.put("customerPhone", i);
            } else if (headerValue.contains("address") || headerValue.contains("adresse")) {
                columnMap.put("destinationAddress", i);
            } else if (headerValue.contains("amount") || headerValue.contains("prix") || headerValue.contains("total")) {
                columnMap.put("totalAmount", i);
            } else if (headerValue.contains("currency") || headerValue.contains("devise")) {
                columnMap.put("currency", i);
            } else if (headerValue.contains("order") && headerValue.contains("id")) {
                columnMap.put("externalOrderId", i);
            } else if (headerValue.contains("product") && headerValue.contains("name")) {
                columnMap.put("productName", i);
            } else if (headerValue.contains("product") && headerValue.contains("id")) {
                columnMap.put("productId", i);
            } else if (headerValue.contains("status") || headerValue.contains("statut")) {
                columnMap.put("status", i);
            }
        }
        
        return columnMap;
    }

    /**
     * Parse column mapping from JSON or auto-detect from header
     */
    private Map<String, Integer> parseColumnMapping(String columnMappingJson, List<List<Object>> sheetData) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        // If mapping is provided, use it
        if (columnMappingJson != null && !columnMappingJson.trim().isEmpty()) {
            try {
                JsonNode mappingNode = objectMapper.readTree(columnMappingJson);
                Iterator<Map.Entry<String, JsonNode>> fields = mappingNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String fieldName = entry.getKey();
                    JsonNode valueNode = entry.getValue();
                    if (valueNode.isNumber()) {
                        columnMap.put(fieldName, valueNode.asInt());
                    } else if (valueNode.isTextual()) {
                        try {
                            columnMap.put(fieldName, Integer.parseInt(valueNode.asText()));
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error parsing column mapping: {}", e.getMessage());
            }
        }
        
        // If no mapping or incomplete mapping, auto-detect from header
        if (columnMap.isEmpty() && !sheetData.isEmpty()) {
            List<Object> headerRow = sheetData.get(0);
            columnMap = autoDetectColumns(headerRow);
        }
        
        return columnMap;
    }

    /**
     * Get cell value as string
     */
    private String getCellValueAsString(List<Object> row, int cellIndex) {
        if (cellIndex < 0 || cellIndex >= row.size()) return null;
        
        Object cellValue = row.get(cellIndex);
        if (cellValue == null) return null;
        
        return cellValue.toString().trim();
    }

    /**
     * Parse order status from string
     */
    private OrderStatus parseOrderStatus(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return null;
        }

        String normalized = statusStr.toUpperCase().trim();
        try {
            return OrderStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try common variations
            if (normalized.contains("ENCOURS") || normalized.contains("EN COURS")) {
                return OrderStatus.ENCOURS;
            } else if (normalized.contains("CONFIRMED") || normalized.contains("CONFIRME")) {
                return OrderStatus.CONFIRMED;
            } else if (normalized.contains("CONCLED") || normalized.contains("CONCLU")) {
                return OrderStatus.CONCLED;
            } else if (normalized.contains("APPEL") && normalized.contains("1")) {
                return OrderStatus.APPEL_1;
            } else if (normalized.contains("APPEL") && normalized.contains("2")) {
                return OrderStatus.APPEL_2;
            }
        }
        return null;
    }

    /**
     * Update sync status
     */
    private void updateSyncStatus(ExcelSyncConfig config, String status, String error) {
        config.setLastSyncAt(OffsetDateTime.now());
        config.setLastSyncStatus(status);
        config.setLastSyncError(error);
        syncConfigRepository.save(config);
    }

    /**
     * Sync all enabled configurations
     */
    public void syncAllEnabled() {
        List<ExcelSyncConfig> configs = syncConfigRepository.findAllEnabled();
        logger.info("Syncing {} Google Sheets", configs.size());
        
        for (ExcelSyncConfig config : configs) {
            try {
                syncGoogleSheet(config.getId());
            } catch (Exception e) {
                logger.error("Error syncing config {}: {}", config.getId(), e.getMessage());
            }
        }
    }

    // Inner classes for results
    public static class SyncResult {
        private final boolean success;
        private final String message;
        private final int created;
        private final int updated;
        private final int errors;

        public SyncResult(boolean success, String message, int created, int updated, int errors) {
            this.success = success;
            this.message = message;
            this.created = created;
            this.updated = updated;
            this.errors = errors;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getCreated() { return created; }
        public int getUpdated() { return updated; }
        public int getErrors() { return errors; }
    }

    private static class OrderSyncResult {
        private final boolean created;
        private final boolean updated;

        public OrderSyncResult(boolean created, boolean updated) {
            this.created = created;
            this.updated = updated;
        }

        public boolean isCreated() { return created; }
        public boolean isUpdated() { return updated; }
    }
}


package ma.talabaty.talabaty.core.scheduling;

import ma.talabaty.talabaty.domain.orders.sync.service.GoogleSheetsSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GoogleSheetsSyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsSyncScheduler.class);
    private final GoogleSheetsSyncService syncService;

    public GoogleSheetsSyncScheduler(GoogleSheetsSyncService syncService) {
        this.syncService = syncService;
    }

    /**
     * Run sync every 30 seconds for all enabled Google Sheets
     * This can be adjusted based on requirements
     */
    @Scheduled(fixedRate = 30000) // 30 seconds
    public void syncGoogleSheets() {
        try {
            logger.debug("Running scheduled Google Sheets sync");
            syncService.syncAllEnabled();
        } catch (Exception e) {
            logger.error("Error in scheduled Google Sheets sync", e);
        }
    }
}


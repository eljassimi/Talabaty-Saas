package ma.talabaty.talabaty.core.scheduling;

import ma.talabaty.talabaty.domain.youcan.model.YouCanStore;
import ma.talabaty.talabaty.domain.youcan.repository.YouCanStoreRepository;
import ma.talabaty.talabaty.domain.youcan.service.YouCanOrderSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class YouCanOrderSyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(YouCanOrderSyncScheduler.class);
    private final YouCanStoreRepository youCanStoreRepository;
    private final YouCanOrderSyncService youCanOrderSyncService;

    public YouCanOrderSyncScheduler(
            YouCanStoreRepository youCanStoreRepository,
            YouCanOrderSyncService youCanOrderSyncService) {
        this.youCanStoreRepository = youCanStoreRepository;
        this.youCanOrderSyncService = youCanOrderSyncService;
    }

    
    @Scheduled(fixedRate = 300000) 
    public void syncAllYouCanStores() {
        try {
            logger.debug("Running scheduled YouCan orders sync");
            
            
            List<YouCanStore> activeStores = youCanStoreRepository.findAll().stream()
                    .filter(YouCanStore::isActive)
                    .toList();
            
            if (activeStores.isEmpty()) {
                logger.debug("No active YouCan stores to sync");
                return;
            }
            
            logger.info("Syncing orders from {} active YouCan store(s)", activeStores.size());
            
            int totalSynced = 0;
            int successCount = 0;
            int errorCount = 0;
            
            for (YouCanStore youCanStore : activeStores) {
                try {
                    logger.debug("Syncing orders from YouCan store: {} (ID: {})", 
                            youCanStore.getYoucanStoreName(), youCanStore.getId());
                    
                    int syncedCount = youCanOrderSyncService.syncOrdersFromYouCanStore(youCanStore.getId());
                    totalSynced += syncedCount;
                    successCount++;
                    
                    logger.info("Successfully synced {} orders from YouCan store: {}", 
                            syncedCount, youCanStore.getYoucanStoreName());
                } catch (Exception e) {
                    errorCount++;
                    logger.error("Failed to sync orders from YouCan store {} (ID: {}): {}", 
                            youCanStore.getYoucanStoreName(), 
                            youCanStore.getId(), 
                            e.getMessage(), 
                            e);
                }
            }
            
            logger.info("YouCan sync completed: {} stores processed, {} successful, {} errors, {} total orders synced", 
                    activeStores.size(), successCount, errorCount, totalSynced);
        } catch (Exception e) {
            logger.error("Error in scheduled YouCan orders sync", e);
        }
    }
}


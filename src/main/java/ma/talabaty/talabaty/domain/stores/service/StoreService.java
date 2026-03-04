package ma.talabaty.talabaty.domain.stores.service;

import ma.talabaty.talabaty.domain.accounts.model.Account;
import ma.talabaty.talabaty.domain.accounts.repository.AccountRepository;
import ma.talabaty.talabaty.domain.stores.model.Store;
import ma.talabaty.talabaty.domain.stores.model.StoreStatus;
import ma.talabaty.talabaty.domain.stores.repository.StoreRepository;
import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.model.UserRole;
import ma.talabaty.talabaty.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public StoreService(StoreRepository storeRepository, AccountRepository accountRepository, UserRepository userRepository) {
        this.storeRepository = storeRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public Store createStore(UUID accountId, String name, UUID managerId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (storeRepository.existsByAccountIdAndName(accountId, name)) {
            throw new RuntimeException("Store with name " + name + " already exists for this account");
        }

        // Generate unique store code
        String code = generateUniqueStoreCode(accountId);

        Store store = new Store();
        store.setAccount(account);
        store.setName(name);
        store.setCode(code);
        store.setStatus(StoreStatus.ACTIVE);

        // Set manager only if provided
        if (managerId != null) {
            User manager = userRepository.findById(managerId)
                    .orElseThrow(() -> new RuntimeException("Manager not found"));
            store.setManager(manager);
        }

        return storeRepository.save(store);
    }

    private String generateUniqueStoreCode(UUID accountId) {
        // Generate a unique code based on account ID and timestamp
        // Format: ST-{first8charsOfAccountId}-{timestamp}
        String accountPrefix = accountId.toString().substring(0, 8).toUpperCase();
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7); // Last 6 digits
        String code = "ST-" + accountPrefix + "-" + timestamp;
        
        // Ensure uniqueness by checking if code exists
        int attempts = 0;
        while (storeRepository.existsByCode(code) && attempts < 10) {
            code = "ST-" + accountPrefix + "-" + timestamp + "-" + attempts;
            attempts++;
        }
        
        if (storeRepository.existsByCode(code)) {
            // Fallback: use UUID short format
            code = "ST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        
        return code;
    }

    public List<Store> findByAccountId(UUID accountId) {
        return storeRepository.findByAccountId(accountId);
    }

    /**
     * Get stores accessible by a user based on their role and team memberships:
     * - ACCOUNT_OWNER or PLATFORM_ADMIN: all stores in the account + stores where they are team members
     * - MANAGER: stores where the user is the manager + stores where they are team members
     * - SUPPORT: stores where the user is a team member
     * 
     * This allows users to work with stores across different accounts if they are team members.
     */
    public List<Store> findStoresForUser(UUID accountId, UUID userId, UserRole userRole) {
        java.util.Set<Store> stores = new java.util.HashSet<>();
        
        // Get stores from account (for ACCOUNT_OWNER and PLATFORM_ADMIN)
        if (userRole == UserRole.ACCOUNT_OWNER || userRole == UserRole.PLATFORM_ADMIN) {
            stores.addAll(storeRepository.findByAccountId(accountId));
        }
        
        // Get stores where user is manager
        if (userRole == UserRole.MANAGER || userRole == UserRole.ACCOUNT_OWNER || userRole == UserRole.PLATFORM_ADMIN) {
            stores.addAll(storeRepository.findByManagerId(userId));
        }
        
        // Get stores where user is a team member (across all accounts)
        stores.addAll(storeRepository.findByTeamMemberUserId(userId));
        
        return new java.util.ArrayList<>(stores);
    }

    public Optional<Store> findById(UUID id) {
        return storeRepository.findById(id);
    }

    public Optional<Store> findByAccountIdAndId(UUID accountId, UUID id) {
        return storeRepository.findByAccountIdAndId(accountId, id);
    }

    public Store updateStore(UUID id, String name, String timezone) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        if (name != null) {
            store.setName(name);
        }
        if (timezone != null) {
            store.setTimezone(timezone);
        }
        return storeRepository.save(store);
    }

    public void deleteStore(UUID id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        store.setStatus(StoreStatus.DELETED);
        storeRepository.save(store);
    }

    public Store save(Store store) {
        return storeRepository.save(store);
    }
}


package ma.talabaty.talabaty.domain.shipping.service;

import ma.talabaty.talabaty.domain.accounts.model.Account;
import ma.talabaty.talabaty.domain.accounts.repository.AccountRepository;
import ma.talabaty.talabaty.domain.shipping.model.ProviderType;
import ma.talabaty.talabaty.domain.shipping.model.ShippingProvider;
import ma.talabaty.talabaty.domain.shipping.repository.ShippingProviderRepository;
import ma.talabaty.talabaty.domain.stores.model.Store;
import ma.talabaty.talabaty.domain.stores.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ShippingProviderService {

    private final ShippingProviderRepository providerRepository;
    private final AccountRepository accountRepository;
    private final StoreRepository storeRepository;

    public ShippingProviderService(ShippingProviderRepository providerRepository, AccountRepository accountRepository,
                                  StoreRepository storeRepository) {
        this.providerRepository = providerRepository;
        this.accountRepository = accountRepository;
        this.storeRepository = storeRepository;
    }

    public ShippingProvider createProvider(UUID accountId, ProviderType providerType, String customerId, 
                                          String apiKey, String displayName) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        ShippingProvider provider = new ShippingProvider();
        provider.setAccount(account);
        provider.setProviderType(providerType);
        provider.setCustomerId(customerId);
        provider.setApiKey(apiKey);
        provider.setDisplayName(displayName != null ? displayName : providerType.name());
        provider.setActive(true);

        return providerRepository.save(provider);
    }

    public ShippingProvider createProviderForStore(UUID accountId, UUID storeId, ProviderType providerType, 
                                                   String customerId, String apiKey, String displayName) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        
        // Verify store belongs to account
        if (!store.getAccount().getId().equals(accountId)) {
            throw new RuntimeException("Store does not belong to the account");
        }

        ShippingProvider provider = new ShippingProvider();
        provider.setAccount(account);
        provider.setStore(store);
        provider.setProviderType(providerType);
        provider.setCustomerId(customerId);
        provider.setApiKey(apiKey);
        provider.setDisplayName(displayName != null ? displayName : providerType.name());
        provider.setActive(true);

        return providerRepository.save(provider);
    }

    public List<ShippingProvider> getAccountProviders(UUID accountId) {
        return providerRepository.findByAccountId(accountId);
    }

    public List<ShippingProvider> getStoreProviders(UUID storeId) {
        return providerRepository.findByStoreId(storeId);
    }

    public Optional<ShippingProvider> getActiveProvider(UUID accountId, ProviderType providerType) {
        List<ShippingProvider> providers = providerRepository.findByAccountIdAndProviderType(accountId, providerType);
        // Return the most recently created one if multiple exist
        return providers.isEmpty() ? Optional.empty() : Optional.of(providers.get(0));
    }

    /**
     * Get active provider for a store. First tries store-level, then falls back to account-level.
     */
    public Optional<ShippingProvider> getActiveProviderForStore(UUID storeId, ProviderType providerType) {
        // First try store-level provider
        List<ShippingProvider> storeProviders = providerRepository.findByStoreIdAndProviderType(storeId, providerType);
        if (!storeProviders.isEmpty()) {
            // Return the most recently created one if multiple exist
            return Optional.of(storeProviders.get(0));
        }
        
        // Fallback to account-level provider
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        List<ShippingProvider> accountProviders = providerRepository.findByAccountIdAndProviderType(store.getAccount().getId(), providerType);
        // Return the most recently created one if multiple exist
        return accountProviders.isEmpty() ? Optional.empty() : Optional.of(accountProviders.get(0));
    }

    public ShippingProvider updateProvider(UUID providerId, String customerId, String apiKey, String displayName, Boolean active) {
        ShippingProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Shipping provider not found"));

        if (customerId != null) {
            provider.setCustomerId(customerId);
        }
        if (apiKey != null) {
            provider.setApiKey(apiKey);
        }
        if (displayName != null) {
            provider.setDisplayName(displayName);
        }
        if (active != null) {
            provider.setActive(active);
        }

        return providerRepository.save(provider);
    }

    public void deleteProvider(UUID providerId) {
        ShippingProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Shipping provider not found"));
        providerRepository.delete(provider);
    }
}


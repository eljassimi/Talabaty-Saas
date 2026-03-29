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
        
        return providers.isEmpty() ? Optional.empty() : Optional.of(providers.get(0));
    }

    
    public Optional<ShippingProvider> getActiveProviderForStore(UUID storeId, ProviderType providerType) {
        List<ShippingProvider> storeProviders = providerRepository.findByStoreIdAndProviderType(storeId, providerType);
        if (!storeProviders.isEmpty()) {
            
            return Optional.of(storeProviders.get(0));
        }
        return Optional.empty();
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


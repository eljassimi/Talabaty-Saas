package ma.talabaty.talabaty.core.security;

import ma.talabaty.talabaty.domain.stores.model.Store;
import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.model.UserRole;
import ma.talabaty.talabaty.domain.users.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PermissionChecker {

    private final UserRepository userRepository;

    public PermissionChecker(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    
    public boolean canCreateStore(UserRole role) {
        return role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER;
    }

    
    public boolean canUpdateStore(UserRole role, UUID userId, Store store) {
        if (role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER) {
            return true;
        }
        if (role == UserRole.MANAGER) {
            return store.getManager() != null && store.getManager().getId().equals(userId);
        }
        return false;
    }

    
    public boolean canDeleteStore(UserRole role) {
        return role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER;
    }

    
    public boolean canViewStore(UserRole role, UUID userId, Store store) {
        if (role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER) {
            return true;
        }
        
        if (store.getManager() != null && store.getManager().getId().equals(userId)) {
            return true;
        }
        
        return store.getTeamMembers().stream()
                .anyMatch(tm -> tm.getUser() != null && tm.getUser().getId().equals(userId));
    }

    
    public boolean canManageUsers(UserRole role) {
        return role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER;
    }

    
    public boolean canManageTeamMembers(UserRole role, UUID userId, Store store) {
        if (role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER) {
            return true;
        }
        if (role == UserRole.MANAGER) {
            
            return store.getManager() != null && store.getManager().getId().equals(userId);
        }
        return false;
    }

    
    public boolean canCreateOrder(UserRole role) {
        
        return true;
    }

    
    public boolean canUpdateOrder(UserRole role) {
        
        return role == UserRole.PLATFORM_ADMIN || 
               role == UserRole.ACCOUNT_OWNER || 
               role == UserRole.MANAGER || 
               role == UserRole.SUPPORT;
    }

    
    public boolean canUpdateOrder(UserRole role, UUID userId, ma.talabaty.talabaty.domain.orders.model.Order order) {
        if (role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER || role == UserRole.MANAGER) {
            return true;
        }
        if (role == UserRole.SUPPORT) {
            
            return order.getAssignedTo() == null || 
                   (order.getAssignedTo() != null && order.getAssignedTo().getId().equals(userId));
        }
        return false;
    }

    
    public boolean canManageShippingProviders(UserRole role) {
        return role == UserRole.PLATFORM_ADMIN || 
               role == UserRole.ACCOUNT_OWNER || 
               role == UserRole.MANAGER;
    }

    
    public boolean canViewShippingProviders(UserRole role) {
        
        return true;
    }

    
    public boolean canManageApiCredentials(UserRole role) {
        return role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER;
    }

    
    public boolean canUploadFiles(UserRole role) {
        return role == UserRole.PLATFORM_ADMIN || 
               role == UserRole.ACCOUNT_OWNER || 
               role == UserRole.MANAGER;
    }

    
    public boolean canAccessIntegrations(UserRole role) {
        return role == UserRole.PLATFORM_ADMIN ||
               role == UserRole.ACCOUNT_OWNER ||
               role == UserRole.MANAGER;
    }

    
    public boolean canManagePaymentRequests(UserRole role) {
        return role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER;
    }
}


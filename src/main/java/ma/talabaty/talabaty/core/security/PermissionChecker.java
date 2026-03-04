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

    /**
     * Check if user can create stores
     */
    public boolean canCreateStore(UserRole role) {
        return role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER;
    }

    /**
     * Check if user can update stores
     */
    public boolean canUpdateStore(UserRole role, UUID userId, Store store) {
        if (role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER) {
            return true;
        }
        if (role == UserRole.MANAGER) {
            return store.getManager() != null && store.getManager().getId().equals(userId);
        }
        return false;
    }

    /**
     * Check if user can delete stores
     */
    public boolean canDeleteStore(UserRole role) {
        return role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER;
    }

    /**
     * Check if user can view a specific store
     * Users can view stores if:
     * - They are ACCOUNT_OWNER or PLATFORM_ADMIN (always allowed)
     * - They are the manager of the store
     * - They are a team member of the store (across any account)
     */
    public boolean canViewStore(UserRole role, UUID userId, Store store) {
        if (role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER) {
            return true;
        }
        // Check if user is the manager
        if (store.getManager() != null && store.getManager().getId().equals(userId)) {
            return true;
        }
        // Check if user is a team member (works across accounts)
        return store.getTeamMembers().stream()
                .anyMatch(tm -> tm.getUser() != null && tm.getUser().getId().equals(userId));
    }

    /**
     * Check if user can manage users (create, update, ban, etc.)
     */
    public boolean canManageUsers(UserRole role) {
        return role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER;
    }

    /**
     * Check if user can manage team members
     */
    public boolean canManageTeamMembers(UserRole role, UUID userId, Store store) {
        if (role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER) {
            return true;
        }
        if (role == UserRole.MANAGER) {
            // Manager can manage team members of stores they manage
            return store.getManager() != null && store.getManager().getId().equals(userId);
        }
        return false;
    }

    /**
     * Check if user can create orders
     */
    public boolean canCreateOrder(UserRole role) {
        // All authenticated users can create orders
        return true;
    }

    /**
     * Check if user can update orders
     */
    public boolean canUpdateOrder(UserRole role) {
        // SUPPORT and above can update orders
        return role == UserRole.PLATFORM_ADMIN || 
               role == UserRole.ACCOUNT_OWNER || 
               role == UserRole.MANAGER || 
               role == UserRole.SUPPORT;
    }

    /**
     * Check if user can update a specific order
     * For SUPPORT: can only update if order is not assigned or assigned to them
     * For others: can always update
     */
    public boolean canUpdateOrder(UserRole role, UUID userId, ma.talabaty.talabaty.domain.orders.model.Order order) {
        if (role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER || role == UserRole.MANAGER) {
            return true;
        }
        if (role == UserRole.SUPPORT) {
            // Support can only update if order is not assigned or assigned to them
            return order.getAssignedTo() == null || 
                   (order.getAssignedTo() != null && order.getAssignedTo().getId().equals(userId));
        }
        return false;
    }

    /**
     * Check if user can manage shipping providers
     */
    public boolean canManageShippingProviders(UserRole role) {
        return role == UserRole.PLATFORM_ADMIN || 
               role == UserRole.ACCOUNT_OWNER || 
               role == UserRole.MANAGER;
    }

    /**
     * Check if user can view shipping providers
     */
    public boolean canViewShippingProviders(UserRole role) {
        // All authenticated users can view shipping providers
        return true;
    }

    /**
     * Check if user can manage API credentials
     */
    public boolean canManageApiCredentials(UserRole role) {
        return role == UserRole.PLATFORM_ADMIN || role == UserRole.ACCOUNT_OWNER;
    }

    /**
     * Check if user can upload files
     */
    public boolean canUploadFiles(UserRole role) {
        return role == UserRole.PLATFORM_ADMIN || 
               role == UserRole.ACCOUNT_OWNER || 
               role == UserRole.MANAGER;
    }
}


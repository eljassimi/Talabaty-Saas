// Permission utilities based on user roles

export enum UserRole {
  PLATFORM_ADMIN = 'PLATFORM_ADMIN',
  ACCOUNT_OWNER = 'ACCOUNT_OWNER',
  MANAGER = 'MANAGER',
  SUPPORT = 'SUPPORT'
}

export interface Permissions {
  canCreateStore: boolean
  canUpdateStore: boolean
  canDeleteStore: boolean
  canManageUsers: boolean
  canManageTeamMembers: boolean
  canCreateOrder: boolean
  canUpdateOrder: boolean
  canManageShippingProviders: boolean
  canUploadFiles: boolean
}

export function getPermissions(role: string | undefined): Permissions {
  if (!role) {
    return {
      canCreateStore: false,
      canUpdateStore: false,
      canDeleteStore: false,
      canManageUsers: false,
      canManageTeamMembers: false,
      canCreateOrder: false,
      canUpdateOrder: false,
      canManageShippingProviders: false,
      canUploadFiles: false,
    }
  }

  const userRole = role as UserRole

  return {
    // Store permissions
    canCreateStore: userRole === UserRole.PLATFORM_ADMIN || userRole === UserRole.ACCOUNT_OWNER,
    canUpdateStore: userRole === UserRole.PLATFORM_ADMIN || userRole === UserRole.ACCOUNT_OWNER || userRole === UserRole.MANAGER,
    canDeleteStore: userRole === UserRole.PLATFORM_ADMIN || userRole === UserRole.ACCOUNT_OWNER,
    
    // User management permissions
    canManageUsers: userRole === UserRole.PLATFORM_ADMIN || userRole === UserRole.ACCOUNT_OWNER,
    
    // Team management permissions
    canManageTeamMembers: userRole === UserRole.PLATFORM_ADMIN || userRole === UserRole.ACCOUNT_OWNER || userRole === UserRole.MANAGER,
    
    // Order permissions
    canCreateOrder: true, // All authenticated users can create orders
    canUpdateOrder: userRole === UserRole.PLATFORM_ADMIN || 
                    userRole === UserRole.ACCOUNT_OWNER || 
                    userRole === UserRole.MANAGER || 
                    userRole === UserRole.SUPPORT,
    
    // Shipping provider permissions
    canManageShippingProviders: userRole === UserRole.PLATFORM_ADMIN || 
                                userRole === UserRole.ACCOUNT_OWNER || 
                                userRole === UserRole.MANAGER,
    
    // File upload permissions
    canUploadFiles: userRole === UserRole.PLATFORM_ADMIN || 
                    userRole === UserRole.ACCOUNT_OWNER || 
                    userRole === UserRole.MANAGER,
  }
}

export function canAccessRoute(role: string | undefined, requiredRoles: UserRole[]): boolean {
  if (!role) return false
  return requiredRoles.includes(role as UserRole)
}


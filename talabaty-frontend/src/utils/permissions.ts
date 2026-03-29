export enum UserRole {
    PLATFORM_ADMIN = 'PLATFORM_ADMIN',
    ACCOUNT_OWNER = 'ACCOUNT_OWNER',
    MANAGER = 'MANAGER',
    SUPPORT = 'SUPPORT'
}
export interface Permissions {
    canCreateStore: boolean;
    canUpdateStore: boolean;
    canDeleteStore: boolean;
    canManageUsers: boolean;
    canManageTeamMembers: boolean;
    canCreateOrder: boolean;
    canUpdateOrder: boolean;
    canManageShippingProviders: boolean;
    canUploadFiles: boolean;
    canAccessIntegrations: boolean;
    canManagePaymentRequests: boolean;
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
            canAccessIntegrations: false,
            canManagePaymentRequests: false,
        };
    }
    const userRole = role as UserRole;
    return {
        canCreateStore: userRole === UserRole.PLATFORM_ADMIN || userRole === UserRole.ACCOUNT_OWNER,
        canUpdateStore: userRole === UserRole.PLATFORM_ADMIN || userRole === UserRole.ACCOUNT_OWNER || userRole === UserRole.MANAGER,
        canDeleteStore: userRole === UserRole.PLATFORM_ADMIN || userRole === UserRole.ACCOUNT_OWNER,
        canManageUsers: userRole === UserRole.PLATFORM_ADMIN || userRole === UserRole.ACCOUNT_OWNER,
        canManageTeamMembers: userRole === UserRole.PLATFORM_ADMIN || userRole === UserRole.ACCOUNT_OWNER || userRole === UserRole.MANAGER,
        canCreateOrder: true,
        canUpdateOrder: userRole === UserRole.PLATFORM_ADMIN ||
            userRole === UserRole.ACCOUNT_OWNER ||
            userRole === UserRole.MANAGER ||
            userRole === UserRole.SUPPORT,
        canManageShippingProviders: userRole === UserRole.PLATFORM_ADMIN ||
            userRole === UserRole.ACCOUNT_OWNER ||
            userRole === UserRole.MANAGER,
        canUploadFiles: userRole === UserRole.PLATFORM_ADMIN ||
            userRole === UserRole.ACCOUNT_OWNER ||
            userRole === UserRole.MANAGER,
        canAccessIntegrations: userRole === UserRole.PLATFORM_ADMIN ||
            userRole === UserRole.ACCOUNT_OWNER ||
            userRole === UserRole.MANAGER,
        canManagePaymentRequests: userRole === UserRole.PLATFORM_ADMIN || userRole === UserRole.ACCOUNT_OWNER,
    };
}
export function canAccessRoute(role: string | undefined, requiredRoles: UserRole[]): boolean {
    if (!role)
        return false;
    return requiredRoles.includes(role as UserRole);
}

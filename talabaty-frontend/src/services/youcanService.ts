import api from './api';
export interface YouCanStore {
    id: string;
    youcanStoreId: string;
    youcanStoreName: string;
    youcanStoreDomain: string;
    storeId: string;
    storeName: string;
    active: boolean;
    lastSyncAt: string | null;
    createdAt: string;
}
export interface ConnectYouCanResponse {
    authorizationUrl: string;
    message: string;
}
export interface SyncOrdersResponse {
    success: boolean;
    message: string;
    syncedCount?: number;
    error?: string;
}
export const youcanService = {
    async connectStore(storeId: string): Promise<ConnectYouCanResponse> {
        const response = await api.get<ConnectYouCanResponse>(`/youcan/connect/${storeId}`);
        return response.data;
    },
    async getConnectedStores(): Promise<YouCanStore[]> {
        const response = await api.get<YouCanStore[]>('/youcan/stores');
        return response.data;
    },
    async syncOrders(youcanStoreId: string): Promise<SyncOrdersResponse> {
        const response = await api.post<SyncOrdersResponse>(`/youcan/stores/${youcanStoreId}/sync`);
        return response.data;
    },
    async disconnectStore(youcanStoreId: string): Promise<{
        success: boolean;
        message: string;
    }> {
        const response = await api.delete<{
            success: boolean;
            message: string;
        }>(`/youcan/stores/${youcanStoreId}`);
        return response.data;
    },
};

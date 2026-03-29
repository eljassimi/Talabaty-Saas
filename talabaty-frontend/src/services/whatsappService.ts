import api from './api';
export interface WhatsAppLinkStatus {
    configured: boolean;
    provider?: 'twilio' | 'bridge';
    ready?: boolean;
    qr?: string;
    initializing?: boolean;
    error?: string;
    bridgeError?: string;
    reason?: string;
}
export const whatsappService = {
    async getLinkStatus(): Promise<WhatsAppLinkStatus> {
        const response = await api.get<WhatsAppLinkStatus>('/whatsapp/link-status');
        return response.data;
    },
    async getStoreLinkStatus(storeId: string): Promise<WhatsAppLinkStatus> {
        const response = await api.get<WhatsAppLinkStatus>(`/stores/${storeId}/whatsapp-link-status`);
        return response.data;
    },
};

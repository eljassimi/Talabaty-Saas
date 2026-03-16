import api from './api'

export interface WhatsAppLinkStatus {
  configured: boolean
  provider?: 'twilio' | 'bridge'
  ready?: boolean
  qr?: string
  /** True while the bridge is starting (browser + WhatsApp Web loading); QR may take up to a minute. */
  initializing?: boolean
  /** Connection error (e.g. bridge not reachable). */
  error?: string
  /** Error from the bridge itself (e.g. init failed). */
  bridgeError?: string
  /** e.g. "logged_out" when user logged out on phone – show message and new QR */
  reason?: string
}

export const whatsappService = {
  /** Global link status (legacy, account-wide). */
  async getLinkStatus(): Promise<WhatsAppLinkStatus> {
    const response = await api.get<WhatsAppLinkStatus>('/whatsapp/link-status')
    return response.data
  },

  /** Per-store link status so each store can have its own WhatsApp session. */
  async getStoreLinkStatus(storeId: string): Promise<WhatsAppLinkStatus> {
    const response = await api.get<WhatsAppLinkStatus>(`/stores/${storeId}/whatsapp-link-status`)
    return response.data
  },
}

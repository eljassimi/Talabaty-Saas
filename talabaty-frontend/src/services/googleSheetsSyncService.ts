import api from './api'

export interface GoogleSheetsSyncConfig {
  id: string
  storeId: string
  spreadsheetId: string
  sheetName: string
  syncEnabled: boolean
  syncIntervalSeconds: number
  columnMapping?: string
  lastSyncAt?: string
  lastSyncStatus?: string
  lastSyncError?: string
  createdAt: string
  updatedAt: string
}

export interface CreateSyncConfigRequest {
  storeId: string
  spreadsheetId: string
  sheetName?: string
  credentialsJson?: string
  accessToken?: string
  refreshToken?: string
  syncEnabled?: boolean
  syncIntervalSeconds?: number
  columnMapping?: string
}

export interface UpdateSyncConfigRequest {
  spreadsheetId?: string
  sheetName?: string
  credentialsJson?: string
  accessToken?: string
  refreshToken?: string
  syncEnabled?: boolean
  syncIntervalSeconds?: number
  columnMapping?: string
}

export interface SyncResult {
  success: boolean
  message: string
  created: number
  updated: number
  errors: number
}

export const googleSheetsSyncService = {
  async getConfigsByStore(storeId: string): Promise<GoogleSheetsSyncConfig[]> {
    const response = await api.get(`/excel-sync/store/${storeId}`)
    return response.data
  },

  async createConfig(request: CreateSyncConfigRequest): Promise<GoogleSheetsSyncConfig> {
    const response = await api.post('/excel-sync', request)
    return response.data
  },

  async updateConfig(configId: string, request: UpdateSyncConfigRequest): Promise<GoogleSheetsSyncConfig> {
    const response = await api.put(`/excel-sync/${configId}`, request)
    return response.data
  },

  async deleteConfig(configId: string): Promise<void> {
    await api.delete(`/excel-sync/${configId}`)
  },

  async triggerSync(configId: string): Promise<SyncResult> {
    const response = await api.post(`/excel-sync/${configId}/sync`)
    return response.data
  },

  async syncAll(): Promise<string> {
    const response = await api.post('/excel-sync/sync-all')
    return response.data
  }
}


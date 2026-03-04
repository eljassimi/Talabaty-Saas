import api from './api'

export interface Store {
  id: string
  name: string
  code: string
  timezone: string
  managerId: string
  logoUrl?: string
  color?: string
  createdAt: string
  updatedAt: string
}

export interface CreateStoreRequest {
  name: string
  managerId?: string
  logoUrl?: string
  color?: string
}

export interface UpdateStoreRequest {
  name?: string
  timezone?: string
}

export interface ShippingProvider {
  id: string
  providerType: string
  customerId: string
  displayName: string
  active: boolean
}

export interface CreateShippingProviderRequest {
  customerId: string
  apiKey: string
  displayName: string
  providerType?: string
}

export const storeService = {
  async getStores(): Promise<Store[]> {
    const response = await api.get<Store[]>('/stores')
    return response.data
  },

  async getStore(id: string): Promise<Store> {
    const response = await api.get<Store>(`/stores/${id}`)
    return response.data
  },

  async createStore(data: CreateStoreRequest): Promise<Store> {
    const response = await api.post<Store>('/stores', data)
    return response.data
  },

  async updateStore(id: string, data: UpdateStoreRequest): Promise<Store> {
    const response = await api.put<Store>(`/stores/${id}`, data)
    return response.data
  },

  async deleteStore(id: string): Promise<void> {
    await api.delete(`/stores/${id}`)
  },

  async getShippingProviders(storeId: string): Promise<ShippingProvider[]> {
    const response = await api.get<ShippingProvider[]>(`/stores/${storeId}/shipping-providers`)
    return response.data
  },

  async createShippingProvider(
    storeId: string,
    data: CreateShippingProviderRequest
  ): Promise<ShippingProvider> {
    const response = await api.post<ShippingProvider>(
      `/stores/${storeId}/shipping-providers`,
      data
    )
    return response.data
  },
}


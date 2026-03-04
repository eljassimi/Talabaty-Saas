import api from './api'

export type OrderStatus = 'ENCOURS' | 'CONFIRMED' | 'CONCLED' | 'APPEL_1' | 'APPEL_2'
export type OrderSource = 'API' | 'MANUAL' | 'IMPORT'

export interface Order {
  id: string
  storeId: string
  customerName: string
  customerPhone: string
  destinationAddress: string
  totalAmount: number
  currency: string
  status: OrderStatus
  source: OrderSource
  externalOrderId?: string
  ozonTrackingNumber?: string
  productName?: string
  productId?: string
  city?: string
  metadata?: string
  assignedToUserId?: string
  assignedToName?: string
  createdAt: string
  updatedAt: string
}

export interface CreateOrderRequest {
  storeId: string
  customerName: string
  customerPhone: string
  destinationAddress: string
  totalAmount: number
  currency?: string
  externalOrderId?: string
  source?: OrderSource
  productName?: string
  productId?: string
}

export interface UpdateStatusRequest {
  status: OrderStatus
  note?: string
  changedByUserId?: string
}

export interface UpdateOrderRequest {
  customerName?: string
  customerPhone?: string
  destinationAddress?: string
  city?: string
  totalAmount?: number
  currency?: string
  metadata?: string
  productName?: string
  productId?: string
}

export interface SendToShippingRequest {
  cityId: string
  note?: string
  nature?: string
  trackingNumber?: string
  products?: string
  stock?: number
  open?: number
  fragile?: number
  replace?: number
}

export interface OrderStatusHistory {
  id: string
  orderId: string
  status: OrderStatus
  note?: string
  changedBy?: {
    id: string
    firstName: string
    lastName: string
  }
  createdAt: string
}

export const orderService = {
  async getOrdersByStore(storeId: string): Promise<Order[]> {
    const response = await api.get<Order[]>(`/orders/store/${storeId}`)
    return response.data
  },

  async getOrdersByStoreAndStatus(storeId: string, status: OrderStatus): Promise<Order[]> {
    const response = await api.get<Order[]>(`/orders/store/${storeId}/status/${status}`)
    return response.data
  },

  async getOrder(id: string): Promise<Order> {
    const response = await api.get<Order>(`/orders/${id}`)
    return response.data
  },

  async createOrder(data: CreateOrderRequest): Promise<Order> {
    const response = await api.post<Order>('/orders', data)
    return response.data
  },

  async updateOrderStatus(id: string, data: UpdateStatusRequest): Promise<Order> {
    const response = await api.put<Order>(`/orders/${id}/status`, data)
    return response.data
  },

  async updateOrder(id: string, data: UpdateOrderRequest): Promise<Order> {
    const response = await api.put<Order>(`/orders/${id}`, data)
    return response.data
  },

  async getOrderHistory(id: string): Promise<OrderStatusHistory[]> {
    const response = await api.get<OrderStatusHistory[]>(`/orders/${id}/history`)
    return response.data
  },

  async sendOrderToShipping(id: string, data: SendToShippingRequest): Promise<any> {
    const response = await api.post(`/orders/${id}/send-to-shipping`, data)
    return response.data
  },

  async sendOrdersToShipping(orderIds: string[], data: SendToShippingRequest): Promise<any> {
    const response = await api.post('/orders/batch/send-to-shipping', {
      orderIds,
      ...data
    })
    return response.data
  },
}


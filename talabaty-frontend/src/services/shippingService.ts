import api from './api'

export interface City {
  id: number
  ref: string
  name: string
  'DELIVERED-PRICE': number
  'RETURNED-PRICE': number
  'REFUSED-PRICE': number
}

interface RawCity {
  ID: number
  REF: string
  NAME: string
  'DELIVERED-PRICE': number
  'RETURNED-PRICE': number
  'REFUSED-PRICE': number
}

export interface CitiesResponse {
  CITIES: Record<string, RawCity>
  DEBUG: {
    timestamp: string
    source: string
    total_cities: number
    request_id: string
  }
}

export interface TrackingResponse {
  [key: string]: any // Ozon Express tracking response can have various fields
  STATUS?: string
  'TRACKING-NUMBER'?: string
  RESULT?: string
  MESSAGE?: string
  TRACKING?: {
    'TRACKING-NUMBER'?: string
    STATUS?: string
    RESULT?: string
    MESSAGE?: string
    LAST_TRACKING?: {
      STATUT?: string
      TIME_STR?: string
      TIME?: string
      COMMENT?: string
    }
    HISTORY?: {
      [key: string]: {
        STATUT?: string
        TIME_STR?: string
        TIME?: string
        COMMENT?: string
      }
    }
    [key: string]: any
  }
  CHECK_API?: {
    MESSAGE?: string
    RESULT?: string
    [key: string]: any
  }
}

export const shippingService = {
  async getCities(): Promise<City[]> {
    const response = await api.get<CitiesResponse>('/shipping/ozon-express/cities')
    const cities = response.data.CITIES
    return Object.values(cities).map((city) => ({
      id: city.ID,
      ref: city.REF,
      name: city.NAME,
      'DELIVERED-PRICE': city['DELIVERED-PRICE'],
      'RETURNED-PRICE': city['RETURNED-PRICE'],
      'REFUSED-PRICE': city['REFUSED-PRICE'],
    }))
  },

  async trackParcel(trackingNumber: string): Promise<TrackingResponse> {
    try {
      const response = await api.post<TrackingResponse>('/shipping/ozon-express/parcels/track', {
        trackingNumber,
      })
      return response.data
    } catch (error: any) {
      // Extract error message from response if available
      const errorMessage = error.response?.data?.error || error.message || 'Unknown error'
      console.error('Error tracking parcel:', errorMessage, 'Tracking number:', trackingNumber)
      throw new Error(errorMessage)
    }
  },

  async trackMultipleParcels(trackingNumbers: string[]): Promise<TrackingResponse> {
    const response = await api.post<TrackingResponse>('/shipping/ozon-express/parcels/track', {
      trackingNumbers,
    })
    return response.data
  },

  /** Create a Bon de Livraison (delivery note) from selected orders with Ozon tracking numbers. */
  async createBonDeLivraison(orderIds: string[]): Promise<BonDeLivraisonResult> {
    const response = await api.post<BonDeLivraisonResult>(
      '/shipping/ozon-express/delivery-notes/create-full',
      { orderIds }
    )
    return response.data
  },
}

export interface BonDeLivraisonResult {
  ref: string
  pdfUrl: string
  pdfTicketsUrl: string
  pdfTickets4x4Url: string
  trackingCount: number
  /** True when the BL already existed (orders were already in a BL). */
  existing?: boolean
}


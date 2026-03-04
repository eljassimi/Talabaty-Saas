import api from './api'

export interface SignupRequest {
  email: string
  password: string
  firstName: string
  lastName: string
  phoneNumber: string
  accountName: string
  accountType: 'INDIVIDUAL' | 'BUSINESS'
}

export interface LoginRequest {
  email: string
  password: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: User
}

export interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  phoneNumber?: string
  role: string
  status: string
  mustChangePassword?: boolean
  selectedStoreId?: string | null
}

export const authService = {
  async signup(data: SignupRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/signup', data)
    return response.data
  },

  async login(data: LoginRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/login', data)
    return response.data
  },

  async refreshToken(refreshToken: string): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/refresh', refreshToken)
    return response.data
  },
}


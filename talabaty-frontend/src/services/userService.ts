import api from './api'

export interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  phoneNumber?: string
  role: 'PLATFORM_ADMIN' | 'ACCOUNT_OWNER' | 'MANAGER' | 'SUPPORT'
  status: 'INVITED' | 'ACTIVE' | 'DISABLED' | 'BANNED'
  lastLoginAt?: string
  createdAt: string
  updatedAt: string
}

export interface CreateUserRequest {
  email: string
  password?: string
  firstName: string
  lastName: string
  role: 'MANAGER' | 'SUPPORT'
}

export interface CreateUserResponse {
  user: User
  generatedPassword?: string
}

export const userService = {
  async getUsers(): Promise<User[]> {
    const response = await api.get<User[]>('/users')
    return response.data
  },

  async getBannedUsers(): Promise<User[]> {
    const response = await api.get<User[]>('/users/banned')
    return response.data
  },

  async createUser(data: CreateUserRequest): Promise<User | CreateUserResponse> {
    const response = await api.post<User | CreateUserResponse>('/users', data)
    return response.data
  },

  async banUser(userId: string): Promise<User> {
    const response = await api.put<User>(`/users/${userId}/ban`)
    return response.data
  },

  async unbanUser(userId: string): Promise<User> {
    const response = await api.put<User>(`/users/${userId}/unban`)
    return response.data
  },

  async updateUserStatus(userId: string, status: User['status']): Promise<User> {
    const response = await api.put<User>(`/users/${userId}/status`, { status })
    return response.data
  },

  async changePassword(currentPassword: string | null, newPassword: string): Promise<User> {
    const requestBody: { newPassword: string; currentPassword?: string | null } = {
      newPassword,
    }
    // Only include currentPassword if it's provided (not null/empty)
    if (currentPassword != null && currentPassword.trim() !== '') {
      requestBody.currentPassword = currentPassword
    }
    const response = await api.post<User>('/auth/change-password', requestBody)
    return response.data
  },

  async updateSelectedStore(storeId: string | null): Promise<User> {
    const response = await api.put<User>('/users/selected-store', { storeId })
    return response.data
  },
}


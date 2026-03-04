import api from './api'

export interface TeamMember {
  id: string
  storeId: string
  userId?: string
  email?: string
  firstName?: string
  lastName?: string
  externalMemberEmail?: string
  role: 'MANAGER' | 'SUPPORT' | 'EXTERNAL_SUPPORT'
  invitationStatus: 'PENDING' | 'ACCEPTED' | 'REJECTED'
  addedBy?: string
  createdAt: string
  updatedAt: string
}

export interface TeamMemberRequest {
  email: string
  firstName: string
  lastName: string
  password?: string
}

export interface BulkCreateTeamRequest {
  managers: TeamMemberRequest[]
  supports: TeamMemberRequest[]
}

export interface BulkCreateTeamResponse {
  createdManagers: TeamMember[]
  createdSupports: TeamMember[]
  totalCreated: number
}

export const teamService = {
  async getTeamMembers(storeId: string): Promise<TeamMember[]> {
    const response = await api.get<TeamMember[]>(`/stores/${storeId}/team`)
    return response.data
  },

  async bulkCreateTeamMembers(
    storeId: string,
    data: BulkCreateTeamRequest
  ): Promise<BulkCreateTeamResponse> {
    const response = await api.post<BulkCreateTeamResponse>(
      `/stores/${storeId}/team/bulk-create`,
      data
    )
    return response.data
  },

  async inviteUser(
    storeId: string,
    userId: string,
    role: TeamMember['role']
  ): Promise<TeamMember> {
    const response = await api.post<TeamMember>(`/stores/${storeId}/team/invite-user`, {
      userId,
      role,
    })
    return response.data
  },

  async inviteExternalMember(
    storeId: string,
    email: string,
    role: TeamMember['role']
  ): Promise<TeamMember> {
    const response = await api.post<TeamMember>(`/stores/${storeId}/team/invite-external`, {
      email,
      role,
    })
    return response.data
  },

  async removeMember(storeId: string, memberId: string): Promise<void> {
    await api.delete(`/stores/${storeId}/team/${memberId}`)
  },

  async updateMemberRole(
    storeId: string,
    memberId: string,
    role: TeamMember['role']
  ): Promise<TeamMember> {
    const response = await api.put<TeamMember>(`/stores/${storeId}/team/${memberId}/role`, {
      role,
    })
    return response.data
  },

  async createTeamMember(
    storeId: string,
    data: {
      email: string
      password?: string
      firstName: string
      lastName: string
      role: 'MANAGER' | 'SUPPORT'
    }
  ): Promise<{ member: TeamMember; userWasCreated: boolean; generatedPassword?: string }> {
    const response = await api.post<{ member: TeamMember; userWasCreated: boolean; generatedPassword?: string }>(
      `/stores/${storeId}/team/create-member`,
      data
    )
    return response.data
  },
}


import { useEffect, useState } from 'react'
import { teamService, TeamMember } from '../../services/teamService'
import { useAuth } from '../../contexts/AuthContext'
import { useStoreColor } from '../../hooks/useStoreColor'
import { UserPlus, Mail, Trash2 } from 'lucide-react'
import CreateTeamMemberModal from '../CreateTeamMemberModal'

export default function TeamSettings() {
  const { user } = useAuth()
  const { storeColor } = useStoreColor()
  const [members, setMembers] = useState<TeamMember[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [emailInput, setEmailInput] = useState('')
  const [emails, setEmails] = useState<string[]>([])
  const [removingMemberId, setRemovingMemberId] = useState<string | null>(null)

  useEffect(() => {
    if (user?.selectedStoreId) {
      loadMembers()
    }
  }, [user?.selectedStoreId])

  const loadMembers = async () => {
    if (!user?.selectedStoreId) return
    try {
      const data = await teamService.getTeamMembers(user.selectedStoreId)
      setMembers(Array.isArray(data) ? data : [])
    } catch (error) {
      console.error('Error loading team members:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleAddEmail = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && emailInput.trim()) {
      e.preventDefault()
      if (!emails.includes(emailInput.trim())) {
        setEmails([...emails, emailInput.trim()])
        setEmailInput('')
      }
    }
  }

  const handleRemoveEmail = (email: string) => {
    setEmails(emails.filter((e) => e !== email))
  }

  const handleRemoveMember = async (memberId: string) => {
    if (!user?.selectedStoreId) return
    if (!confirm('Are you sure you want to remove this team member?')) return

    setRemovingMemberId(memberId)
    try {
      await teamService.removeMember(user.selectedStoreId, memberId)
      await loadMembers()
    } catch (error) {
      console.error('Error removing team member:', error)
      alert('Failed to remove team member')
    } finally {
      setRemovingMemberId(null)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div 
          className="animate-spin rounded-full h-12 w-12 border-2 border-transparent"
          style={{
            borderTopColor: '#123133',
            borderRightColor: '#FF6E00',
          }}
        ></div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Add Recipients Section */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Add Recipients</h2>
        <div className="relative">
          <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            type="email"
            placeholder="Enter email address, press enter to add.."
            value={emailInput}
            onChange={(e) => setEmailInput(e.target.value)}
            onKeyDown={handleAddEmail}
            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:border-transparent transition-all"
            style={{
              '--tw-ring-color': '#123133',
            } as React.CSSProperties & { '--tw-ring-color': string }}
            onFocus={(e) => {
              e.currentTarget.style.borderColor = '#123133'
              e.currentTarget.style.boxShadow = '0 0 0 2px rgba(18, 49, 51, 0.25)'
            }}
            onBlur={(e) => {
              e.currentTarget.style.borderColor = ''
              e.currentTarget.style.boxShadow = ''
            }}
          />
        </div>
        {emails.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-2">
            {emails.map((email) => (
              <span
                key={email}
                className="inline-flex items-center px-3 py-1 rounded-full text-sm bg-blue-100 text-blue-800"
              >
                {email}
                <button
                  onClick={() => handleRemoveEmail(email)}
                  className="ml-2 transition-colors"
                  style={{ color: '#123133' }}
                  onMouseEnter={(e) => e.currentTarget.style.color = '#FF6E00'}
                  onMouseLeave={(e) => e.currentTarget.style.color = '#123133'}
                >
                  ×
                </button>
              </span>
            ))}
          </div>
        )}
      </div>

      {/* Members Section */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-900">Members</h2>
          <button
            onClick={() => setShowCreateModal(true)}
            className="flex items-center px-4 py-2 text-white rounded-lg hover:opacity-90 transition-all shadow-sm"
            style={{ backgroundColor: storeColor }}
          >
            <UserPlus className="h-4 w-4 mr-2" />
            Add Member
          </button>
        </div>

        {members.length === 0 ? (
          <div className="text-center py-12">
            <div className="mx-auto w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
              <UserPlus className="h-8 w-8 text-gray-400" />
            </div>
            <h3 className="text-sm font-medium text-gray-900">No team members</h3>
            <p className="mt-1 text-sm text-gray-500">Create a team with managers and support members.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {members.map((member) => {
              const displayName = member.firstName && member.lastName
                ? `${member.firstName} ${member.lastName}`
                : member.email || member.externalMemberEmail || member.userId || 'Team Member'
              const displayEmail = member.email || member.externalMemberEmail || 'N/A'
              const initials = member.firstName && member.lastName
                ? `${member.firstName[0]}${member.lastName[0]}`.toUpperCase()
                : displayName.substring(0, 2).toUpperCase()

              return (
                <div key={member.id} className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors">
                  <div className="flex items-center space-x-3 flex-1">
                    <div className="h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
                      <span className="font-semibold text-sm" style={{ color: '#123133' }}>
                        {initials}
                      </span>
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 truncate">
                        {displayName}
                      </p>
                      <p className="text-sm text-gray-500 truncate">{displayEmail}</p>
                    </div>
                    <div className="flex items-center space-x-2">
                      <span className="px-2 py-1 text-xs font-medium rounded-md bg-gray-100 text-gray-700">
                        {member.role === 'MANAGER' ? 'Manager' : member.role === 'SUPPORT' ? 'Support' : member.role}
                      </span>
                      <button
                        onClick={() => handleRemoveMember(member.id)}
                        disabled={removingMemberId === member.id}
                        className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                        title="Remove team member"
                      >
                        {removingMemberId === member.id ? (
                          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-red-600"></div>
                        ) : (
                          <Trash2 className="h-4 w-4" />
                        )}
                      </button>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        )}

        {/* Info Message */}
        <div className="mt-6 flex items-start space-x-3 p-4 bg-orange-50 rounded-lg">
          <div className="h-5 w-5 rounded-full bg-orange-500 flex items-center justify-center flex-shrink-0 mt-0.5">
            <span className="text-white text-xs font-bold">!</span>
          </div>
          <p className="text-sm text-gray-700">
            Adding members will help you separate the roles of each one, and facilitate the management of your business.
          </p>
        </div>
      </div>

      {showCreateModal && user?.selectedStoreId && (
        <CreateTeamMemberModal
          storeId={user.selectedStoreId}
          onClose={() => {
            setShowCreateModal(false)
            loadMembers()
          }}
          onSuccess={() => {
            setShowCreateModal(false)
            loadMembers()
          }}
        />
      )}
    </div>
  )
}


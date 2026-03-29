import { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { userService, User } from '../services/userService';
import { storeService } from '../services/storeService';
import { teamService, type TeamMember } from '../services/teamService';
import { Users as UsersIcon, Ban, UserCheck, Plus, Search, Mail, MoreVertical, ChevronDown } from 'lucide-react';
import CreateTeamMemberModal from '../components/CreateTeamMemberModal';
import { useStoreColor } from '../hooks/useStoreColor';
function hexToRgb(hex: string) {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
    } : { r: 2, g: 132, b: 199 };
}
function getInitials(user: User): string {
    return `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase() || 'U';
}
export default function Users() {
    const { user: currentUser } = useAuth();
    const { storeColor } = useStoreColor();
    const [users, setUsers] = useState<User[]>([]);
    const [teamMembers, setTeamMembers] = useState<TeamMember[]>([]);
    const [bannedUsers, setBannedUsers] = useState<User[]>([]);
    const [stores, setStores] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState<'all' | 'banned'>('all');
    const [showTeamMemberModal, setShowTeamMemberModal] = useState(false);
    const [selectedStoreId, setSelectedStoreId] = useState<string | null>(currentUser?.selectedStoreId ?? null);
    const [searchQuery, setSearchQuery] = useState('');
    const [actionMenuOpen, setActionMenuOpen] = useState<string | null>(null);
    const isAdmin = currentUser?.role === 'ACCOUNT_OWNER' || currentUser?.role === 'PLATFORM_ADMIN';
    useEffect(() => {
        loadUsers();
        loadBannedUsers();
        if (isAdmin) {
            loadStores();
        }
    }, [isAdmin]);
    useEffect(() => {
        if (isAdmin && currentUser?.selectedStoreId) {
            setSelectedStoreId(currentUser.selectedStoreId);
        }
    }, [isAdmin, currentUser?.selectedStoreId]);
    useEffect(() => {
        if (isAdmin && selectedStoreId) {
            loadTeamMembers(selectedStoreId);
        }
        else {
            setTeamMembers([]);
        }
    }, [isAdmin, selectedStoreId]);
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (actionMenuOpen && !(event.target as HTMLElement).closest('.action-menu-container')) {
                setActionMenuOpen(null);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [actionMenuOpen]);
    const loadUsers = async () => {
        try {
            const data = await userService.getUsers();
            const filteredData = data.filter(user => user.role !== 'ACCOUNT_OWNER');
            setUsers(filteredData);
        }
        catch (error) {
            console.error('Error loading users:', error);
        }
        finally {
            setLoading(false);
        }
    };
    const loadBannedUsers = async () => {
        try {
            const data = await userService.getBannedUsers();
            setBannedUsers(Array.isArray(data) ? data : []);
        }
        catch (error) {
            console.error('Error loading banned users:', error);
            setBannedUsers([]);
        }
    };
    const loadStores = async () => {
        try {
            const data = await storeService.getStores();
            setStores(data);
            if (data.length > 0) {
                const preferredId = currentUser?.selectedStoreId;
                if (preferredId && data.some((s) => s.id === preferredId)) {
                    setSelectedStoreId(preferredId);
                }
                else if (!selectedStoreId) {
                    setSelectedStoreId(data[0].id);
                }
            }
        }
        catch (error) {
            console.error('Error loading stores:', error);
        }
    };
    const loadTeamMembers = async (storeId: string) => {
        try {
            const data = await teamService.getTeamMembers(storeId);
            setTeamMembers(Array.isArray(data) ? data : []);
        }
        catch (error) {
            console.error('Error loading team members:', error);
            setTeamMembers([]);
        }
    };
    const handleBan = async (userId: string) => {
        if (!confirm('Are you sure you want to ban this user?'))
            return;
        try {
            await userService.banUser(userId);
            loadUsers();
            loadBannedUsers();
            setActionMenuOpen(null);
        }
        catch (error) {
            console.error('Error banning user:', error);
            alert('Failed to ban user');
        }
    };
    const handleUnban = async (userId: string) => {
        if (!confirm('Are you sure you want to unban this user?'))
            return;
        try {
            await userService.unbanUser(userId);
            loadUsers();
            loadBannedUsers();
            setActionMenuOpen(null);
        }
        catch (error) {
            console.error('Error unbanning user:', error);
            alert('Failed to unban user');
        }
    };
    const getRoleBadgeColor = (role: User['role']) => {
        switch (role) {
            case 'PLATFORM_ADMIN':
                return 'bg-purple-100 text-purple-800 border-purple-200';
            case 'ACCOUNT_OWNER':
                return 'bg-blue-100 text-blue-800 border-blue-200';
            case 'MANAGER':
                return 'bg-green-100 text-green-800 border-green-200';
            case 'SUPPORT':
                return 'bg-yellow-100 text-yellow-800 border-yellow-200';
            default:
                return 'bg-gray-100 text-gray-800 border-gray-200';
        }
    };
    const getStatusBadgeColor = (status: User['status']) => {
        switch (status) {
            case 'ACTIVE':
                return 'bg-green-100 text-green-800';
            case 'BANNED':
                return 'bg-red-100 text-red-800';
            case 'DISABLED':
                return 'bg-gray-100 text-gray-800';
            case 'INVITED':
                return 'bg-yellow-100 text-yellow-800';
            default:
                return 'bg-gray-100 text-gray-800';
        }
    };
    const getRoleLabel = (role: User['role']) => {
        return role.replace('_', ' ');
    };
    const displayUsers: User[] = activeTab === 'banned'
        ? bannedUsers
        : teamMembers.map((member) => {
            const base = member.userId ? users.find((u) => u.id === member.userId) : undefined;
            return {
                id: member.userId || member.id,
                email: member.email || member.externalMemberEmail || base?.email || '',
                firstName: member.firstName || base?.firstName || '',
                lastName: member.lastName || base?.lastName || '',
                phoneNumber: base?.phoneNumber,
                role: (base?.role as User['role']) || (member.role === 'MANAGER' ? 'MANAGER' : 'SUPPORT'),
                status: base?.status || 'ACTIVE',
                lastLoginAt: base?.lastLoginAt,
                createdAt: base?.createdAt || member.createdAt,
                updatedAt: base?.updatedAt || member.updatedAt,
            };
        });
    const filteredUsers = displayUsers.filter((user) => {
        const query = searchQuery.toLowerCase();
        return (user.firstName?.toLowerCase().includes(query) ||
            user.lastName?.toLowerCase().includes(query) ||
            user.email?.toLowerCase().includes(query) ||
            user.role?.toLowerCase().includes(query));
    });
    if (loading) {
        return (<div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>);
    }
    return (<div className="space-y-6">
      
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">People</h1>
          <p className="mt-2 text-gray-600">Manage your team members and banned users</p>
        </div>
        {isAdmin && stores.length > 0 && (<div className="flex items-center space-x-3">
            <div className="relative">
              <select value={selectedStoreId || ''} onChange={(e) => setSelectedStoreId(e.target.value)} className="appearance-none bg-white border border-gray-300 rounded-lg px-4 py-2.5 pr-8 text-sm transition-all" style={{
                '--tw-ring-color': storeColor,
            } as React.CSSProperties & {
                '--tw-ring-color': string;
            }} onFocus={(e) => {
                e.currentTarget.style.borderColor = storeColor;
                e.currentTarget.style.boxShadow = `0 0 0 2px ${storeColor}40`;
            }} onBlur={(e) => {
                e.currentTarget.style.borderColor = '';
                e.currentTarget.style.boxShadow = '';
            }}>
                {stores.map((store) => (<option key={store.id} value={store.id}>
                    {store.name}
                  </option>))}
              </select>
              <ChevronDown className="absolute right-2 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none"/>
            </div>
            <button onClick={() => setShowTeamMemberModal(true)} disabled={!selectedStoreId} className="inline-flex items-center px-4 py-2.5 border border-transparent shadow-sm text-sm font-medium rounded-lg text-white transition-all disabled:opacity-50 disabled:cursor-not-allowed" style={{ backgroundColor: storeColor }} onMouseEnter={(e) => {
                if (!selectedStoreId)
                    return;
                const rgb = hexToRgb(storeColor);
                e.currentTarget.style.backgroundColor = `rgb(${Math.max(0, rgb.r - 20)}, ${Math.max(0, rgb.g - 20)}, ${Math.max(0, rgb.b - 20)})`;
            }} onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = storeColor;
            }}>
              <Plus className="h-5 w-5 mr-2"/>
              Add Team Member
            </button>
          </div>)}
      </div>

      
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-1 flex space-x-1">
        <button onClick={() => setActiveTab('all')} className={`px-4 py-2 text-sm font-medium rounded-lg transition-all ${activeTab === 'all' ? 'text-white' : 'text-gray-700 hover:bg-gray-50'}`} style={activeTab === 'all' ? { backgroundColor: storeColor } : {}}>
          <div className="flex items-center">
            <UsersIcon className="h-4 w-4 mr-2"/>
            All ({displayUsers.length})
          </div>
        </button>
        <button onClick={() => setActiveTab('banned')} className={`px-4 py-2 text-sm font-medium rounded-lg transition-all ${activeTab === 'banned' ? 'text-white' : 'text-gray-700 hover:bg-gray-50'}`} style={activeTab === 'banned' ? { backgroundColor: storeColor } : {}}>
          <div className="flex items-center">
            <Ban className="h-4 w-4 mr-2"/>
            Banned ({bannedUsers.length})
          </div>
        </button>
      </div>

      
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400"/>
          <input type="text" placeholder="Search by name, email, or role..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 transition-all" style={{
            '--tw-ring-color': storeColor,
        } as React.CSSProperties & {
            '--tw-ring-color': string;
        }} onFocus={(e) => {
            e.currentTarget.style.borderColor = storeColor;
            e.currentTarget.style.boxShadow = `0 0 0 2px ${storeColor}40`;
        }} onBlur={(e) => {
            e.currentTarget.style.borderColor = '';
            e.currentTarget.style.boxShadow = '';
        }}/>
        </div>
      </div>

      
      {filteredUsers.length === 0 ? (<div className="bg-white rounded-xl shadow-sm border border-gray-100 px-6 py-12 text-center">
          {activeTab === 'banned' ? (<>
              <div className="mx-auto w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
                <Ban className="h-8 w-8 text-gray-400"/>
              </div>
              <h3 className="text-sm font-medium text-gray-900">No banned users</h3>
              <p className="mt-1 text-sm text-gray-500">There are no banned users in your account.</p>
            </>) : (<>
              <div className="mx-auto w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
                <UsersIcon className="h-8 w-8 text-gray-400"/>
              </div>
              <h3 className="text-sm font-medium text-gray-900">No users found</h3>
              <p className="mt-1 text-sm text-gray-500">
                {searchQuery ? 'Try adjusting your search criteria.' : 'No users found in your account.'}
              </p>
            </>)}
        </div>) : (<div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    User
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Email
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Role
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Last Login
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Created At
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Action
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filteredUsers.map((user) => {
                const initials = getInitials(user);
                return (<tr key={user.id} className="hover:bg-gray-50 transition-colors">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center">
                          <div className="h-10 w-10 rounded-full flex items-center justify-center text-white text-sm font-medium mr-3 shadow-md" style={{ backgroundColor: storeColor || '#0284c7' }}>
                            {initials}
                          </div>
                          <div>
                            <div className="text-sm font-medium text-gray-900">
                              {user.firstName} {user.lastName}
                            </div>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center text-sm text-gray-900">
                          <Mail className="h-4 w-4 mr-2 text-gray-400"/>
                          {user.email}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex px-3 py-1 text-xs font-semibold rounded-full border ${getRoleBadgeColor(user.role)}`}>
                          {getRoleLabel(user.role)}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex px-3 py-1 text-xs font-semibold rounded-full ${getStatusBadgeColor(user.status)}`}>
                          {user.status}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleDateString() : 'Never'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {new Date(user.createdAt).toLocaleDateString()}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium relative action-menu-container">
                        {isAdmin && (<>
                            <button onClick={(e) => {
                            e.stopPropagation();
                            setActionMenuOpen(actionMenuOpen === user.id ? null : user.id);
                        }} className="p-2 hover:bg-gray-100 rounded-full transition-colors">
                              <MoreVertical className="h-5 w-5 text-gray-400"/>
                            </button>
                            {actionMenuOpen === user.id && (<div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg z-10 border border-gray-200">
                                <div className="py-1">
                                  {user.status === 'BANNED' ? (<button onClick={(e) => {
                                    e.stopPropagation();
                                    handleUnban(user.id);
                                }} className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 flex items-center">
                                      <UserCheck className="h-4 w-4 mr-2 text-green-600"/>
                                      Unban User
                                    </button>) : (<button onClick={(e) => {
                                    e.stopPropagation();
                                    handleBan(user.id);
                                }} className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 flex items-center">
                                      <Ban className="h-4 w-4 mr-2 text-red-600"/>
                                      Ban User
                                    </button>)}
                                </div>
                              </div>)}
                          </>)}
                      </td>
                    </tr>);
            })}
              </tbody>
            </table>
          </div>
        </div>)}

      {showTeamMemberModal && selectedStoreId && (<CreateTeamMemberModal storeId={selectedStoreId} onClose={() => setShowTeamMemberModal(false)} onSuccess={() => {
                setShowTeamMemberModal(false);
                loadUsers();
            }}/>)}
    </div>);
}

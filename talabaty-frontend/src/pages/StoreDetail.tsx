import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { storeService, Store, ShippingProvider } from '../services/storeService'
import { teamService, TeamMember } from '../services/teamService'
import { googleSheetsSyncService, GoogleSheetsSyncConfig } from '../services/googleSheetsSyncService'
import { ArrowLeft, Settings, Truck, Users, Plus, Store as StoreIcon, FileSpreadsheet, CheckCircle, XCircle, Banknote } from 'lucide-react'
import ShippingProviderForm from '../components/ShippingProviderForm'
import CreateTeamMemberModal from '../components/CreateTeamMemberModal'
import GoogleSheetsSyncModal from '../components/GoogleSheetsSyncModal'
import YouCanIntegration from '../components/YouCanIntegration'
import { useStoreColor } from '../hooks/useStoreColor'
import { useAuth } from '../contexts/AuthContext'
import { getPermissions } from '../utils/permissions'

export default function StoreDetail() {
  const { id } = useParams<{ id: string }>()
  const { storeColor, hexToRgb } = useStoreColor()
  const [store, setStore] = useState<Store | null>(null)
  const [providers, setProviders] = useState<ShippingProvider[]>([])
  const [teamMembers, setTeamMembers] = useState<TeamMember[]>([])
  const [syncConfig, setSyncConfig] = useState<GoogleSheetsSyncConfig | null>(null)
  const [loading, setLoading] = useState(true)
  const [showProviderForm, setShowProviderForm] = useState(false)
  const [showTeamModal, setShowTeamModal] = useState(false)
  const [showSyncModal, setShowSyncModal] = useState(false)
  const [supportRevenueSettings, setSupportRevenueSettings] = useState<{ pricePerOrderConfirmedMad: number; pricePerOrderDeliveredMad: number } | null>(null)
  const [supportRevenueSaving, setSupportRevenueSaving] = useState(false)
  const { user } = useAuth()
  const canUpdateStore = getPermissions(user?.role).canUpdateStore

  useEffect(() => {
    if (id) {
      loadStore()
      loadProviders()
      loadTeamMembers()
      loadSyncConfig()
    }
  }, [id])

  const loadStore = async () => {
    if (!id) return
    try {
      const data = await storeService.getStore(id)
      setStore(data)
    } catch (error) {
      console.error('Error loading store:', error)
    } finally {
      setLoading(false)
    }
  }

  const loadProviders = async () => {
    if (!id) return
    try {
      const data = await storeService.getShippingProviders(id)
      console.log('Loaded providers:', data) // Debug log
      if (Array.isArray(data)) {
        setProviders(data)
      } else if (data && typeof data === 'object') {
        // If response is wrapped in an object, try to extract array
        const providersArray = (data as any).providers || (data as any).data || []
        setProviders(Array.isArray(providersArray) ? providersArray : [])
      } else {
        setProviders([])
      }
    } catch (error) {
      console.error('Error loading providers:', error)
      setProviders([])
    }
  }

  const loadTeamMembers = async () => {
    if (!id) return
    try {
      const data = await teamService.getTeamMembers(id)
      setTeamMembers(Array.isArray(data) ? data : [])
    } catch (error) {
      console.error('Error loading team members:', error)
      setTeamMembers([])
    }
  }

  const loadSyncConfig = async () => {
    if (!id) return
    try {
      const configs = await googleSheetsSyncService.getConfigsByStore(id)
      if (configs.length > 0) {
        setSyncConfig(configs[0]) // Take the first config if multiple exist
      } else {
        setSyncConfig(null)
      }
    } catch (error) {
      console.error('Error loading sync config:', error)
      setSyncConfig(null)
    }
  }

  const loadSupportRevenueSettings = async () => {
    if (!id || !canUpdateStore) return
    try {
      const data = await storeService.getSupportRevenueSettings(id)
      setSupportRevenueSettings(data)
    } catch {
      setSupportRevenueSettings({ pricePerOrderConfirmedMad: 0, pricePerOrderDeliveredMad: 0 })
    }
  }

  useEffect(() => {
    if (id && canUpdateStore) {
      loadSupportRevenueSettings()
    } else {
      setSupportRevenueSettings(null)
    }
  }, [id, canUpdateStore])

  const handleSaveSupportRevenue = async () => {
    if (!id || !supportRevenueSettings) return
    setSupportRevenueSaving(true)
    try {
      await storeService.updateSupportRevenueSettings(id, {
        pricePerOrderConfirmedMad: supportRevenueSettings.pricePerOrderConfirmedMad,
        pricePerOrderDeliveredMad: supportRevenueSettings.pricePerOrderDeliveredMad,
      })
    } finally {
      setSupportRevenueSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    )
  }

  if (!store) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">Store not found</p>
        <Link to="/stores" className="mt-4 text-primary-600 hover:text-primary-700">
          Back to Stores
        </Link>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <Link
        to="/stores"
        className="inline-flex items-center text-sm text-gray-500 hover:text-gray-700"
      >
        <ArrowLeft className="h-4 w-4 mr-2" />
        Back to Stores
      </Link>

      <div 
        className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden"
        style={{ 
          borderTopColor: store.color || '#123133',
          borderTopWidth: '4px'
        }}
      >
        <div className="px-6 py-4 border-b border-gray-200 flex items-center space-x-4">
          {store.logoUrl ? (
            <img 
              src={store.logoUrl} 
              alt={store.name} 
              className="w-16 h-16 object-cover rounded-lg border-2 border-gray-200"
            />
          ) : (
            <div 
              className="w-16 h-16 rounded-lg flex items-center justify-center shadow-md"
              style={{ backgroundColor: store.color || '#123133' }}
            >
                      <StoreIcon className="h-8 w-8 text-white" />
            </div>
          )}
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{store.name}</h1>
            <p className="text-sm text-gray-500">Code: {store.code}</p>
          </div>
        </div>
        <div className="px-6 py-4 space-y-4">
          <div>
            <label className="text-sm font-medium text-gray-500">Timezone</label>
            <p className="mt-1 text-sm text-gray-900">{store.timezone || 'Not set'}</p>
          </div>
          <div>
            <label className="text-sm font-medium text-gray-500">Created At</label>
            <p className="mt-1 text-sm text-gray-900">
              {new Date(store.createdAt).toLocaleString()}
            </p>
          </div>
        </div>
      </div>

      {/* Shipping Providers */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
          <div className="flex items-center">
            <Truck className="h-5 w-5 text-gray-400 mr-2" />
            <h2 className="text-lg font-semibold text-gray-900">Shipping Providers</h2>
          </div>
          <button
            onClick={() => setShowProviderForm(true)}
            className="inline-flex items-center px-3 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700"
          >
            <Settings className="h-4 w-4 mr-2" />
            Configure Provider
          </button>
        </div>
        <div className="px-6 py-4">
          {providers.length === 0 ? (
            <div className="text-center py-8">
              <Truck className="mx-auto h-12 w-12 text-gray-400" />
              <h3 className="mt-2 text-sm font-medium text-gray-900">No shipping providers</h3>
              <p className="mt-1 text-sm text-gray-500">
                Configure a shipping provider to start shipping orders.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {Array.isArray(providers) && providers.map((provider) => {
                const isOzonExpress = provider.providerType === 'OZON_EXPRESS' || provider.displayName?.toLowerCase().includes('ozon')
                return (
                  <div
                    key={provider.id}
                    className="border border-gray-200 rounded-lg p-4 flex items-center justify-between hover:shadow-md transition-shadow"
                  >
                    <div className="flex items-center space-x-4">
                      {isOzonExpress ? (
                        <img 
                          src="https://ozoneexpress.ma/wp/wp-content/uploads/2025/07/Untitled-design-38.png"
                          alt="Ozon Express"
                          className="h-12 w-12 object-contain"
                        />
                      ) : (
                        <div className="h-12 w-12 bg-gray-100 rounded-lg flex items-center justify-center">
                          <Truck className="h-6 w-6 text-gray-400" />
                        </div>
                      )}
                      <div>
                        <h3 className="text-sm font-medium text-gray-900">{provider.displayName}</h3>
                        <p className="text-sm text-gray-500">Type: {provider.providerType}</p>
                        <p className="text-sm text-gray-500">Customer ID: {provider.customerId}</p>
                      </div>
                    </div>
                    <span
                      className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                        provider.active
                          ? 'bg-green-100 text-green-800'
                          : 'bg-gray-100 text-gray-800'
                      }`}
                    >
                      {provider.active ? 'Active' : 'Inactive'}
                    </span>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </div>

      {/* Team Members */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
          <div className="flex items-center">
            <Users className="h-5 w-5 text-gray-400 mr-2" />
            <h2 className="text-lg font-semibold text-gray-900">Team Members</h2>
          </div>
          <button
            onClick={() => setShowTeamModal(true)}
            className="inline-flex items-center px-3 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700"
          >
            <Plus className="h-4 w-4 mr-2" />
            Add Team Member
          </button>
        </div>
        <div className="px-6 py-4">
          {teamMembers.length === 0 ? (
            <div className="text-center py-8">
              <Users className="mx-auto h-12 w-12 text-gray-400" />
              <h3 className="mt-2 text-sm font-medium text-gray-900">No team members</h3>
              <p className="mt-1 text-sm text-gray-500">
                Create a team with managers and support members.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {Array.isArray(teamMembers) && teamMembers.map((member) => (
                <div
                  key={member.id}
                  className="border border-gray-200 rounded-lg p-4 flex items-center justify-between"
                >
                  <div>
                    <h3 className="text-sm font-medium text-gray-900">
                      {member.externalMemberEmail || `User ID: ${member.userId}`}
                    </h3>
                    <p className="text-sm text-gray-500">Role: {member.role}</p>
                    <p className="text-sm text-gray-500">
                      Status: {member.invitationStatus}
                    </p>
                  </div>
                  <span
                    className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                      member.invitationStatus === 'ACCEPTED'
                        ? 'bg-green-100 text-green-800'
                        : member.invitationStatus === 'PENDING'
                        ? 'bg-yellow-100 text-yellow-800'
                        : 'bg-gray-100 text-gray-800'
                    }`}
                  >
                    {member.invitationStatus}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Support team revenue (admin/manager: set price per order for confirmation team) */}
      {canUpdateStore && id && (
        <div className="bg-white rounded-lg shadow">
          <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
            <div className="flex items-center">
              <Banknote className="h-5 w-5 text-gray-400 mr-2" />
              <h2 className="text-lg font-semibold text-gray-900">Support team revenue</h2>
            </div>
          </div>
          <div className="px-6 py-4">
            <p className="text-sm text-gray-600 mb-2">
              Set the amount (MAD) you pay to the support team member for each order they handle. This is credited to their balance when they confirm the order.
            </p>
            <p className="text-xs text-gray-500 mb-4">
              Only &quot;confirmed&quot; orders count for revenue right now. Save after changing the value.
            </p>
            {supportRevenueSettings && (
              <div className="flex flex-wrap gap-6 items-end">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Amount per order confirmed (MAD)</label>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="e.g. 10"
                    value={supportRevenueSettings.pricePerOrderConfirmedMad ?? 0}
                    onChange={(e) =>
                      setSupportRevenueSettings((s) =>
                        s ? { ...s, pricePerOrderConfirmedMad: parseFloat(e.target.value) || 0 } : s
                      )
                    }
                    className="rounded-lg border border-gray-300 px-3 py-2 w-36"
                  />
                  <p className="text-xs text-gray-500 mt-1">Paid to support when they confirm this order</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Amount per order delivered (MAD)</label>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="e.g. 15"
                    value={supportRevenueSettings.pricePerOrderDeliveredMad ?? 0}
                    onChange={(e) =>
                      setSupportRevenueSettings((s) =>
                        s ? { ...s, pricePerOrderDeliveredMad: parseFloat(e.target.value) || 0 } : s
                      )
                    }
                    className="rounded-lg border border-gray-300 px-3 py-2 w-36"
                  />
                  <p className="text-xs text-gray-500 mt-1">Reserved for when delivered revenue is enabled</p>
                </div>
                <button
                  onClick={handleSaveSupportRevenue}
                  disabled={supportRevenueSaving}
                  className="px-4 py-2 rounded-lg text-white font-medium disabled:opacity-50"
                  style={{ backgroundColor: storeColor || '#0284c7' }}
                >
                  {supportRevenueSaving ? 'Saving…' : 'Save'}
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* YouCan Integration */}
      {id && <YouCanIntegration storeId={id} />}

      {/* Google Sheets Sync */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
          <div className="flex items-center">
            <FileSpreadsheet className="h-5 w-5 text-gray-400 mr-2" />
            <h2 className="text-lg font-semibold text-gray-900">Google Sheets Synchronisation</h2>
          </div>
          <button
            onClick={() => setShowSyncModal(true)}
            className="inline-flex items-center px-3 py-2 border border-transparent text-sm font-medium rounded-md text-white transition-all"
            style={{ backgroundColor: storeColor || '#0284c7' }}
            onMouseEnter={(e) => {
              const rgb = hexToRgb(storeColor || '#0284c7')
              e.currentTarget.style.backgroundColor = `rgb(${Math.max(0, rgb.r - 20)}, ${Math.max(0, rgb.g - 20)}, ${Math.max(0, rgb.b - 20)})`
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = storeColor || '#0284c7'
            }}
          >
            {syncConfig ? (
              <>
                <Settings className="h-4 w-4 mr-2" />
                Modifier
              </>
            ) : (
              <>
                <Plus className="h-4 w-4 mr-2" />
                Lier un Google Sheet
              </>
            )}
          </button>
        </div>
        <div className="px-6 py-4">
          {!syncConfig ? (
            <div className="text-center py-8">
              <FileSpreadsheet className="mx-auto h-12 w-12 text-gray-400" />
              <h3 className="mt-2 text-sm font-medium text-gray-900">Aucun Google Sheet lié</h3>
              <p className="mt-1 text-sm text-gray-500">
                Liez un Google Sheet pour synchroniser automatiquement les commandes.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="border border-gray-200 rounded-lg p-4">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-2 mb-2">
                      <h3 className="text-sm font-medium text-gray-900">Google Sheet</h3>
                      <span
                        className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                          syncConfig.syncEnabled
                            ? 'bg-green-100 text-green-800'
                            : 'bg-gray-100 text-gray-800'
                        }`}
                      >
                        {syncConfig.syncEnabled ? 'Actif' : 'Inactif'}
                      </span>
                    </div>
                    <p className="text-sm text-gray-600 mb-1">
                      <span className="font-medium">Spreadsheet ID:</span> {syncConfig.spreadsheetId}
                    </p>
                    <p className="text-sm text-gray-600 mb-1">
                      <span className="font-medium">Feuille:</span> {syncConfig.sheetName}
                    </p>
                    <p className="text-sm text-gray-600 mb-1">
                      <span className="font-medium">Intervalle:</span> {syncConfig.syncIntervalSeconds} secondes
                    </p>
                    {syncConfig.lastSyncAt && (
                      <p className="text-sm text-gray-500 mt-2">
                        Dernière sync: {new Date(syncConfig.lastSyncAt).toLocaleString()}
                      </p>
                    )}
                    {syncConfig.lastSyncStatus && (
                      <div className="mt-2">
                        <span
                          className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                            syncConfig.lastSyncStatus === 'SUCCESS'
                              ? 'bg-green-100 text-green-800'
                              : syncConfig.lastSyncStatus === 'ERROR'
                              ? 'bg-red-100 text-red-800'
                              : 'bg-gray-100 text-gray-800'
                          }`}
                        >
                          {syncConfig.lastSyncStatus === 'SUCCESS' && (
                            <CheckCircle className="h-3 w-3 mr-1" />
                          )}
                          {syncConfig.lastSyncStatus === 'ERROR' && (
                            <XCircle className="h-3 w-3 mr-1" />
                          )}
                          {syncConfig.lastSyncStatus}
                        </span>
                      </div>
                    )}
                    {syncConfig.lastSyncError && (
                      <p className="text-xs text-red-600 mt-2">{syncConfig.lastSyncError}</p>
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {showProviderForm && id && (
        <ShippingProviderForm
          storeId={id}
          onClose={() => setShowProviderForm(false)}
          onSuccess={async () => {
            setShowProviderForm(false)
            // Add a small delay to ensure backend has saved
            await new Promise(resolve => setTimeout(resolve, 200))
            await loadProviders()
          }}
        />
      )}

      {showTeamModal && id && (
        <CreateTeamMemberModal
          storeId={id}
          onClose={() => setShowTeamModal(false)}
          onSuccess={() => {
            setShowTeamModal(false)
            loadTeamMembers()
          }}
        />
      )}

      {showSyncModal && id && (
        <GoogleSheetsSyncModal
          storeId={id}
          config={syncConfig}
          onClose={() => setShowSyncModal(false)}
          onSuccess={() => {
            setShowSyncModal(false)
            loadSyncConfig()
          }}
        />
      )}
    </div>
  )
}


import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { storeService, Store } from '../services/storeService'
import { userService } from '../services/userService'
import { useAuth } from '../contexts/AuthContext'
import { Store as StoreIcon, Check, Plus } from 'lucide-react'
import CreateStoreModal from '../components/CreateStoreModal'
import { BRAND_COLORS } from '../constants/brand'
import TalabatyLogo from '../images/talabaty-logo.png'

export default function SelectStore() {
  const [stores, setStores] = useState<Store[]>([])
  const [loading, setLoading] = useState(true)
  const [selecting, setSelecting] = useState(false)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const { user, updateUser } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    loadStores()
  }, [])

  const loadStores = async () => {
    try {
      const data = await storeService.getStores()
      setStores(Array.isArray(data) ? data : [])
    } catch (error) {
      console.error('Error loading stores:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleSelectStore = async (storeId: string) => {
    setSelecting(true)
    try {
      // Update in backend
      await userService.updateSelectedStore(storeId)
      // Update in context immediately - this is critical for ProtectedRoute
      updateUser({ selectedStoreId: storeId })
      // Force a small delay to ensure state propagation
      await new Promise(resolve => setTimeout(resolve, 150))
      // Navigate to dashboard
      navigate('/', { replace: true })
    } catch (error) {
      console.error('Error selecting store:', error)
      alert('Failed to select store')
    } finally {
      setSelecting(false)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-50 via-white to-gray-50">
        <div 
          className="animate-spin rounded-full h-12 w-12 border-2 border-transparent"
          style={{
            borderTopColor: BRAND_COLORS.primary,
            borderRightColor: BRAND_COLORS.secondary,
          }}
        ></div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="flex justify-center mb-4">
            <img 
              src={TalabatyLogo} 
              alt="Talabaty" 
              className="h-10 w-auto"
            />
          </div>
          <h2 className="text-3xl font-bold text-gray-900">Select a Store</h2>
          <p className="mt-2 text-sm text-gray-600">
            Choose a store to work with. You can see all stores you belong to, including stores from different accounts where you are a team member.
          </p>
          <div className="mt-4">
            <button
              onClick={() => setShowCreateModal(true)}
              className="inline-flex items-center px-4 py-2.5 border border-transparent shadow-sm text-sm font-semibold rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-offset-2 transition-all"
              style={{
                background: `linear-gradient(135deg, ${BRAND_COLORS.primary} 0%, ${BRAND_COLORS.secondary} 100%)`,
                '--tw-ring-color': BRAND_COLORS.primary,
              } as React.CSSProperties & { '--tw-ring-color': string }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = `linear-gradient(135deg, ${BRAND_COLORS.secondary} 0%, ${BRAND_COLORS.primary} 100%)`
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = `linear-gradient(135deg, ${BRAND_COLORS.primary} 0%, ${BRAND_COLORS.secondary} 100%)`
              }}
            >
              <Plus className="h-5 w-5 mr-2" />
              Create New Store
            </button>
          </div>
        </div>

        {/* Stores Grid */}
        {stores.length === 0 ? (
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-12 text-center">
            <div className="mx-auto w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
              <StoreIcon className="h-8 w-8 text-gray-400" />
            </div>
            <h3 className="text-sm font-medium text-gray-900">No stores available</h3>
            <p className="mt-1 text-sm text-gray-500 mb-6">
              Create your first store to get started.
            </p>
            <button
              onClick={() => setShowCreateModal(true)}
              className="inline-flex items-center px-4 py-2.5 border border-transparent shadow-sm text-sm font-semibold rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-offset-2 transition-all"
              style={{
                background: `linear-gradient(135deg, ${BRAND_COLORS.primary} 0%, ${BRAND_COLORS.secondary} 100%)`,
                '--tw-ring-color': BRAND_COLORS.primary,
              } as React.CSSProperties & { '--tw-ring-color': string }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = `linear-gradient(135deg, ${BRAND_COLORS.secondary} 0%, ${BRAND_COLORS.primary} 100%)`
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = `linear-gradient(135deg, ${BRAND_COLORS.primary} 0%, ${BRAND_COLORS.secondary} 100%)`
              }}>
              <Plus className="h-5 w-5 mr-2" />
              Create Your First Store
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {stores.map((store) => {
              const isSelected = user?.selectedStoreId === store.id
              const storeColor = store.color || BRAND_COLORS.primary
              return (
                <button
                  key={store.id}
                  onClick={() => !selecting && handleSelectStore(store.id)}
                  disabled={selecting}
                  className={`bg-white rounded-xl shadow-sm border-2 p-6 hover:shadow-lg transition-all text-left relative group ${
                    isSelected 
                      ? 'border-green-500 bg-green-50' 
                      : 'border-gray-200 hover:border-blue-300'
                  } ${selecting ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
                  style={isSelected ? { borderColor: storeColor } : {}}
                >
                  <div className="flex items-center justify-between mb-4">
                    {store.logoUrl ? (
                      <img 
                        src={store.logoUrl} 
                        alt={store.name} 
                        className="w-16 h-16 object-cover rounded-xl shadow-md border-2 border-gray-200"
                      />
                    ) : (
                      <div 
                        className="p-3 rounded-xl shadow-md"
                        style={{ backgroundColor: storeColor }}
                      >
                        <StoreIcon className="h-6 w-6 text-white" />
                      </div>
                    )}
                    {isSelected && (
                      <div className="bg-green-500 p-2 rounded-full shadow-md">
                        <Check className="h-5 w-5 text-white" />
                      </div>
                    )}
                  </div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-1">{store.name}</h3>
                  <p className="text-sm text-gray-500">Code: {store.code || 'N/A'}</p>
                  {isSelected && (
                    <p className="mt-3 text-sm font-medium" style={{ color: storeColor }}>
                      Currently Selected
                    </p>
                  )}
                </button>
              )
            })}
          </div>
        )}

        {showCreateModal && (
          <CreateStoreModal
            onClose={() => setShowCreateModal(false)}
            onSuccess={async (newStore?: Store) => {
              setShowCreateModal(false)
              await loadStores()
              // Automatically select the newly created store
              if (newStore) {
                await handleSelectStore(newStore.id)
              }
            }}
          />
        )}
      </div>
    </div>
  )
}

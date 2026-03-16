import { useEffect, useState } from 'react'
import { storeService, Store } from '../../services/storeService'
import { Plus, Edit, Trash2, Search } from 'lucide-react'
import CreateStoreModal from '../CreateStoreModal'
import { useStoreColor } from '../../hooks/useStoreColor'

export default function StoresSettings() {
  const { storeColor } = useStoreColor()
  const [stores, setStores] = useState<Store[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')

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

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this store?')) return

    try {
      await storeService.deleteStore(id)
      loadStores()
    } catch (error) {
      console.error('Error deleting store:', error)
      alert('Failed to delete store')
    }
  }

  const filteredStores = stores.filter(
    (store) =>
      store.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      store.code?.toLowerCase().includes(searchQuery.toLowerCase())
  )

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
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-xl font-semibold text-gray-900">Stores</h2>
            <p className="mt-1 text-sm text-gray-600">Manage your stores</p>
          </div>
          <button
            onClick={() => setShowCreateModal(true)}
            className="flex items-center px-4 py-2 text-white rounded-lg hover:opacity-90 transition-all shadow-sm"
            style={{ backgroundColor: storeColor }}
          >
            <Plus className="h-4 w-4 mr-2" />
            Add Store
          </button>
        </div>

        {/* Search */}
        <div className="mb-6">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search stores..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
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
        </div>

        {/* Stores List */}
        {filteredStores.length === 0 ? (
          <div className="text-center py-12">
            <div className="mx-auto w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
              <Plus className="h-8 w-8 text-gray-400" />
            </div>
            <h3 className="text-sm font-medium text-gray-900">No stores</h3>
            <p className="mt-1 text-sm text-gray-500">Get started by creating a new store.</p>
            <div className="mt-6">
              <button
                onClick={() => setShowCreateModal(true)}
                className="inline-flex items-center px-4 py-2 text-white rounded-lg hover:opacity-90 transition-all shadow-sm"
                style={{ backgroundColor: storeColor }}
              >
                <Plus className="h-4 w-4 mr-2" />
                Add Store
              </button>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {filteredStores.map((store) => (
              <div
                key={store.id}
                className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow"
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <h3 className="text-lg font-semibold text-gray-900">{store.name}</h3>
                    {store.code && <p className="text-sm text-gray-500 mt-1">Code: {store.code}</p>}
                  </div>
                  <div className="flex items-center space-x-2">
                    <button
                      onClick={() => {
                        // TODO: Implement edit
                        alert('Edit functionality coming soon')
                      }}
                      className="p-2 text-gray-400 hover:text-gray-600 transition-colors"
                    >
                      <Edit className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(store.id)}
                      className="p-2 text-gray-400 hover:text-red-600 transition-colors"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {showCreateModal && (
        <CreateStoreModal
          onClose={() => {
            setShowCreateModal(false)
          }}
          onSuccess={() => {
            setShowCreateModal(false)
            loadStores()
          }}
        />
      )}
    </div>
  )
}


import { useEffect, useState } from 'react'
import { storeService, Store, ShippingProvider } from '../../services/storeService'
import { useStoreColor } from '../../hooks/useStoreColor'
import { Truck, Plus, Settings } from 'lucide-react'
import ShippingProviderForm from '../ShippingProviderForm'
import { DELIVERY_PROVIDERS, getProviderLogo } from '../../utils/deliveryProviders'

interface ShippingProvidersSettingsProps {
  /** When true, hide the section title (e.g. when embedded in Integrations page) */
  hideTitle?: boolean
}

export default function ShippingProvidersSettings({ hideTitle }: ShippingProvidersSettingsProps) {
  const { storeColor } = useStoreColor()
  const [stores, setStores] = useState<Store[]>([])
  const [storeProviders, setStoreProviders] = useState<Record<string, ShippingProvider[]>>({})
  const [loading, setLoading] = useState(true)
  const [selectedStore, setSelectedStore] = useState<string | null>(null)
  const [selectedProvider, setSelectedProvider] = useState<string | null>(null)
  const [editingProvider, setEditingProvider] = useState<ShippingProvider | null>(null)
  const [showProviderForm, setShowProviderForm] = useState(false)

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      const storesData = await storeService.getStores()
      setStores(storesData)

      const providersMap: Record<string, ShippingProvider[]> = {}
      for (const store of storesData) {
        try {
          const providers = await storeService.getShippingProviders(store.id)
          providersMap[store.id] = Array.isArray(providers) ? providers : []
        } catch (error) {
          providersMap[store.id] = []
        }
      }
      setStoreProviders(providersMap)
    } catch (error) {
      console.error('Error loading data:', error)
    } finally {
      setLoading(false)
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
      <div className={hideTitle ? 'p-6' : 'bg-white rounded-lg shadow-sm border border-gray-200 p-6'}>
        {!hideTitle && (
          <div className="mb-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-2">Connect your Delivery companies</h2>
            <p className="text-sm text-gray-600">Choose your delivery company to link it.</p>
          </div>
        )}

        {/* Delivery Companies Grid */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Object.values(DELIVERY_PROVIDERS).map((provider) => (
            <div
              key={provider.name}
              className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow cursor-pointer"
              onClick={() => {
                if (stores.length > 0) {
                  setSelectedStore(stores[0].id)
                  setSelectedProvider(provider.name)
                  setEditingProvider(null)
                  setShowProviderForm(true)
                }
              }}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  {provider.logo ? (
                    <img 
                      src={provider.logo} 
                      alt={provider.name}
                      className="h-12 w-12 object-contain"
                      onError={(e) => {
                        // Fallback to icon if image fails
                        e.currentTarget.style.display = 'none'
                        const fallback = e.currentTarget.nextElementSibling as HTMLElement
                        if (fallback) fallback.style.display = 'flex'
                      }}
                    />
                  ) : null}
                  <div 
                    className="h-12 w-12 rounded-lg flex items-center justify-center" 
                    style={{ 
                      backgroundColor: storeColor + '20',
                      display: provider.logo ? 'none' : 'flex'
                    }}
                  >
                    <Truck className="h-6 w-6" style={{ color: storeColor }} />
                  </div>
                  <div>
                    <h3 className="text-sm font-semibold text-gray-900">{provider.name}</h3>
                    <p className="text-xs text-gray-500">One-click integration with {provider.name}</p>
                  </div>
                </div>
                <button 
                  className="p-2 text-gray-400 hover:text-gray-600 transition-colors"
                  style={{ color: storeColor }}
                >
                  <Settings className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Connected Providers */}
      {Object.keys(storeProviders).length > 0 && (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Connected Providers</h3>
          <div className="space-y-4">
            {Object.entries(storeProviders).map(([storeId, providers]) => {
              const store = stores.find((s) => s.id === storeId)
              if (!providers.length) return null

              return (
                <div key={storeId} className="border border-gray-200 rounded-lg p-4">
                  <h4 className="text-sm font-semibold text-gray-900 mb-3">{store?.name || 'Store'}</h4>
                  <div className="space-y-2">
                    {providers.map((provider) => {
                      const providerLogo = getProviderLogo(provider.providerType, provider.displayName)
                      return (
                        <div key={provider.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
                          <div className="flex items-center space-x-3">
                            {providerLogo ? (
                              <img 
                                src={providerLogo} 
                                alt={provider.displayName}
                                className="h-8 w-8 object-contain"
                                onError={(e) => {
                                  e.currentTarget.style.display = 'none'
                                  const fallback = e.currentTarget.nextElementSibling as HTMLElement
                                  if (fallback) fallback.style.display = 'flex'
                                }}
                              />
                            ) : null}
                            <div 
                              className="h-8 w-8 rounded flex items-center justify-center"
                              style={{ 
                                backgroundColor: storeColor + '20',
                                display: providerLogo ? 'none' : 'flex'
                              }}
                            >
                              <Truck className="h-4 w-4" style={{ color: storeColor }} />
                            </div>
                            <span className="text-sm font-medium text-gray-900">{provider.displayName}</span>
                          </div>
                          <button 
                            onClick={() => {
                              setSelectedStore(storeId)
                              setSelectedProvider(null)
                              setEditingProvider(provider)
                              setShowProviderForm(true)
                            }}
                            className="text-sm font-medium hover:opacity-80 transition-opacity px-3 py-1 rounded-md"
                            style={{ 
                              color: storeColor,
                              backgroundColor: storeColor + '10'
                            }}
                          >
                            Configure
                          </button>
                        </div>
                      )
                    })}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {showProviderForm && selectedStore && (
        <ShippingProviderForm
          storeId={selectedStore}
          providerName={selectedProvider || undefined}
          existingProvider={editingProvider || undefined}
          onClose={() => {
            setShowProviderForm(false)
            setSelectedStore(null)
            setSelectedProvider(null)
            setEditingProvider(null)
            loadData()
          }}
          onSuccess={() => {
            setShowProviderForm(false)
            setSelectedStore(null)
            setSelectedProvider(null)
            setEditingProvider(null)
            loadData()
          }}
        />
      )}
    </div>
  )
}


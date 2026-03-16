import { useEffect, useState } from 'react'
import { storeService, Store, ShippingProvider } from '../services/storeService'
import { Truck, Settings, Plus } from 'lucide-react'

export default function ShippingProviders() {
  const [stores, setStores] = useState<Store[]>([])
  const [storeProviders, setStoreProviders] = useState<Record<string, ShippingProvider[]>>({})
  const [loading, setLoading] = useState(true)

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
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Shipping Providers</h1>
        <p className="mt-2 text-gray-600">
          Configure shipping providers for each store to enable order shipping
        </p>
      </div>

      {!stores || stores.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-12 text-center">
          <Truck className="mx-auto h-12 w-12 text-gray-400" />
          <h3 className="mt-2 text-sm font-medium text-gray-900">No stores found</h3>
          <p className="mt-1 text-sm text-gray-500">
            Create a store first to configure shipping providers.
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          {Array.isArray(stores) && stores.map((store) => {
            const providers = storeProviders[store.id] || []
            return (
              <div key={store.id} className="bg-white rounded-lg shadow">
                <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
                  <div>
                    <h2 className="text-lg font-semibold text-gray-900">{store.name}</h2>
                    <p className="text-sm text-gray-500">Code: {store.code}</p>
                  </div>
                  <a
                    href={`/stores/${store.id}`}
                    className="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
                  >
                    <Settings className="h-4 w-4 mr-2" />
                    Configure
                  </a>
                </div>
                <div className="px-6 py-4">
                  {providers.length === 0 ? (
                    <div className="text-center py-8">
                      <Truck className="mx-auto h-8 w-8 text-gray-400" />
                      <p className="mt-2 text-sm text-gray-500">No shipping providers configured</p>
                      <a
                        href={`/stores/${store.id}`}
                        className="mt-4 inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700"
                      >
                        <Plus className="h-4 w-4 mr-2" />
                        Add Provider
                      </a>
                    </div>
                  ) : (
                    <div className="space-y-3">
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
                                <h3 className="text-sm font-medium text-gray-900">
                                  {provider.displayName}
                                </h3>
                                <p className="text-sm text-gray-500">
                                  Type: {provider.providerType}
                                </p>
                                <p className="text-sm text-gray-500">
                                  Customer ID: {provider.customerId}
                                </p>
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
            )
          })}
        </div>
      )}
    </div>
  )
}


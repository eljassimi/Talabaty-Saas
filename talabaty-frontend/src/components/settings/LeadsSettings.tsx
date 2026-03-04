import { useState, useEffect } from 'react'
import { Check } from 'lucide-react'
import { useStoreColor } from '../../hooks/useStoreColor'
import { useAuth } from '../../contexts/AuthContext'
import YouCanIntegration from '../YouCanIntegration'
import { youcanService } from '../../services/youcanService'
import { googleSheetsSyncService, GoogleSheetsSyncConfig } from '../../services/googleSheetsSyncService'
import GoogleSheetsSyncModal from '../GoogleSheetsSyncModal'

export default function LeadsSettings() {
  const { storeColor } = useStoreColor()
  const { user } = useAuth()
  const [youcanConnected, setYoucanConnected] = useState(false)
  const [googleSheetsConnected, setGoogleSheetsConnected] = useState(false)
  const [googleSheetsConfig, setGoogleSheetsConfig] = useState<GoogleSheetsSyncConfig | null>(null)
  const [showGoogleSheetsModal, setShowGoogleSheetsModal] = useState(false)

  useEffect(() => {
    if (user?.selectedStoreId) {
      checkYouCanConnection()
      checkGoogleSheetsConnection()
    }
  }, [user?.selectedStoreId])

  // Re-check connection when component updates
  useEffect(() => {
    const interval = setInterval(() => {
      if (user?.selectedStoreId) {
        checkYouCanConnection()
      }
    }, 5000) // Check every 5 seconds

    return () => clearInterval(interval)
  }, [user?.selectedStoreId])

  const checkYouCanConnection = async () => {
    if (!user?.selectedStoreId) return
    try {
      const stores = await youcanService.getConnectedStores()
      const connected = stores.some(s => s.storeId === user.selectedStoreId)
      setYoucanConnected(connected)
    } catch (error) {
      console.error('Error checking YouCan connection:', error)
    }
  }

  const checkGoogleSheetsConnection = async () => {
    if (!user?.selectedStoreId) return
    try {
      const configs = await googleSheetsSyncService.getConfigsByStore(user.selectedStoreId)
      if (configs && configs.length > 0) {
        setGoogleSheetsConnected(true)
        setGoogleSheetsConfig(configs[0]) // Use first config
      } else {
        setGoogleSheetsConnected(false)
        setGoogleSheetsConfig(null)
      }
    } catch (error) {
      console.error('Error checking Google Sheets connection:', error)
      setGoogleSheetsConnected(false)
      setGoogleSheetsConfig(null)
    }
  }


  const leadCollectors = [
    {
      id: 'youcan',
      name: 'YouCan',
      description: 'E-commerce platform',
      icon: 'y',
      iconBg: '#ec4899', // Pink background
      logoUrl: 'https://developer.youcan.shop/logo-with-shadow.webp',
      connected: youcanConnected,
      component: 'youcan',
    },
    {
      id: 'shopify',
      name: 'Shopify',
      description: 'E-commerce platform',
      icon: 'S',
      iconBg: '#1f2937', // Dark grey
      logoUrl: 'https://cdn.shopify.com/shopifycloud/brochure/assets/brand-assets/shopify-logo-shopping-bag-full-color-66166b2e55d67988b56b4bd28b63c271e2b9713358cb723070a92bde17ad7d63.svg',
      connected: false,
      component: 'shopify',
    },
    {
      id: 'lightfunnels',
      name: 'Lightfunnels',
      description: 'E-commerce platform.',
      icon: 'LF',
      iconBg: '#1f2937', // Dark grey
      logoUrl: 'https://image.crisp.chat/avatar/website/5a6d16d6-72d4-4240-bd2f-c7cf71c87083/60/?1764848253754',
      connected: false,
      component: 'lightfunnels',
    },
    {
      id: 'wordpress',
      name: 'WordPress',
      description: 'Content management system with WooCommerce',
      icon: 'WP',
      iconBg: '#1f2937', // Dark grey
      logoUrl: 'https://i0.wp.com/wordpress.org/files/2023/02/wmark.png?w=500&ssl=1',
      connected: false,
      component: 'wordpress',
    },
    {
      id: 'google-sheet',
      name: 'Google Sheet',
      description: 'Sync orders from Google Sheets',
      icon: '📊',
      iconBg: '#34a853', // Google green
      logoUrl: 'https://www.gstatic.com/images/branding/product/2x/sheets_2020q4_48dp.png',
      connected: googleSheetsConnected,
      component: 'google-sheet',
    },
    {
      id: 'file-upload',
      name: 'File Upload',
      description: 'Upload your own CSV file.',
      icon: '📄',
      iconBg: '#1f2937', // Dark grey
      connected: false,
      component: 'file-upload',
    },
  ]

  const handleCollectorClick = (collector: typeof leadCollectors[0]) => {
    if (collector.component === 'youcan') {
      // YouCan integration is handled in the component below
      return
    }
    if (collector.component === 'google-sheet') {
      // Open Google Sheets modal
      setShowGoogleSheetsModal(true)
      return
    }
    // For other integrations, show coming soon or open modal
    if (collector.component === 'shopify' || collector.component === 'lightfunnels' || collector.component === 'wordpress') {
      alert(`${collector.name} integration coming soon!`)
    }
  }

  return (
    <div className="space-y-6">
      <div 
        className="bg-white rounded-xl shadow-sm border p-6"
        style={{ 
          borderColor: storeColor + '15',
          boxShadow: `0 1px 3px 0 ${storeColor}08`
        }}
      >
        <div className="mb-6">
          <h2 
            className="text-xl font-semibold mb-2"
            style={{ color: storeColor }}
          >
            Connect your Lead Collector
          </h2>
          <p className="text-sm text-gray-600">Choose your lead collector to link it.</p>
        </div>

        {/* Lead Collectors Grid */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {leadCollectors.map((collector) => (
            <div
              key={collector.id}
              onClick={() => handleCollectorClick(collector)}
              className={`border rounded-xl p-5 hover:shadow-lg transition-all duration-300 cursor-pointer relative bg-white group ${
                collector.connected 
                  ? 'border-green-300 shadow-md' 
                  : 'border-gray-200 hover:border-opacity-60'
              }`}
              style={collector.connected ? {
                borderColor: storeColor + '40',
                boxShadow: `0 4px 6px -1px ${storeColor}15, 0 2px 4px -1px ${storeColor}10`
              } : {}}
              onMouseEnter={(e) => {
                if (!collector.connected) {
                  e.currentTarget.style.borderColor = storeColor + '60'
                  e.currentTarget.style.transform = 'translateY(-2px)'
                }
              }}
              onMouseLeave={(e) => {
                if (!collector.connected) {
                  e.currentTarget.style.borderColor = ''
                  e.currentTarget.style.transform = ''
                }
              }}
            >
              {collector.connected && (
                <div className="absolute -top-2 -left-2 z-10">
                  <div 
                    className="rounded-full p-1 shadow-lg"
                    style={{ backgroundColor: storeColor }}
                  >
                    <Check className="h-3.5 w-3.5 text-white" />
                  </div>
                </div>
              )}
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-4 flex-1">
                  {collector.logoUrl ? (
                    <div 
                      className="h-14 w-14 rounded-xl flex items-center justify-center bg-gradient-to-br from-white to-gray-50 border-2 overflow-hidden p-1.5 transition-all duration-300 group-hover:scale-110"
                      style={collector.connected ? {
                        borderColor: storeColor + '30',
                        boxShadow: `0 2px 8px ${storeColor}20`
                      } : {
                        borderColor: '#e5e7eb'
                      }}
                    >
                      <img 
                        src={collector.logoUrl} 
                        alt={collector.name} 
                        className="h-full w-full object-contain"
                        onError={(e) => {
                          // Fallback if image fails to load
                          const target = e.target as HTMLImageElement
                          target.style.display = 'none'
                          const parent = target.parentElement
                          if (parent) {
                            parent.innerHTML = `<div class="h-full w-full flex items-center justify-center text-white font-bold text-lg rounded-lg" style="background: linear-gradient(135deg, ${collector.iconBg || '#1f2937'} 0%, ${collector.iconBg || '#1f2937'}dd 100%);">${collector.icon}</div>`
                          }
                        }}
                      />
                    </div>
                  ) : (
                    <div
                      className="h-14 w-14 rounded-xl flex items-center justify-center text-white font-bold text-lg transition-all duration-300 group-hover:scale-110 shadow-md"
                      style={{ 
                        background: collector.connected 
                          ? `linear-gradient(135deg, ${storeColor} 0%, ${storeColor}dd 100%)`
                          : `linear-gradient(135deg, ${collector.iconBg || '#1f2937'} 0%, ${collector.iconBg || '#1f2937'}dd 100%)`
                      }}
                    >
                      {collector.icon}
                    </div>
                  )}
                  <div className="flex-1 min-w-0">
                    <h3 
                      className="text-sm font-semibold truncate mb-0.5 transition-colors"
                      style={collector.connected ? { color: storeColor } : { color: '#111827' }}
                    >
                      {collector.name}
                    </h3>
                    <p className="text-xs text-gray-500 truncate">{collector.description}</p>
                  </div>
                </div>
                {!collector.connected && (
                  <div 
                    className="opacity-0 group-hover:opacity-100 transition-opacity duration-300"
                    style={{ color: storeColor }}
                  >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* YouCan Integration Section - Only show if not connected */}
      {user?.selectedStoreId && !youcanConnected && (
        <div 
          className="bg-white rounded-xl shadow-sm border p-6"
          style={{ 
            borderColor: storeColor + '20',
            boxShadow: `0 1px 3px 0 ${storeColor}10`
          }}
        >
          <h3 
            className="text-lg font-semibold mb-4"
            style={{ color: storeColor }}
          >
            YouCan Integration
          </h3>
          <YouCanIntegration 
            storeId={user.selectedStoreId} 
            onConnectionChange={() => checkYouCanConnection()} 
          />
        </div>
      )}

      {/* Connected Integrations */}
      {(youcanConnected || googleSheetsConnected) && (
        <div 
          className="bg-white rounded-xl shadow-sm border p-6"
          style={{ 
            borderColor: storeColor + '20',
            boxShadow: `0 1px 3px 0 ${storeColor}10`
          }}
        >
          <h3 
            className="text-lg font-semibold mb-4"
            style={{ color: storeColor }}
          >
            Connected Integrations
          </h3>
          <div className="space-y-3">
            {youcanConnected && (
              <div 
                className="flex items-center justify-between p-4 border rounded-xl transition-all duration-300 hover:shadow-md"
                style={{ 
                  borderColor: storeColor + '30',
                  backgroundColor: storeColor + '08'
                }}
              >
                <div className="flex items-center space-x-3">
                  <div 
                    className="h-12 w-12 rounded-xl flex items-center justify-center bg-white border-2 overflow-hidden p-1.5 shadow-sm"
                    style={{ borderColor: storeColor + '30' }}
                  >
                    <img 
                      src="https://developer.youcan.shop/logo-with-shadow.webp" 
                      alt="YouCan" 
                      className="h-full w-full object-contain"
                      onError={(e) => {
                        // Fallback if image fails to load
                        const target = e.target as HTMLImageElement
                        target.style.display = 'none'
                        const parent = target.parentElement
                        if (parent) {
                          parent.innerHTML = `<div class="h-full w-full flex items-center justify-center text-white font-bold text-sm rounded-lg" style="background: linear-gradient(135deg, ${storeColor} 0%, ${storeColor}dd 100%);">Y</div>`
                        }
                      }}
                    />
                  </div>
                  <div>
                    <p 
                      className="text-sm font-semibold"
                      style={{ color: storeColor }}
                    >
                      YouCan
                    </p>
                    <p className="text-xs text-gray-500">E-commerce platform</p>
                  </div>
                </div>
                <div 
                  className="flex items-center"
                  style={{ color: storeColor }}
                >
                  <div 
                    className="rounded-full p-1 mr-2"
                    style={{ backgroundColor: storeColor + '20' }}
                  >
                    <Check className="h-4 w-4" style={{ color: storeColor }} />
                  </div>
                  <span className="text-sm font-medium">Connected</span>
                </div>
              </div>
            )}
            {googleSheetsConnected && (
              <div 
                className="flex items-center justify-between p-4 border rounded-xl transition-all duration-300 hover:shadow-md cursor-pointer"
                style={{ 
                  borderColor: storeColor + '30',
                  backgroundColor: storeColor + '08'
                }}
                onClick={() => setShowGoogleSheetsModal(true)}
              >
                <div className="flex items-center space-x-3">
                  <div 
                    className="h-12 w-12 rounded-xl flex items-center justify-center bg-white border-2 overflow-hidden p-1.5 shadow-sm"
                    style={{ borderColor: storeColor + '30' }}
                  >
                    <img 
                      src="https://www.gstatic.com/images/branding/product/2x/sheets_2020q4_48dp.png" 
                      alt="Google Sheets" 
                      className="h-full w-full object-contain"
                      onError={(e) => {
                        // Fallback if image fails to load
                        const target = e.target as HTMLImageElement
                        target.style.display = 'none'
                        const parent = target.parentElement
                        if (parent) {
                          parent.innerHTML = `<div class="h-full w-full flex items-center justify-center text-white font-bold text-sm rounded-lg" style="background: linear-gradient(135deg, ${storeColor} 0%, ${storeColor}dd 100%);">📊</div>`
                        }
                      }}
                    />
                  </div>
                  <div>
                    <p 
                      className="text-sm font-semibold"
                      style={{ color: storeColor }}
                    >
                      Google Sheet
                    </p>
                    <p className="text-xs text-gray-500">Sync orders from Google Sheets</p>
                  </div>
                </div>
                <div 
                  className="flex items-center"
                  style={{ color: storeColor }}
                >
                  <div 
                    className="rounded-full p-1 mr-2"
                    style={{ backgroundColor: storeColor + '20' }}
                  >
                    <Check className="h-4 w-4" style={{ color: storeColor }} />
                  </div>
                  <span className="text-sm font-medium">Connected</span>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Google Sheets Modal */}
      {user?.selectedStoreId && showGoogleSheetsModal && (
        <GoogleSheetsSyncModal
          storeId={user.selectedStoreId}
          config={googleSheetsConfig}
          onClose={() => {
            setShowGoogleSheetsModal(false)
            checkGoogleSheetsConnection()
          }}
          onSuccess={() => {
            checkGoogleSheetsConnection()
          }}
        />
      )}
    </div>
  )
}



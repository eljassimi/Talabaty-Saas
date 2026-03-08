import { useState, useEffect } from 'react'
import { Check, Unplug, ShoppingCart, Truck } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { useStoreColor } from '../hooks/useStoreColor'
import { youcanService, YouCanStore } from '../services/youcanService'
import { googleSheetsSyncService, GoogleSheetsSyncConfig } from '../services/googleSheetsSyncService'
import YouCanIntegration from '../components/YouCanIntegration'
import GoogleSheetsSyncModal from '../components/GoogleSheetsSyncModal'
import ShippingProvidersSettings from '../components/settings/ShippingProvidersSettings'
import youcanLogo from '../images/youcan-logo.png'

const ECOM_PLATFORMS = [
  {
    id: 'youcan',
    name: 'YouCan',
    description: 'E-commerce platform – sync orders automatically',
    logoUrl: youcanLogo,
    icon: 'Y',
    iconBg: '#ec4899',
  },
  {
    id: 'google-sheet',
    name: 'Google Sheets',
    description: 'Sync orders from a Google Sheet',
    logoUrl: 'https://www.gstatic.com/images/branding/product/2x/sheets_2020q4_48dp.png',
    icon: '📊',
    iconBg: '#34a853',
  },
  {
    id: 'shopify',
    name: 'Shopify',
    description: 'E-commerce platform',
    logoUrl: 'https://cdn.shopify.com/shopifycloud/brochure/assets/brand-assets/shopify-logo-shopping-bag-full-color-66166b2e55d67988b56b4bd28b63c271e2b9713358cb723070a92bde17ad7d63.svg',
    icon: 'S',
    iconBg: '#1f2937',
    comingSoon: true,
  },
]

export default function Integrations() {
  const { user } = useAuth()
  const { storeColor } = useStoreColor()
  const [youcanConnected, setYoucanConnected] = useState(false)
  const [connectedYouCanStore, setConnectedYouCanStore] = useState<YouCanStore | null>(null)
  const [disconnectingYouCan, setDisconnectingYouCan] = useState(false)
  const [googleSheetsConnected, setGoogleSheetsConnected] = useState(false)
  const [googleSheetsConfig, setGoogleSheetsConfig] = useState<GoogleSheetsSyncConfig | null>(null)
  const [showGoogleSheetsModal, setShowGoogleSheetsModal] = useState(false)
  const [disconnectingSheets, setDisconnectingSheets] = useState(false)

  useEffect(() => {
    if (user?.selectedStoreId) {
      checkYouCanConnection()
      checkGoogleSheetsConnection()
    }
  }, [user?.selectedStoreId])

  const checkYouCanConnection = async () => {
    if (!user?.selectedStoreId) return
    try {
      const stores = await youcanService.getConnectedStores()
      const forStore = stores.find((s) => s.storeId === user.selectedStoreId)
      setYoucanConnected(!!forStore)
      setConnectedYouCanStore(forStore ?? null)
    } catch {
      setYoucanConnected(false)
      setConnectedYouCanStore(null)
    }
  }

  const checkGoogleSheetsConnection = async () => {
    if (!user?.selectedStoreId) return
    try {
      const configs = await googleSheetsSyncService.getConfigsByStore(user.selectedStoreId)
      if (configs?.length > 0) {
        setGoogleSheetsConnected(true)
        setGoogleSheetsConfig(configs[0])
      } else {
        setGoogleSheetsConnected(false)
        setGoogleSheetsConfig(null)
      }
    } catch {
      setGoogleSheetsConnected(false)
      setGoogleSheetsConfig(null)
    }
  }

  const handleDisconnectYouCan = async () => {
    if (!connectedYouCanStore || disconnectingYouCan) return
    if (!confirm('Disconnect YouCan from this store? You can reconnect anytime.')) return
    setDisconnectingYouCan(true)
    try {
      await youcanService.disconnectStore(connectedYouCanStore.id)
      await checkYouCanConnection()
    } catch {
      alert('Failed to disconnect. Please try again.')
    } finally {
      setDisconnectingYouCan(false)
    }
  }

  const handleDisconnectGoogleSheets = async () => {
    if (!googleSheetsConfig || disconnectingSheets) return
    if (!confirm('Remove Google Sheets sync? You can set it up again anytime.')) return
    setDisconnectingSheets(true)
    try {
      await googleSheetsSyncService.deleteConfig(googleSheetsConfig.id)
      await checkGoogleSheetsConnection()
    } catch {
      alert('Failed to disconnect. Please try again.')
    } finally {
      setDisconnectingSheets(false)
    }
  }

  const getEcomStatus = (id: string) => {
    if (id === 'youcan') return youcanConnected
    if (id === 'google-sheet') return googleSheetsConnected
    return false
  }

  const handleEcomClick = (id: string) => {
    if (id === 'shopify' || id === 'youcan') return // YouCan handled below; Shopify coming soon
    if (id === 'google-sheet') setShowGoogleSheetsModal(true)
  }

  if (!user?.selectedStoreId) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-[#111827]">Integrations</h1>
          <p className="mt-1 text-sm text-[#6B7280]">Connect your e-commerce platform and delivery providers.</p>
        </div>
        <div className="bg-white rounded-xl border border-[#E6E8EC] p-8 text-center">
          <p className="text-[#6B7280]">Select a store to manage integrations.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-[#111827]">Integrations</h1>
        <p className="mt-1 text-sm text-[#6B7280]">
          Connect your e-commerce platform and delivery providers in one place.
        </p>
      </div>

      {/* E-commerce platforms */}
      <section className="bg-white rounded-xl border border-[#E6E8EC] p-6 shadow-sm">
        <div className="flex items-center gap-2 mb-5">
          <ShoppingCart className="h-5 w-5" style={{ color: storeColor }} />
          <h2 className="text-lg font-semibold text-[#111827]">E-commerce platforms</h2>
        </div>
        <p className="text-sm text-[#6B7280] mb-6">
          Connect where your orders come from. Sync orders automatically from YouCan or Google Sheets.
        </p>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {ECOM_PLATFORMS.map((platform) => {
            const connected = getEcomStatus(platform.id)
            const isYouCan = platform.id === 'youcan'
            return (
              <div
                key={platform.id}
                onClick={() => !platform.comingSoon && (isYouCan ? undefined : handleEcomClick(platform.id))}
                className={`
                  border rounded-xl p-5 transition-all duration-200
                  ${connected ? 'border-[#E6E8EC] bg-[#F6F8FB]' : 'border-[#E6E8EC] hover:border-[#d1d5db] hover:shadow-md'}
                  ${!platform.comingSoon && !isYouCan ? 'cursor-pointer' : ''}
                  ${platform.comingSoon ? 'opacity-75' : ''}
                `}
                style={connected ? { borderColor: `${storeColor}40`, backgroundColor: `${storeColor}08` } : undefined}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3 min-w-0">
                    {platform.logoUrl ? (
                      <div className="h-12 w-12 rounded-xl flex items-center justify-center bg-white border border-[#E6E8EC] overflow-hidden p-1.5 shrink-0">
                        <img src={platform.logoUrl} alt={platform.name} className="h-full w-full object-contain" />
                      </div>
                    ) : (
                      <div
                        className="h-12 w-12 rounded-xl flex items-center justify-center text-white font-bold text-lg shrink-0"
                        style={{ backgroundColor: platform.iconBg }}
                      >
                        {platform.icon}
                      </div>
                    )}
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-[#111827] truncate">{platform.name}</p>
                      <p className="text-xs text-[#6B7280] truncate">{platform.description}</p>
                    </div>
                  </div>
                  {connected && (
                    <div className="flex items-center gap-1.5 shrink-0" style={{ color: storeColor }}>
                      <Check className="h-4 w-4" />
                      <span className="text-xs font-medium">Connected</span>
                    </div>
                  )}
                  {platform.comingSoon && (
                    <span className="text-xs font-medium text-[#6B7280] bg-[#E6E8EC] px-2 py-1 rounded-md shrink-0">
                      Coming soon
                    </span>
                  )}
                </div>
                {connected && isYouCan && (
                  <div className="mt-4 pt-4 border-t border-[#E6E8EC] flex flex-wrap items-center gap-2">
                    <button
                      type="button"
                      onClick={(e) => { e.stopPropagation(); handleDisconnectYouCan() }}
                      disabled={disconnectingYouCan}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-red-600 hover:bg-red-50 rounded-lg disabled:opacity-50"
                    >
                      <Unplug className="h-3.5 w-3.5" />
                      Disconnect
                    </button>
                  </div>
                )}
                {connected && platform.id === 'google-sheet' && (
                  <div className="mt-4 pt-4 border-t border-[#E6E8EC] flex flex-wrap items-center gap-2">
                    <button
                      type="button"
                      onClick={(e) => { e.stopPropagation(); setShowGoogleSheetsModal(true) }}
                      className="text-sm font-medium hover:underline"
                      style={{ color: storeColor }}
                    >
                      Configure
                    </button>
                    <button
                      type="button"
                      onClick={(e) => { e.stopPropagation(); handleDisconnectGoogleSheets() }}
                      disabled={disconnectingSheets}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-red-600 hover:bg-red-50 rounded-lg disabled:opacity-50"
                    >
                      <Unplug className="h-3.5 w-3.5" />
                      Disconnect
                    </button>
                  </div>
                )}
              </div>
            )
          })}
        </div>

        {/* YouCan connect flow (when not connected) */}
        {!youcanConnected && (
          <div className="mt-6 pt-6 border-t border-[#E6E8EC]">
            <h3 className="text-sm font-semibold text-[#111827] mb-4">Connect YouCan</h3>
            <YouCanIntegration storeId={user.selectedStoreId} onConnectionChange={checkYouCanConnection} />
          </div>
        )}
      </section>

      {/* Delivery providers */}
      <section className="bg-white rounded-xl border border-[#E6E8EC] overflow-hidden shadow-sm">
        <div className="p-6 pb-4 flex items-center gap-2">
          <Truck className="h-5 w-5" style={{ color: storeColor }} />
          <h2 className="text-lg font-semibold text-[#111827]">Delivery providers</h2>
        </div>
        <p className="text-sm text-[#6B7280] px-6 pb-6">
          Link delivery companies to ship orders. Configure API keys and settings per store.
        </p>
        <div className="border-t border-[#E6E8EC]">
          <ShippingProvidersSettings hideTitle />
        </div>
      </section>

      {/* Google Sheets modal */}
      {showGoogleSheetsModal && (
        <GoogleSheetsSyncModal
          storeId={user.selectedStoreId}
          config={googleSheetsConfig}
          onClose={() => {
            setShowGoogleSheetsModal(false)
            checkGoogleSheetsConnection()
          }}
          onSuccess={checkGoogleSheetsConnection}
        />
      )}
    </div>
  )
}

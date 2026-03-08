import { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Link2, RefreshCw, Unlink, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react'
import { youcanService, YouCanStore } from '../services/youcanService'
import { useStoreColor } from '../hooks/useStoreColor'
import youcanLogo from '../images/youcan-logo.png'

interface YouCanIntegrationProps {
  storeId: string
  onConnectionChange?: () => void
}

export default function YouCanIntegration({ storeId, onConnectionChange }: YouCanIntegrationProps) {
  const { storeColor } = useStoreColor()
  const [searchParams, setSearchParams] = useSearchParams()
  const [connectedStore, setConnectedStore] = useState<YouCanStore | null>(null)
  const [loading, setLoading] = useState(true)
  const [syncing, setSyncing] = useState(false)
  const [connecting, setConnecting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  useEffect(() => {
    loadConnectedStore()
    
    // Check for success/error messages from OAuth callback
    const youcanStatus = searchParams.get('youcan')
    const message = searchParams.get('message')
    
    if (youcanStatus === 'connected') {
      setSuccess('YouCan store connected successfully!')
      // Remove query params from URL
      searchParams.delete('youcan')
      searchParams.delete('message')
      setSearchParams(searchParams, { replace: true })
      // Reload connected store
      loadConnectedStore()
      // Notify parent component of connection change
      if (onConnectionChange) {
        onConnectionChange()
      }
    } else if (youcanStatus === 'error' && message) {
      setError(decodeURIComponent(message))
      // Remove query params from URL
      searchParams.delete('youcan')
      searchParams.delete('message')
      setSearchParams(searchParams, { replace: true })
    }
  }, [storeId, searchParams, setSearchParams])

  const loadConnectedStore = async () => {
    try {
      setLoading(true)
      setError(null)
      const stores = await youcanService.getConnectedStores()
      console.log('Loaded YouCan stores:', stores)
      console.log('Looking for storeId:', storeId)
      const store = stores.find(s => s.storeId === storeId)
      console.log('Found connected store:', store)
      setConnectedStore(store || null)
    } catch (err: any) {
      console.error('Error loading YouCan store:', err)
      setError('Failed to load YouCan integration status: ' + (err.response?.data?.error || err.message))
    } finally {
      setLoading(false)
    }
  }

  const handleConnect = async () => {
    try {
      setConnecting(true)
      setError(null)
      setSuccess(null)
      
      const response = await youcanService.connectStore(storeId)
      
      // Redirect to YouCan authorization page
      window.location.href = response.authorizationUrl
    } catch (err: any) {
      console.error('Error connecting YouCan store:', err)
      setError(err.response?.data?.error || 'Failed to initiate YouCan connection')
      setConnecting(false)
    }
  }

  const handleSync = async () => {
    if (!connectedStore) return

    try {
      setSyncing(true)
      setError(null)
      setSuccess(null)

      const response = await youcanService.syncOrders(connectedStore.id)
      
      if (response.success) {
        setSuccess(`Successfully synced ${response.syncedCount || 0} orders`)
        // Reload connected store to update lastSyncAt
        await loadConnectedStore()
        // Refresh the page after a short delay to show updated orders
        setTimeout(() => {
          window.location.reload()
        }, 2000)
      } else {
        setError(response.error || 'Failed to sync orders')
      }
    } catch (err: any) {
      console.error('Error syncing orders:', err)
      setError(err.response?.data?.error || 'Failed to sync orders from YouCan')
    } finally {
      setSyncing(false)
    }
  }

  const handleDisconnect = async () => {
    if (!connectedStore) return

    if (!confirm('Are you sure you want to disconnect this YouCan store? You will need to reconnect to sync orders.')) {
      return
    }

    try {
      setError(null)
      setSuccess(null)

      await youcanService.disconnectStore(connectedStore.id)
      setSuccess('YouCan store disconnected successfully')
      setConnectedStore(null)
      // Notify parent component of connection change
      if (onConnectionChange) {
        onConnectionChange()
      }
    } catch (err: any) {
      console.error('Error disconnecting YouCan store:', err)
      setError(err.response?.data?.error || 'Failed to disconnect YouCan store')
    }
  }

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="flex items-center justify-center py-8">
          <Loader2 className="h-6 w-6 animate-spin text-gray-400" />
        </div>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">YouCan Integration</h3>
          <p className="text-sm text-gray-500 mt-1">
            Connect your YouCan store to automatically sync orders
          </p>
        </div>
        {connectedStore && (
          <div className="flex items-center text-green-600">
            <CheckCircle2 className="h-5 w-5 mr-2" />
            <span className="text-sm font-medium">Connected</span>
          </div>
        )}
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg flex items-start">
          <AlertCircle className="h-5 w-5 text-red-600 mr-2 mt-0.5 flex-shrink-0" />
          <p className="text-sm text-red-800">{error}</p>
        </div>
      )}

      {success && (
        <div className="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg flex items-start">
          <CheckCircle2 className="h-5 w-5 text-green-600 mr-2 mt-0.5 flex-shrink-0" />
          <p className="text-sm text-green-800">{success}</p>
        </div>
      )}

      {connectedStore ? (
        <div className="space-y-4">
          <div className="flex items-center space-x-3 mb-4">
            <div className="h-12 w-12 rounded-lg flex items-center justify-center bg-white border border-gray-200 overflow-hidden p-1">
              <img 
                src={youcanLogo} 
                alt="YouCan" 
                className="h-full w-full object-contain"
                onError={(e) => {
                  const target = e.target as HTMLImageElement
                  target.style.display = 'none'
                  const parent = target.parentElement
                  if (parent) {
                    parent.innerHTML = '<div class="h-full w-full flex items-center justify-center text-white font-bold text-lg rounded-lg" style="background-color: #ec4899;">Y</div>'
                  }
                }}
              />
            </div>
            <div>
              <h4 className="text-sm font-semibold text-gray-900">YouCan Store Connected</h4>
              <p className="text-xs text-gray-500">{connectedStore.youcanStoreName}</p>
            </div>
          </div>

          <div className="bg-gray-50 rounded-lg p-4">
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-gray-500">Store Name</p>
                <p className="font-medium text-gray-900">{connectedStore.youcanStoreName}</p>
              </div>
              <div>
                <p className="text-gray-500">Store Domain</p>
                <p className="font-medium text-gray-900">{connectedStore.youcanStoreDomain || 'N/A'}</p>
              </div>
              <div>
                <p className="text-gray-500">Last Sync</p>
                <p className="font-medium text-gray-900">
                  {connectedStore.lastSyncAt
                    ? new Date(connectedStore.lastSyncAt).toLocaleString()
                    : 'Never'}
                </p>
              </div>
              <div>
                <p className="text-gray-500">Status</p>
                <p className="font-medium text-gray-900">
                  {connectedStore.active ? 'Active' : 'Inactive'}
                </p>
              </div>
            </div>
          </div>

          <div className="flex gap-3">
            <button
              onClick={handleDisconnect}
              className="flex items-center justify-center px-4 py-2 border border-gray-300 rounded-lg shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 transition-all"
            >
              <Unlink className="h-4 w-4 mr-2" />
              Disconnect
            </button>
          </div>
          <p className="text-xs text-gray-500 italic">
            Orders will sync automatically when you visit the Orders page
          </p>
        </div>
      ) : (
        <div className="text-center py-6">
          <div className="mx-auto w-16 h-16 rounded-lg flex items-center justify-center mb-4 bg-white border border-gray-200 overflow-hidden p-2">
            <img 
              src={youcanLogo} 
              alt="YouCan" 
              className="h-full w-full object-contain"
              onError={(e) => {
                const target = e.target as HTMLImageElement
                target.style.display = 'none'
                const parent = target.parentElement
                if (parent) {
                  parent.innerHTML = '<div class="h-full w-full flex items-center justify-center text-white font-bold text-lg rounded-lg" style="background-color: #ec4899;">Y</div>'
                }
              }}
            />
          </div>
          <h4 className="text-sm font-medium text-gray-900 mb-2">No YouCan Store Connected</h4>
          <p className="text-sm text-gray-500 mb-6">
            Connect your YouCan store to start syncing orders automatically
          </p>
          <button
            onClick={handleConnect}
            disabled={connecting}
            className="inline-flex items-center px-4 py-2 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed hover:opacity-90 transition-all"
            style={{ backgroundColor: storeColor }}
          >
            {connecting ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Connecting...
              </>
            ) : (
              <>
                <Link2 className="h-4 w-4 mr-2" />
                Connect YouCan Store
              </>
            )}
          </button>
        </div>
      )}
    </div>
  )
}


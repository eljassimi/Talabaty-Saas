import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { orderService, Order } from '../services/orderService'
import { storeService, Store } from '../services/storeService'
import { youcanService } from '../services/youcanService'
import { shippingService, TrackingResponse } from '../services/shippingService'
import { Plus, Package, Search, ChevronDown, Calendar, MoreVertical, ChevronLeft, ChevronRight, Truck, AlertTriangle, RefreshCw, X, MessageCircle, Eye, Bike, Upload, CheckCircle, Clock, XCircle } from 'lucide-react'
import CreateOrderModal from '../components/CreateOrderModal'
import UpdateOrderStatusModal from '../components/UpdateOrderStatusModal'
import { useStoreColor } from '../hooks/useStoreColor'
import { cityExistsInDeliveryPlatform } from '../utils/deliveryCities'
import TalabatyLogoSpinner from '../components/TalabatyLogoSpinner'

// Helper to convert hex to RGB
function hexToRgb(hex: string) {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : { r: 2, g: 132, b: 199 }
}

// Get initials for avatar
function getInitials(name: string): string {
  return name
    .split(' ')
    .map(n => n[0])
    .join('')
    .toUpperCase()
    .substring(0, 2)
}

// Pagination component
function Pagination({ currentPage, totalPages, onPageChange, storeColor }: { currentPage: number; totalPages: number; onPageChange: (page: number) => void; storeColor: string }) {
  const pages = []
  const maxVisible = 5
  
  let startPage = Math.max(1, currentPage - Math.floor(maxVisible / 2))
  let endPage = Math.min(totalPages, startPage + maxVisible - 1)
  
  if (endPage - startPage < maxVisible - 1) {
    startPage = Math.max(1, endPage - maxVisible + 1)
  }
  
  for (let i = startPage; i <= endPage; i++) {
    pages.push(i)
  }
  
  return (
    <div className="flex items-center justify-between px-6 py-4 border-t border-gray-200 bg-white">
      <div className="flex items-center space-x-2">
        <button
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 1}
          className="px-3 py-1.5 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
        {startPage > 1 && (
          <>
            <button
              onClick={() => onPageChange(1)}
              className="px-3 py-1.5 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
            >
              1
            </button>
            {startPage > 2 && <span className="px-2 text-gray-500">...</span>}
          </>
        )}
        {pages.map((page) => (
          <button
            key={page}
            onClick={() => onPageChange(page)}
            className={`px-3 py-1.5 border rounded-md text-sm font-medium ${
              page === currentPage
                ? 'border-primary-500 bg-primary-50 text-primary-700'
                : 'border-gray-300 text-gray-700 bg-white hover:bg-gray-50'
            }`}
            style={page === currentPage ? { borderColor: storeColor || '#0284c7', backgroundColor: `${storeColor || '#0284c7'}15`, color: storeColor || '#0284c7' } : {}}
          >
            {page}
          </button>
        ))}
        {endPage < totalPages && (
          <>
            {endPage < totalPages - 1 && <span className="px-2 text-gray-500">...</span>}
            <button
              onClick={() => onPageChange(totalPages)}
              className="px-3 py-1.5 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
            >
              {totalPages}
            </button>
          </>
        )}
        <button
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage === totalPages}
          className="px-3 py-1.5 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
      <div className="text-sm text-gray-700">
        Page {currentPage} of {totalPages}
      </div>
    </div>
  )
}

export default function Orders() {
  const navigate = useNavigate()
  const { storeColor } = useStoreColor()
  const [orders, setOrders] = useState<Order[]>([])
  const [stores, setStores] = useState<Store[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedStore, setSelectedStore] = useState<string>('all')
  const [selectedStatus, setSelectedStatus] = useState<string>('all')
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [showStatusModal, setShowStatusModal] = useState(false)
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)
  const [currentPage, setCurrentPage] = useState(1)
  const [itemsPerPage] = useState(10)
  const [actionMenuOpen, setActionMenuOpen] = useState<string | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [syncingYouCan, setSyncingYouCan] = useState(false)
  const [trackingStatuses, setTrackingStatuses] = useState<Record<string, TrackingResponse>>({})
  const [loadingTracking, setLoadingTracking] = useState<Record<string, boolean>>({})

  useEffect(() => {
    loadStores()
    // Sync YouCan stores - backend handles duplicate prevention
    syncYouCanStores()
  }, [])

  useEffect(() => {
    // Load orders when stores are available or a specific store is selected
    if (stores.length > 0 || selectedStore !== 'all') {
      // If we're syncing, wait a bit for sync to complete before loading orders
      if (syncingYouCan) {
        const timer = setTimeout(() => {
          loadOrders()
        }, 1500) // Wait 1.5 seconds for sync to complete
        return () => clearTimeout(timer)
      } else {
        loadOrders()
      }
      setCurrentPage(1) // Reset to first page when filters change
    }
  }, [selectedStore, selectedStatus, stores.length, syncingYouCan])

  // Close action menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (actionMenuOpen && !(event.target as HTMLElement).closest('.action-menu-container')) {
        setActionMenuOpen(null)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [actionMenuOpen])

  const loadStores = async () => {
    try {
      const data = await storeService.getStores()
      setStores(data)
    } catch (error) {
      // Silently handle error
    }
  }

  const syncYouCanStores = async () => {
    try {
      setSyncingYouCan(true)
      const connectedStores = await youcanService.getConnectedStores()
      
      if (connectedStores.length === 0) {
        setSyncingYouCan(false)
        return
      }
      
      // Sync all connected YouCan stores in parallel
      // Backend handles duplicate prevention, so we can sync on every page load
      // Only throttle if last sync was less than 3 seconds ago (prevent rapid-fire refreshes)
      const lastSyncTime = localStorage.getItem('youcan_last_sync_time')
      const now = Date.now()
      const threeSeconds = 3 * 1000
      
      if (lastSyncTime && (now - parseInt(lastSyncTime)) < threeSeconds) {
        // Very recently synced (within 3 seconds), skip to prevent excessive API calls
        // But still load existing orders
        setSyncingYouCan(false)
        // Trigger order load even if sync is skipped
        setTimeout(() => {
          if (stores.length > 0 || selectedStore !== 'all') {
            loadOrders()
          }
        }, 100)
        return
      }
      
      await Promise.allSettled(
        connectedStores.map(async (youcanStore) => {
          try {
            await youcanService.syncOrders(youcanStore.id)
          } catch (error) {
            // Don't throw - continue with other stores
          }
        })
      )
      
      // Update last sync time
      localStorage.setItem('youcan_last_sync_time', now.toString())
      
      // Reload orders after sync completes
      if (stores.length > 0 || selectedStore !== 'all') {
        setTimeout(() => {
          loadOrders()
        }, 500)
      }
    } catch (error) {
      // Don't show error to user - this is a background sync
    } finally {
      setSyncingYouCan(false)
    }
  }

  const loadOrders = async () => {
    setLoading(true)
    try {
      let ordersData: Order[] = []

      if (selectedStore === 'all') {
        if (stores.length === 0) {
          setLoading(false)
          return
        }
        const errors: string[] = []
        const allOrders = await Promise.all(
          stores.map((store) =>
            orderService.getOrdersByStore(store.id)
              .then(orders => orders)
              .catch((err) => {
                const errorMessage = err.response?.data?.error || 
                                  err.response?.data?.message || 
                                  err.message || 
                                  'Failed to load orders'
                
                // Collect error messages
                if (err.response?.status === 403 || errorMessage.includes('does not belong')) {
                  errors.push(`Store "${store.name}": Access denied. This store may not belong to your account.`)
                } else {
                  errors.push(`Store "${store.name}": ${errorMessage}`)
                }
                
                // Return empty array to continue loading other stores
                return []
              })
          )
        )
        ordersData = allOrders.flat()
        
        // If there were errors, show them to the user
        if (errors.length > 0) {
          setLoadError(errors.join(' '))
        }
      } else {
        if (selectedStatus === 'all') {
          ordersData = await orderService.getOrdersByStore(selectedStore)
        } else {
          ordersData = await orderService.getOrdersByStoreAndStatus(
            selectedStore,
            selectedStatus as Order['status']
          )
        }
      }

      setOrders(ordersData)
      setLoadError(null) // Clear any previous errors
      
      // Debug: Log all orders and their tracking numbers
      const ordersWithTracking = ordersData.filter(o => o.ozonTrackingNumber && o.ozonTrackingNumber.trim() !== '')
      console.log(`Total orders loaded: ${ordersData.length}, Orders with tracking numbers: ${ordersWithTracking.length}`)
      if (ordersWithTracking.length > 0) {
        console.log('Orders with tracking numbers:', ordersWithTracking.map(o => ({ id: o.id, trackingNumber: o.ozonTrackingNumber })))
      }
      
      // Load tracking statuses for orders with tracking numbers
      loadTrackingStatuses(ordersData)
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 
                          error.response?.data?.error || 
                          error.message || 
                          'Failed to load orders'
      // Show error to user
      if (error.response?.status === 403 || errorMessage.includes('does not belong')) {
        setLoadError('Store access denied. This store may not belong to your account. Please check the store selection.')
      } else {
        setLoadError(errorMessage)
      }
      setOrders([]) // Set empty array on error to avoid showing stale data
    } finally {
      setLoading(false)
    }
  }

  const loadTrackingStatuses = async (ordersToLoad: Order[]) => {
    // Filter all orders with tracking numbers
    const ordersWithTracking = ordersToLoad.filter(o => o.ozonTrackingNumber && o.ozonTrackingNumber.trim() !== '')
    
    if (ordersWithTracking.length === 0) {
      return
    }
    
    console.log(`Loading tracking statuses for ${ordersWithTracking.length} orders with tracking numbers`)
    console.log(`Orders to process:`, ordersWithTracking.map(o => ({ id: o.id, trackingNumber: o.ozonTrackingNumber })))
    
    // Load tracking statuses for ALL orders with tracking numbers in parallel
    const trackingPromises = ordersWithTracking.map(async (order) => {
      if (!order.ozonTrackingNumber || order.ozonTrackingNumber.trim() === '') {
        return
      }
      
      try {
        setLoadingTracking(prev => ({ ...prev, [order.id]: true }))
        console.log(`Fetching status for tracking number: ${order.ozonTrackingNumber} (order: ${order.id})`)
        const status = await shippingService.trackParcel(order.ozonTrackingNumber!)
        console.log(`Tracking status for ${order.ozonTrackingNumber}:`, status)
        setTrackingStatuses(prev => ({ ...prev, [order.id]: status }))
      } catch (error: any) {
        const errorMessage = error.message || error.response?.data?.error || 'Failed to load tracking status'
        console.error(`Error loading tracking for ${order.ozonTrackingNumber}:`, errorMessage)
        // Set error status so user knows something went wrong
        setTrackingStatuses(prev => ({ 
          ...prev, 
          [order.id]: { 
            STATUS: 'ERROR', 
            MESSAGE: errorMessage 
          } as any 
        }))
      } finally {
        setLoadingTracking(prev => ({ ...prev, [order.id]: false }))
      }
    })
    
    await Promise.all(trackingPromises)
    console.log(`Finished loading tracking statuses for ${ordersWithTracking.length} orders`)
  }

  const refreshTrackingStatus = async (orderId: string, trackingNumber: string) => {
    try {
      setLoadingTracking(prev => ({ ...prev, [orderId]: true }))
      const status = await shippingService.trackParcel(trackingNumber)
      console.log(`Tracking status for ${trackingNumber}:`, status)
      setTrackingStatuses(prev => ({ ...prev, [orderId]: status }))
    } catch (error: any) {
      const errorMessage = error.message || error.response?.data?.error || 'Failed to load tracking status'
      console.error(`Error loading tracking for ${trackingNumber}:`, errorMessage)
    } finally {
      setLoadingTracking(prev => ({ ...prev, [orderId]: false }))
    }
  }

  const getTrackingStatusBadge = (status: TrackingResponse | undefined, loading: boolean) => {
    if (loading) {
      return (
        <div className="flex items-center space-x-1.5">
          <RefreshCw className="h-3.5 w-3.5 text-gray-400 animate-spin" />
          <span className="text-xs text-gray-500">Loading...</span>
        </div>
      )
    }
    
    if (!status) {
      return <span className="text-xs text-gray-400">-</span>
    }

    // Try to extract status from various possible fields in the API response
    // Ozon Express API returns the actual delivery status in TRACKING.LAST_TRACKING.STATUT
    let statusText: string | undefined
    
    // First, check TRACKING.LAST_TRACKING.STATUT (this is the actual delivery status)
    if (typeof status === 'object' && status.TRACKING) {
      const tracking = status.TRACKING as any
      if (tracking.LAST_TRACKING && tracking.LAST_TRACKING.STATUT) {
        statusText = tracking.LAST_TRACKING.STATUT
      } else {
        // Fallback to other fields in TRACKING object
        statusText = tracking.STATUS || tracking.MESSAGE || tracking.RESULT
      }
    }
    
    // If not found in TRACKING, check top level
    if (!statusText) {
      statusText = (status as any).STATUS || (status as any).RESULT || (status as any).MESSAGE
    }
    
    // If still not found, check other nested objects
    if (!statusText && typeof status === 'object') {
      for (const key in status) {
        if (key && key !== 'TRACKING' && typeof status[key] === 'object' && status[key] !== null) {
          const nested = status[key] as any
          if (nested.STATUS || nested.RESULT || nested.MESSAGE) {
            statusText = nested.STATUS || nested.RESULT || nested.MESSAGE
            break
          }
        }
      }
    }
    
    if (!statusText) {
      return <span className="text-xs text-gray-400">-</span>
    }
    
    return (
      <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-gray-100 text-gray-800">
        {statusText}
      </span>
    )
  }

  const filteredOrders = orders.filter((order) => {
    const query = searchQuery.toLowerCase()
    return (
      order.customerName?.toLowerCase().includes(query) ||
      order.customerPhone?.toLowerCase().includes(query) ||
      order.productName?.toLowerCase().includes(query) ||
      order.city?.toLowerCase().includes(query) ||
      order.destinationAddress?.toLowerCase().includes(query)
    )
  })

  // Pagination logic
  const totalPages = Math.ceil(filteredOrders.length / itemsPerPage)
  const startIndex = (currentPage - 1) * itemsPerPage
  const endIndex = startIndex + itemsPerPage
  const paginatedOrders = filteredOrders.slice(startIndex, endIndex)

  const getStatusColor = (status: Order['status']) => {
    switch (status) {
      case 'CONFIRMED':
        return 'bg-green-100 text-green-800'
      case 'CONCLED':
        return 'bg-red-100 text-red-800'
      case 'APPEL_1':
      case 'APPEL_2':
        return 'bg-orange-100 text-orange-800'
      case 'ENCOURS':
        return 'bg-blue-100 text-blue-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  const getStatusLabel = (status: Order['status']) => {
    const labels: Record<Order['status'], string> = {
      'ENCOURS': 'En cours',
      'CONFIRMED': 'Confirmed',
      'CONCLED': 'Cancelled',
      'APPEL_1': 'Appel 1',
      'APPEL_2': 'Appel 2'
    }
    return labels[status] || status
  }

  const getStatusDot = (status: Order['status']) => {
    switch (status) {
      case 'CONFIRMED':
        return 'bg-green-500'
      case 'CONCLED':
        return 'bg-red-500'
      case 'APPEL_1':
      case 'APPEL_2':
        return 'bg-orange-500'
      case 'ENCOURS':
        return 'bg-blue-500'
      default:
        return 'bg-gray-500'
    }
  }


  if (loading && orders.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[320px] gap-4">
        <TalabatyLogoSpinner spinning size={88} />
        <p className="text-sm text-[#6B7280]">Loading orders...</p>
      </div>
    )
  }

  // Calculate statistics
  const stats = {
    total: filteredOrders.length,
    confirmed: filteredOrders.filter(o => o.status === 'CONFIRMED').length,
    pending: filteredOrders.filter(o => o.status === 'ENCOURS' || o.status === 'APPEL_1' || o.status === 'APPEL_2').length,
    canceled: filteredOrders.filter(o => o.status === 'CONCLED').length,
  }

  // Format date helper
  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    const now = new Date()
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const orderDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())
    
    if (orderDate.getTime() === today.getTime()) {
      return `Today at ${date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false })}`
    }
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' })
  }

  // Get confirmation status label
  const getConfirmationStatus = (status: Order['status']) => {
    switch (status) {
      case 'CONFIRMED':
        return { label: 'Confirmed', color: 'text-green-600', dot: 'bg-green-500' }
      case 'CONCLED':
        return { label: 'Canceled', color: 'text-red-600', dot: 'bg-red-500' }
      case 'ENCOURS':
        return { label: 'New order', color: 'text-blue-600', dot: 'bg-blue-500' }
      case 'APPEL_1':
      case 'APPEL_2':
        return { label: 'Call', color: 'text-orange-600', dot: 'bg-orange-500' }
      default:
        return { label: 'New order', color: 'text-blue-600', dot: 'bg-blue-500' }
    }
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-4xl font-bold text-gray-900">Order overview</h1>
          <p className="mt-3 text-gray-600">Manage all your orders.</p>
        </div>
            <div className="flex items-center space-x-3">
              <button
                className="inline-flex items-center px-5 py-3 border border-gray-300 shadow-sm text-sm font-medium rounded-2xl text-gray-700 bg-white transition-all hover:bg-gray-50"
                title="Export orders"
              >
                <Upload className="h-5 w-5 mr-2" />
                Export
              </button>
              <button
                onClick={() => setShowCreateModal(true)}
                className="inline-flex items-center px-5 py-3 border border-transparent shadow-sm text-sm font-medium rounded-2xl text-white transition-all hover:shadow-lg"
                style={{ backgroundColor: storeColor }}
                onMouseEnter={(e) => {
                  const rgb = hexToRgb(storeColor)
                  e.currentTarget.style.backgroundColor = `rgb(${Math.max(0, rgb.r - 20)}, ${Math.max(0, rgb.g - 20)}, ${Math.max(0, rgb.b - 20)})`
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = storeColor
                }}
              >
                <Plus className="h-5 w-5 mr-2" />
                New Order
              </button>
            </div>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 mb-3">Total Orders</p>
              <p className="text-3xl font-bold text-gray-900">{stats.total}</p>
            </div>
            <div className="h-14 w-14 rounded-2xl bg-blue-50 flex items-center justify-center">
              <Package className="h-7 w-7 text-blue-600" />
            </div>
          </div>
        </div>
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 mb-3">Confirmed</p>
              <p className="text-3xl font-bold text-gray-900">{stats.confirmed}</p>
            </div>
            <div className="h-14 w-14 rounded-2xl bg-green-50 flex items-center justify-center">
              <CheckCircle className="h-7 w-7 text-green-600" />
            </div>
          </div>
        </div>
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 mb-3">Pending</p>
              <p className="text-3xl font-bold text-gray-900">{stats.pending}</p>
            </div>
            <div className="h-14 w-14 rounded-2xl bg-orange-50 flex items-center justify-center">
              <Clock className="h-7 w-7 text-orange-600" />
            </div>
          </div>
        </div>
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 mb-3">Canceled</p>
              <p className="text-sm font-medium text-gray-900 mb-1">
                Shipping: {filteredOrders.filter(o => o.status === 'CONCLED' && o.ozonTrackingNumber).length}
              </p>
              <p className="text-sm font-medium text-gray-900">
                Confirmation: {stats.canceled}
              </p>
            </div>
            <div className="h-14 w-14 rounded-2xl bg-red-50 flex items-center justify-center">
              <XCircle className="h-7 w-7 text-red-600" />
            </div>
          </div>
        </div>
      </div>

      {/* Load Error Notification */}
      {loadError && (
        <div className="p-4 rounded-lg flex items-center justify-between bg-red-50 border border-red-200 text-red-700">
          <div className="flex items-center">
            <AlertTriangle className="h-5 w-5 mr-2" />
            <p className="text-sm font-medium">{loadError}</p>
          </div>
          <button onClick={() => setLoadError(null)} className="text-red-400 hover:text-red-600">
            <X className="h-5 w-5" />
          </button>
        </div>
      )}

      {/* Search and Filters */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between space-y-4 md:space-y-0 md:space-x-4">
          {/* Search */}
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search By Order Id, Product name, Customer name Or phone"
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value)
                setCurrentPage(1)
              }}
              className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:border-transparent transition-all"
              style={{ 
                '--tw-ring-color': storeColor,
              } as React.CSSProperties & { '--tw-ring-color': string }}
              onFocus={(e) => {
                e.currentTarget.style.borderColor = storeColor
                e.currentTarget.style.boxShadow = `0 0 0 2px ${storeColor}40`
              }}
              onBlur={(e) => {
                e.currentTarget.style.borderColor = ''
                e.currentTarget.style.boxShadow = ''
              }}
            />
          </div>

          {/* Date Filter */}
          <div className="flex items-center space-x-2 border border-gray-300 rounded-lg px-3 py-2.5">
            <Calendar className="h-5 w-5 text-gray-400" />
            <input
              type="date"
              className="border-none outline-none text-sm"
            />
            <span className="text-gray-400">-</span>
            <input
              type="date"
              className="border-none outline-none text-sm"
            />
          </div>

          {/* Filters */}
          <div className="flex items-center space-x-3">
            <div className="relative">
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="appearance-none bg-white border border-gray-300 rounded-lg px-4 py-2.5 pr-8 text-sm transition-all"
              >
                <option value="all">Confirmation</option>
                <option value="ENCOURS">New order</option>
                <option value="CONFIRMED">Confirmed</option>
                <option value="CONCLED">Canceled</option>
                <option value="APPEL_1">Appel 1</option>
                <option value="APPEL_2">Appel 2</option>
              </select>
              <ChevronDown className="absolute right-2 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
            <div className="relative">
              <select
                className="appearance-none bg-white border border-gray-300 rounded-lg px-4 py-2.5 pr-8 text-sm transition-all"
              >
                <option>Shipping</option>
              </select>
              <ChevronDown className="absolute right-2 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
            <div className="relative">
              <select
                className="appearance-none bg-white border border-gray-300 rounded-lg px-4 py-2.5 pr-8 text-sm transition-all"
              >
                <option>Date</option>
              </select>
              <ChevronDown className="absolute right-2 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
            <button className="px-4 py-2.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50">
              Advanced filter
            </button>
            <button className="px-4 py-2.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50">
              Reset filters
            </button>
          </div>
        </div>
      </div>

      {/* Orders Table */}
      {filteredOrders.length === 0 ? (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 px-6 py-16 text-center">
          <div className="mx-auto w-20 h-20 bg-gray-50 rounded-2xl flex items-center justify-center mb-6">
            <Package className="h-10 w-10 text-gray-400" />
          </div>
          <h3 className="text-base font-medium text-gray-900 mb-2">No orders found</h3>
          <p className="text-sm text-gray-500">
            {searchQuery ? 'Try adjusting your search criteria.' : 'Get started by creating a new order.'}
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50/50">
                <tr>
                  <th className="px-8 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    ID
                  </th>
                  <th className="px-8 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Date
                  </th>
                  <th className="px-8 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Product
                  </th>
                  <th className="px-8 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Customer
                  </th>
                  <th className="px-8 py-4 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {/* WhatsApp */}
                  </th>
                  <th className="px-8 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    City
                  </th>
                  <th className="px-8 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Shipping
                  </th>
                  <th className="px-8 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Confirmation
                  </th>
                  <th className="px-8 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Total
                  </th>
                  <th className="px-8 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Handled By
                  </th>
                  <th className="px-8 py-4 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Delivery
                  </th>
                  <th className="px-8 py-4 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Source
                  </th>
                  <th className="px-8 py-4 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {/* Actions */}
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-100">
                {paginatedOrders.map((order) => {
                  const confirmationStatus = getConfirmationStatus(order.status)
                  return (
                    <tr key={order.id} className="hover:bg-gray-50/50 transition-colors">
                      <td className="px-8 py-5 whitespace-nowrap">
                        <div className="text-sm font-medium text-gray-900">#{order.id.substring(0, 8)}</div>
                      </td>
                      <td className="px-8 py-5 whitespace-nowrap">
                        <div className="text-sm text-gray-900">{formatDate(order.createdAt)}</div>
                      </td>
                      <td className="px-8 py-5">
                        <div className="text-sm text-gray-900 max-w-xs truncate" title={order.productName || 'N/A'}>
                          {order.productName || 'N/A'} ...
                        </div>
                      </td>
                      <td className="px-8 py-5">
                        <div className="text-sm text-gray-900 max-w-xs truncate" title={order.customerName}>
                          {order.customerName.substring(0, 8)} ...
                        </div>
                      </td>
                      <td className="px-8 py-5 whitespace-nowrap text-center">
                        <button
                          onClick={() => {
                            const phoneNumber = order.customerPhone.replace(/\D/g, '')
                            window.open(`https://wa.me/${phoneNumber}`, '_blank')
                          }}
                          className="inline-flex items-center justify-center p-2.5 text-green-600 hover:bg-green-50 rounded-xl transition-colors"
                          title="Open WhatsApp"
                        >
                          <MessageCircle className="h-5 w-5" />
                        </button>
                      </td>
                      <td className="px-8 py-5 whitespace-nowrap">
                        <div className="flex items-center space-x-2">
                          <div className="text-sm text-gray-900">{order.city || 'N/A'}</div>
                          {(!order.city || order.city === 'N/A' || !cityExistsInDeliveryPlatform(order.city)) && (
                            <div className="relative">
                              <AlertTriangle className="h-5 w-5 text-red-500" />
                              <span className="absolute -top-1 -right-1 h-2 w-2 bg-red-500 rounded-full animate-ping"></span>
                              <span className="absolute -top-1 -right-1 h-2 w-2 bg-red-500 rounded-full"></span>
                            </div>
                          )}
                        </div>
                      </td>
                      <td className="px-8 py-5 whitespace-nowrap">
                        <div className="flex items-center space-x-2">
                          {order.ozonTrackingNumber ? (
                            <>
                              <div className="flex-1 min-w-0">
                                <div className="flex items-center space-x-2">
                                  <span className="text-sm text-blue-600 font-mono">{order.ozonTrackingNumber.substring(0, 10)}...</span>
                                  <button
                                    onClick={(e) => {
                                      e.stopPropagation()
                                      refreshTrackingStatus(order.id, order.ozonTrackingNumber!)
                                    }}
                                    className="p-1 hover:bg-gray-100 rounded transition-colors"
                                    title="Refresh tracking status"
                                  >
                                    <RefreshCw className={`h-3.5 w-3.5 text-gray-500 ${loadingTracking[order.id] ? 'animate-spin' : ''}`} />
                                  </button>
                                </div>
                                <div className="mt-1">
                                  {getTrackingStatusBadge(trackingStatuses[order.id], loadingTracking[order.id] || false)}
                                </div>
                              </div>
                            </>
                          ) : (
                            <span className="text-gray-400">-</span>
                          )}
                        </div>
                      </td>
                      <td className="px-8 py-5 whitespace-nowrap">
                        <div className="flex items-center">
                          <span className={`h-2.5 w-2.5 rounded-full mr-2.5 ${confirmationStatus.dot}`}></span>
                          <span className={`text-sm font-medium ${confirmationStatus.color}`}>
                            {confirmationStatus.label}
                          </span>
                        </div>
                      </td>
                      <td className="px-8 py-5 whitespace-nowrap">
                        <div className="text-sm font-medium text-gray-900">
                          {order.totalAmount} {order.currency}
                        </div>
                      </td>
                      <td className="px-8 py-5 whitespace-nowrap">
                        <div className="text-sm text-gray-500">
                          {order.assignedToName || 'Not assigned'}
                        </div>
                      </td>
                      <td className="px-8 py-5 whitespace-nowrap">
                        {order.ozonTrackingNumber ? (
                          <div className="flex flex-col items-center space-y-1.5">
                            <Bike className="h-5 w-5 text-gray-400" />
                            <div className="flex justify-center">
                              {getTrackingStatusBadge(trackingStatuses[order.id], loadingTracking[order.id] || false)}
                            </div>
                          </div>
                        ) : (
                          <div className="text-center">
                            <span className="text-gray-400">-</span>
                          </div>
                        )}
                      </td>
                      <td className="px-8 py-5 whitespace-nowrap text-center">
                        <button
                          onClick={() => navigate(`/orders/${order.id}`)}
                          className="inline-flex items-center justify-center p-2.5 text-gray-600 hover:bg-gray-100 rounded-xl transition-colors"
                          title="View order details"
                        >
                          <Eye className="h-5 w-5" />
                        </button>
                      </td>
                      <td className="px-8 py-5 whitespace-nowrap text-center relative action-menu-container">
                        <button
                          onClick={(e) => {
                            e.stopPropagation()
                            setActionMenuOpen(actionMenuOpen === order.id ? null : order.id)
                          }}
                          className="p-2 hover:bg-gray-100 rounded-xl transition-colors"
                        >
                          <MoreVertical className="h-5 w-5 text-gray-400" />
                        </button>
                        {actionMenuOpen === order.id && (
                          <div className="absolute right-0 mt-2 w-48 bg-white rounded-xl shadow-lg z-10 border border-gray-200">
                            <div className="py-1">
                              <button
                                onClick={(e) => {
                                  e.stopPropagation()
                                  navigate(`/orders/${order.id}`)
                                  setActionMenuOpen(null)
                                }}
                                className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                              >
                                View
                              </button>
                              <button
                                onClick={(e) => {
                                  e.stopPropagation()
                                  setSelectedOrder(order)
                                  setShowStatusModal(true)
                                  setActionMenuOpen(null)
                                }}
                                className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                              >
                                Edit Status
                              </button>
                            </div>
                          </div>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
          <div className="flex items-center justify-between px-8 py-5 border-t border-gray-100 bg-white">
            <div className="flex items-center space-x-2">
              <span className="text-sm text-gray-700">Shows</span>
              <select className="border border-gray-300 rounded-md px-2 py-1 text-sm">
                <option>15 rows</option>
                <option>25 rows</option>
                <option>50 rows</option>
              </select>
            </div>
            <div className="flex items-center space-x-2">
              <span className="text-sm text-gray-700">{filteredOrders.length} Items</span>
              {totalPages > 1 && (
                <>
                  <button
                    onClick={() => setCurrentPage(currentPage - 1)}
                    disabled={currentPage === 1}
                    className="p-1 border border-gray-300 rounded-md disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </button>
                  <span className="px-2 text-sm font-medium text-gray-900">{currentPage}</span>
                  <button
                    onClick={() => setCurrentPage(currentPage + 1)}
                    disabled={currentPage === totalPages}
                    className="p-1 border border-gray-300 rounded-md disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}

      {showCreateModal && (
        <CreateOrderModal
          stores={stores}
          onClose={() => setShowCreateModal(false)}
          onSuccess={() => {
            setShowCreateModal(false)
            loadOrders()
          }}
        />
      )}

      {showStatusModal && selectedOrder && (
        <UpdateOrderStatusModal
          order={selectedOrder}
          onClose={() => {
            setShowStatusModal(false)
            setSelectedOrder(null)
          }}
          onSuccess={() => {
            setShowStatusModal(false)
            setSelectedOrder(null)
            loadOrders()
          }}
        />
      )}

    </div>
  )
}

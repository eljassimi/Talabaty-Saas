import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { orderService, Order, SendToShippingRequest } from '../services/orderService'
import { shippingService, TrackingResponse } from '../services/shippingService'
import { Package, User, MapPin, Phone, DollarSign, Edit, Hash, Truck, AlertTriangle, RefreshCw } from 'lucide-react'
import SendToShippingModal from '../components/SendToShippingModal'
import UpdateOrderModal from '../components/UpdateOrderModal'
import { useStoreColor } from '../hooks/useStoreColor'
import { findCityId } from '../utils/cityMapping'
import { cityExistsInDeliveryPlatform } from '../utils/deliveryCities'

// Helper to convert hex to RGB
function hexToRgb(hex: string) {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : { r: 2, g: 132, b: 199 }
}

export default function OrderDetail() {
  const { id } = useParams<{ id: string }>()
  const { storeColor } = useStoreColor()
  const [order, setOrder] = useState<Order | null>(null)
  const [loading, setLoading] = useState(true)
  const [showShippingModal, setShowShippingModal] = useState(false)
  const [showUpdateModal, setShowUpdateModal] = useState(false)
  const [statusUpdateError, setStatusUpdateError] = useState<string | null>(null)
  const [trackingStatus, setTrackingStatus] = useState<TrackingResponse | null>(null)
  const [loadingTracking, setLoadingTracking] = useState(false)
  const { user: currentUser } = useAuth()
  
  // Check if user can update orders (SUPPORT, MANAGER, ACCOUNT_OWNER, PLATFORM_ADMIN)
  const canUpdateOrder = currentUser?.role === 'SUPPORT' || 
                        currentUser?.role === 'MANAGER' || 
                        currentUser?.role === 'ACCOUNT_OWNER' || 
                        currentUser?.role === 'PLATFORM_ADMIN'

  useEffect(() => {
    if (id) {
      loadOrder()
    }
  }, [id])

  useEffect(() => {
    if (order?.ozonTrackingNumber) {
      loadTrackingStatus()
    }
  }, [order?.ozonTrackingNumber])

  const loadOrder = async () => {
    if (!id) return
    try {
      const orderData = await orderService.getOrder(id)
      setOrder(orderData)
    } catch (error) {
      console.error('Error loading order:', error)
    } finally {
      setLoading(false)
    }
  }

  const loadTrackingStatus = async () => {
    if (!order?.ozonTrackingNumber) return
    setLoadingTracking(true)
    try {
      const status = await shippingService.trackParcel(order.ozonTrackingNumber)
      setTrackingStatus(status)
    } catch (error) {
      console.error('Error loading tracking status:', error)
      setTrackingStatus(null)
    } finally {
      setLoadingTracking(false)
    }
  }

  const handleStatusUpdate = async (status: Order['status']) => {
    if (!id || !order) return
    setStatusUpdateError(null)
    
    try {
      const previousStatus = order.status
      await orderService.updateOrderStatus(id, { status })
      
      // If status changed to CONFIRMED, automatically send to shipping (silently)
      if (status === 'CONFIRMED' && previousStatus !== 'CONFIRMED') {
        try {
          // First check if city exists in delivery platform or is N/A
          if (!order.city || order.city === 'N/A' || !cityExistsInDeliveryPlatform(order.city)) {
            // City not found or missing - silently skip shipping, warning icon will show
            console.log(`City "${order.city || 'N/A'}" not found in delivery platform, skipping auto-ship`)
            loadOrder()
            return
          }
          
          // Check if city exists in mapping for cityId
          const cityId = order.city ? findCityId(order.city) : null
          
          if (cityId) {
            // Auto-send to shipping with default values
            const shippingData: SendToShippingRequest = {
              cityId: cityId.toString(),
              stock: 0, // Pickup
              open: 1, // Open
              fragile: 0, // No
              replace: 0, // No
            }
            
            await orderService.sendOrderToShipping(id, shippingData)
            // Small delay to ensure backend has time to save tracking number
            await new Promise(resolve => setTimeout(resolve, 500))
          }
        } catch (shippingError: any) {
          // Silently fail - warning icon will indicate issues
          console.error('Failed to automatically send to shipping:', shippingError)
        }
      }
      
      loadOrder()
    } catch (error: any) {
      console.error('Error updating status:', error)
      setStatusUpdateError(error.response?.data?.message || error.message || 'Failed to update order status')
    }
  }

  const getStatusColor = (status: Order['status']) => {
    switch (status) {
      case 'CONFIRMED':
        return 'bg-green-100 text-green-800 border-green-200'
      case 'CONCLED':
        return 'bg-red-100 text-red-800 border-red-200'
      case 'APPEL_1':
      case 'APPEL_2':
        return 'bg-orange-100 text-orange-800 border-orange-200'
      case 'ENCOURS':
        return 'bg-blue-100 text-blue-800 border-blue-200'
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200'
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

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2" style={{ borderColor: storeColor }}></div>
      </div>
    )
  }

  if (!order) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">Order not found</p>
        <Link 
          to="/orders" 
          className="mt-4 inline-flex items-center text-sm transition-colors"
          style={{ color: storeColor }}
        >
          Back to Orders
        </Link>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Order Details</h1>
          {order.externalOrderId && (
            <p className="text-sm text-gray-500 mt-1">External ID: {order.externalOrderId}</p>
          )}
        </div>
        <div className="flex items-center space-x-3">
          <span className={`inline-flex px-4 py-2 text-sm font-semibold rounded-full border ${getStatusColor(order.status)}`}>
            {getStatusLabel(order.status)}
          </span>
          {canUpdateOrder && (
            <button
              onClick={() => setShowUpdateModal(true)}
              className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-lg text-white transition-all"
              style={{ backgroundColor: storeColor }}
              onMouseEnter={(e) => {
                const rgb = hexToRgb(storeColor)
                e.currentTarget.style.backgroundColor = `rgb(${Math.max(0, rgb.r - 20)}, ${Math.max(0, rgb.g - 20)}, ${Math.max(0, rgb.b - 20)})`
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = storeColor
              }}
            >
              <Edit className="h-4 w-4 mr-2" />
              Edit Order
            </button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Content */}
        <div className="lg:col-span-2 space-y-6">
          {/* Customer Information */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
            <div 
              className="px-6 py-4 border-b border-gray-200"
              style={{ borderBottomColor: `${storeColor}30` }}
            >
              <h2 className="text-lg font-semibold text-gray-900 flex items-center">
                <User className="h-5 w-5 mr-2" style={{ color: storeColor }} />
                Customer Information
              </h2>
            </div>
            <div className="px-6 py-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-1">
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">Full Name</label>
                  <p className="text-sm font-medium text-gray-900">{order.customerName}</p>
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wide flex items-center">
                    <Phone className="h-3 w-3 mr-1" />
                    Phone Number
                  </label>
                  <p className="text-sm font-medium text-gray-900">{order.customerPhone}</p>
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wide flex items-center">
                    <MapPin className="h-3 w-3 mr-1" />
                    City
                  </label>
                  <div className="flex items-center space-x-2">
                    <p className="text-sm font-medium text-gray-900">{order.city || 'N/A'}</p>
                    {(!order.city || order.city === 'N/A' || !cityExistsInDeliveryPlatform(order.city)) && (
                      <div className="group relative">
                        <div className="relative">
                          <AlertTriangle className="h-4 w-4 text-red-500 cursor-help" />
                          <span className="absolute -top-0.5 -right-0.5 h-1.5 w-1.5 bg-red-500 rounded-full animate-ping"></span>
                          <span className="absolute -top-0.5 -right-0.5 h-1.5 w-1.5 bg-red-500 rounded-full"></span>
                        </div>
                        <div className="absolute left-0 bottom-full mb-2 hidden group-hover:block z-50 w-80 p-4 bg-red-50 border-2 border-red-300 rounded-xl shadow-lg text-xs text-red-800 break-words">
                          <p className="font-bold mb-2 text-red-900">⚠️ City Not Found in Delivery Platform</p>
                          {!order.city || order.city === 'N/A' ? (
                            <p className="mb-2">City is missing or set to "N/A". This order cannot be sent to the delivery platform.</p>
                          ) : (
                            <p className="mb-2">City "<span className="font-semibold">{order.city}</span>" is not recognized in the delivery platform.</p>
                          )}
                          <p className="text-red-700 font-medium mb-1">Action Required:</p>
                          <p className="text-red-800 break-words">Click "Edit Order" to update the city name to match a valid delivery city.</p>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
                <div className="space-y-1 md:col-span-2">
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wide flex items-center">
                    <MapPin className="h-3 w-3 mr-1" />
                    Delivery Address
                  </label>
                  <p className="text-sm text-gray-900 bg-gray-50 px-4 py-3 rounded-lg border border-gray-200">
                    {order.destinationAddress}
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Product Information */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
            <div 
              className="px-6 py-4 border-b border-gray-200"
              style={{ borderBottomColor: `${storeColor}30` }}
            >
              <h2 className="text-lg font-semibold text-gray-900 flex items-center">
                <Package className="h-5 w-5 mr-2" style={{ color: storeColor }} />
                Product Information
              </h2>
            </div>
            <div className="px-6 py-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {order.productName && (
                  <div className="space-y-1">
                    <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">Product Name</label>
                    <p className="text-sm font-medium text-gray-900">{order.productName}</p>
                  </div>
                )}
                {order.productId && (
                  <div className="space-y-1">
                    <label className="text-xs font-medium text-gray-500 uppercase tracking-wide flex items-center">
                      <Hash className="h-3 w-3 mr-1" />
                      Product ID
                    </label>
                    <p className="text-sm font-medium text-gray-900 font-mono bg-gray-50 px-4 py-2 rounded-lg border border-gray-200">
                      {order.productId}
                    </p>
                  </div>
                )}
                <div className="space-y-1 md:col-span-2">
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wide flex items-center">
                    <DollarSign className="h-3 w-3 mr-1" />
                    Total Amount
                  </label>
                  <div 
                    className="text-3xl font-bold px-4 py-3 rounded-lg"
                    style={{ 
                      color: storeColor,
                      backgroundColor: `${storeColor}10`
                    }}
                  >
                    {order.totalAmount} {order.currency}
                  </div>
                </div>
                {order.ozonTrackingNumber && (
                  <div className="space-y-1 md:col-span-2">
                    <label className="text-xs font-medium text-gray-500 uppercase tracking-wide flex items-center justify-between">
                      <div className="flex items-center">
                        <Truck className="h-3 w-3 mr-1" />
                        Tracking Number
                      </div>
                      <button
                        onClick={loadTrackingStatus}
                        disabled={loadingTracking}
                        className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-50"
                        title="Refresh tracking status"
                      >
                        <RefreshCw className={`h-3.5 w-3.5 text-gray-500 ${loadingTracking ? 'animate-spin' : ''}`} />
                      </button>
                    </label>
                    <div className="flex items-center space-x-3 bg-gray-50 px-4 py-3 rounded-lg border border-gray-200">
                      <img 
                        src="https://ozoneexpress.ma/wp/wp-content/uploads/2025/07/Untitled-design-38.png"
                        alt="Ozon Express"
                        className="h-8 w-8 object-contain"
                        onError={(e) => {
                          e.currentTarget.style.display = 'none'
                        }}
                      />
                      <div className="flex-1">
                        <p className="text-sm font-medium text-gray-900 font-mono mb-1">
                          {order.ozonTrackingNumber}
                        </p>
                        {loadingTracking ? (
                          <div className="flex items-center space-x-1.5 text-xs text-gray-500">
                            <RefreshCw className="h-3 w-3 animate-spin" />
                            <span>Loading status...</span>
                          </div>
                        ) : trackingStatus ? (
                          <div className="flex items-center space-x-2 mt-1">
                            {(() => {
                              // Display the raw status from the delivery platform
                              // Ozon Express API returns the actual delivery status in TRACKING.LAST_TRACKING.STATUT
                              let statusText: string | undefined
                              
                              if (trackingStatus.TRACKING) {
                                const tracking = trackingStatus.TRACKING as any
                                // Prioritize LAST_TRACKING.STATUT (actual delivery status)
                                if (tracking.LAST_TRACKING && tracking.LAST_TRACKING.STATUT) {
                                  statusText = tracking.LAST_TRACKING.STATUT
                                } else {
                                  // Fallback to other fields in TRACKING object
                                  statusText = tracking.STATUS || tracking.MESSAGE || tracking.RESULT
                                }
                              }
                              
                              // If not found in TRACKING, check top level
                              if (!statusText) {
                                statusText = trackingStatus.STATUS || trackingStatus.RESULT || trackingStatus.MESSAGE
                              }
                              
                              if (!statusText) {
                                statusText = 'Unknown'
                              }
                              
                              return (
                                <span className="text-xs font-medium px-2 py-1 rounded-full bg-gray-100 text-gray-800">
                                  {statusText}
                                </span>
                              )
                            })()}
                          </div>
                        ) : (
                          <span className="text-xs text-gray-400">Click refresh to check status</span>
                        )}
                      </div>
                    </div>
                  </div>
                )}
                {order.assignedToName && (
                  <div className="space-y-1 md:col-span-2">
                    <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">Assigned To</label>
                    <p className="text-sm font-medium text-gray-900 bg-gray-50 px-4 py-2 rounded-lg border border-gray-200">
                      {order.assignedToName}
                    </p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Sidebar - Actions */}
        <div className="space-y-6">
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
            <div 
              className="px-6 py-4 border-b border-gray-200"
              style={{ borderBottomColor: `${storeColor}30` }}
            >
              <h2 className="text-lg font-semibold text-gray-900">Quick Actions</h2>
            </div>
            <div className="px-6 py-4 space-y-3">
              {order.status !== 'CONCLED' && (
                <button
                  onClick={() => setShowShippingModal(true)}
                  className="w-full inline-flex items-center justify-center px-4 py-2.5 border border-transparent shadow-sm text-sm font-medium rounded-lg text-white transition-all"
                  style={{ backgroundColor: storeColor }}
                  onMouseEnter={(e) => {
                    const rgb = hexToRgb(storeColor)
                    e.currentTarget.style.backgroundColor = `rgb(${Math.max(0, rgb.r - 20)}, ${Math.max(0, rgb.g - 20)}, ${Math.max(0, rgb.b - 20)})`
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.backgroundColor = storeColor
                  }}
                >
                  <Truck className="h-4 w-4 mr-2" />
                  Send to Shipping
                </button>
              )}
              {canUpdateOrder && (
                <div className="space-y-2 pt-2 border-t border-gray-200">
                  <label className="block text-sm font-medium text-gray-700">Update Status</label>
                  <select
                    value={order.status}
                    onChange={(e) => handleStatusUpdate(e.target.value as Order['status'])}
                    className="block w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 text-sm transition-all"
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
                  >
                    <option value="ENCOURS">En cours</option>
                    <option value="CONFIRMED">Confirmed</option>
                    <option value="CONCLED">Cancelled</option>
                    <option value="APPEL_1">Appel 1</option>
                    <option value="APPEL_2">Appel 2</option>
                  </select>
                  {statusUpdateError && (
                    <div className="mt-2 p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-800">
                      <div className="flex items-start">
                        <AlertTriangle className="h-4 w-4 mr-2 flex-shrink-0 mt-0.5" />
                        <div>
                          <p className="font-semibold mb-1">Status Update Error</p>
                          <p>{statusUpdateError}</p>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {showShippingModal && id && (
        <SendToShippingModal
          orderId={id}
          order={order}
          onClose={() => setShowShippingModal(false)}
          onSuccess={() => {
            setShowShippingModal(false)
            loadOrder()
          }}
        />
      )}

      {showUpdateModal && order && (
        <UpdateOrderModal
          order={order}
          onClose={() => setShowUpdateModal(false)}
          onSuccess={() => {
            setShowUpdateModal(false)
            loadOrder()
          }}
        />
      )}
    </div>
  )
}

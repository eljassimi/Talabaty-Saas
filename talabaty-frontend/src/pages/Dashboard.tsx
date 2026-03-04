import { useEffect, useState, useRef } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { orderService, Order, OrderStatus } from '../services/orderService'
import { storeService, Store } from '../services/storeService'
import { userService } from '../services/userService'
import { Package, TrendingUp, XCircle, CheckCircle, Plus, Search, ChevronDown, Store as StoreIcon } from 'lucide-react'
import { useStoreColor } from '../hooks/useStoreColor'
import { BRAND_COLORS } from '../constants/brand'

export default function Dashboard() {
  const { user, updateUser } = useAuth()
  const { storeColor } = useStoreColor()
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [stores, setStores] = useState<Store[]>([])
  const [switchingStore, setSwitchingStore] = useState(false)
  const [showStoreDropdown, setShowStoreDropdown] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)
  const [stats, setStats] = useState({
    totalOrders: 0,
    confirmed: 0,
    pending: 0,
    canceled: 0,
  })

  useEffect(() => {
    if (user?.selectedStoreId) {
      loadData()
      loadStores()
    }
  }, [user?.selectedStoreId])

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setShowStoreDropdown(false)
      }
    }

    if (showStoreDropdown) {
      document.addEventListener('mousedown', handleClickOutside)
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [showStoreDropdown])

  const loadStores = async () => {
    try {
      const data = await storeService.getStores()
      setStores(Array.isArray(data) ? data : [])
    } catch (error) {
      console.error('Error loading stores:', error)
    }
  }

  const handleSwitchStore = async (storeId: string) => {
    if (storeId === user?.selectedStoreId || switchingStore) {
      setShowStoreDropdown(false)
      return
    }
    
    setSwitchingStore(true)
    setShowStoreDropdown(false)
    try {
      await userService.updateSelectedStore(storeId)
      updateUser({ selectedStoreId: storeId })
      // Reload data after switching
      await loadData()
    } catch (error) {
      console.error('Error switching store:', error)
      alert('Failed to switch store')
    } finally {
      setSwitchingStore(false)
    }
  }

  const selectedStore = stores.find(s => s.id === user?.selectedStoreId)

  const loadData = async () => {
    if (!user?.selectedStoreId) return

    try {
      const ordersData = await orderService.getOrdersByStore(user.selectedStoreId).catch(() => [])

      setOrders(ordersData)

      const total = ordersData.length
      const confirmed = ordersData.filter((o) => o.status === 'CONFIRMED').length
      const pending = ordersData.filter((o) => ['ENCOURS', 'APPEL_1', 'APPEL_2'].includes(o.status)).length
      const canceled = ordersData.filter((o) => o.status === 'CANCELLED' || o.status === 'CANCELED').length

      setStats({
        totalOrders: total,
        confirmed,
        pending,
        canceled,
      })
    } catch (error) {
      console.error('Error loading dashboard data:', error)
    } finally {
      setLoading(false)
    }
  }

  const getStatusColor = (status: OrderStatus) => {
    switch (status) {
      case 'CONFIRMED':
        return 'bg-green-100 text-green-800'
      case 'CONCLED':
        return 'bg-blue-100 text-blue-800'
      case 'ENCOURS':
      case 'APPEL_1':
      case 'APPEL_2':
        return 'bg-yellow-100 text-yellow-800'
      case 'CANCELLED':
      case 'CANCELED':
        return 'bg-red-100 text-red-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  const getStatusText = (status: OrderStatus) => {
    switch (status) {
      case 'ENCOURS':
        return 'New order'
      case 'CONFIRMED':
        return 'Confirmed'
      case 'CONCLED':
        return 'Delivered'
      case 'APPEL_1':
      case 'APPEL_2':
        return 'No answer'
      case 'CANCELLED':
      case 'CANCELED':
        return 'Canceled'
      default:
        return status
    }
  }

  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    const now = new Date()
    const diffTime = Math.abs(now.getTime() - date.getTime())
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

    if (diffDays === 0) {
      return date.toLocaleDateString('en-US', { weekday: 'short', hour: '2-digit', minute: '2-digit' })
    } else if (diffDays === 1) {
      return 'Yesterday'
    } else if (diffDays < 7) {
      return `${diffDays} days ago`
    } else {
      return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
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
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Orders overview</h1>
          <p className="mt-1 text-gray-600">Manage all your orders</p>
        </div>
        <div className="flex items-center space-x-3">
          {/* Store Switcher */}
          {stores.length > 1 && (
            <div className="relative" ref={dropdownRef}>
              <button
                onClick={() => setShowStoreDropdown(!showStoreDropdown)}
                disabled={switchingStore}
                className="flex items-center space-x-2 bg-white border border-gray-300 rounded-lg px-3 py-2.5 text-sm transition-all disabled:opacity-50 disabled:cursor-not-allowed hover:border-gray-400 focus:outline-none focus:ring-2 focus:ring-offset-2 min-w-[180px]"
                style={{
                  '--tw-ring-color': storeColor,
                } as React.CSSProperties & { '--tw-ring-color': string }}
                onFocus={(e) => {
                  e.currentTarget.style.borderColor = storeColor
                  e.currentTarget.style.boxShadow = `0 0 0 2px ${storeColor}40`
                }}
                onBlur={(e) => {
                  if (!showStoreDropdown) {
                    e.currentTarget.style.borderColor = ''
                    e.currentTarget.style.boxShadow = ''
                  }
                }}
              >
                {selectedStore?.logoUrl ? (
                  <img 
                    src={selectedStore.logoUrl} 
                    alt={selectedStore.name}
                    className="h-5 w-5 rounded object-cover border-2"
                    style={{ borderColor: selectedStore.color || storeColor }}
                  />
                ) : (
                  <div 
                    className="h-5 w-5 rounded flex items-center justify-center flex-shrink-0"
                    style={{ backgroundColor: selectedStore?.color || storeColor }}
                  >
                    <StoreIcon className="h-3 w-3 text-white" />
                  </div>
                )}
                <span className="flex-1 text-left font-medium text-gray-700 truncate">
                  {selectedStore?.name || 'Select Store'}
                </span>
                <ChevronDown className={`h-4 w-4 text-gray-400 transition-transform ${showStoreDropdown ? 'rotate-180' : ''}`} />
              </button>

              {/* Dropdown Menu */}
              {showStoreDropdown && (
                <div className="absolute top-full left-0 right-0 mt-1 bg-white border border-gray-200 rounded-lg shadow-lg z-50 max-h-64 overflow-y-auto">
                  {stores.map((store) => {
                    const isSelected = store.id === user?.selectedStoreId
                    const storeColorValue = store.color || BRAND_COLORS.primary
                    return (
                      <button
                        key={store.id}
                        onClick={() => handleSwitchStore(store.id)}
                        className={`w-full flex items-center space-x-3 px-3 py-2.5 text-left hover:bg-gray-50 transition-colors ${
                          isSelected ? 'bg-gray-50' : ''
                        }`}
                      >
                        {store.logoUrl ? (
                          <img 
                            src={store.logoUrl} 
                            alt={store.name}
                            className="h-8 w-8 rounded object-cover border-2 flex-shrink-0"
                            style={{ borderColor: storeColorValue }}
                          />
                        ) : (
                          <div 
                            className="h-8 w-8 rounded flex items-center justify-center flex-shrink-0"
                            style={{ backgroundColor: storeColorValue }}
                          >
                            <StoreIcon className="h-4 w-4 text-white" />
                          </div>
                        )}
                        <div className="flex-1 min-w-0">
                          <p className={`text-sm font-medium truncate ${isSelected ? 'text-gray-900' : 'text-gray-700'}`}>
                            {store.name}
                          </p>
                          {isSelected && (
                            <p className="text-xs text-gray-500">Current store</p>
                          )}
                        </div>
                        {isSelected && (
                          <div 
                            className="h-2 w-2 rounded-full flex-shrink-0"
                            style={{ backgroundColor: storeColorValue }}
                          />
                        )}
                      </button>
                    )
                  })}
                </div>
              )}
            </div>
          )}
          <Link
            to="/orders"
            className="flex items-center px-4 py-2 text-white rounded-lg hover:opacity-90 transition-all"
            style={{ backgroundColor: storeColor }}
          >
            <Plus className="h-4 w-4 mr-2" />
            New Order
          </Link>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {/* Total Orders */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600 mb-1">Total Orders</p>
              <p className="text-3xl font-bold text-gray-900">{stats.totalOrders}</p>
              <p className="text-sm text-green-600 mt-2 flex items-center">
                <TrendingUp className="h-4 w-4 mr-1" />
                147% vs last 30 days
              </p>
            </div>
            <div className="h-12 w-12 bg-blue-50 rounded-lg flex items-center justify-center">
              <Package className="h-6 w-6" style={{ color: BRAND_COLORS.primary }} />
            </div>
          </div>
        </div>

        {/* Confirmed */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600 mb-1">Confirmed</p>
              <p className="text-3xl font-bold text-gray-900">{stats.confirmed}</p>
              <p className="text-sm text-green-600 mt-2">You saved MAD 327</p>
            </div>
            <div className="h-12 w-12 bg-green-50 rounded-lg flex items-center justify-center">
              <CheckCircle className="h-6 w-6 text-green-600" />
            </div>
          </div>
        </div>

        {/* Pending */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600 mb-1">Pending</p>
              <p className="text-3xl font-bold text-gray-900">{stats.pending}</p>
              <p className="text-sm text-red-600 mt-2 flex items-center">
                <TrendingUp className="h-4 w-4 mr-1 rotate-180" />
                23% vs last 30 days
              </p>
            </div>
            <div className="h-12 w-12 bg-yellow-50 rounded-lg flex items-center justify-center">
              <Package className="h-6 w-6 text-yellow-600" />
            </div>
          </div>
        </div>

        {/* Canceled */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600 mb-1">Canceled</p>
              <p className="text-3xl font-bold text-gray-900">{stats.canceled}</p>
              <p className="text-sm text-green-600 mt-2 flex items-center">
                <TrendingUp className="h-4 w-4 mr-1" />
                10.7% vs last 30 days
              </p>
            </div>
            <div className="h-12 w-12 bg-red-50 rounded-lg flex items-center justify-center">
              <XCircle className="h-6 w-6 text-red-600" />
            </div>
          </div>
        </div>
      </div>

      {/* Orders Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200">
        {/* Table Header with Search */}
        <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
          <div className="flex items-center space-x-4 flex-1">
            <div className="relative flex-1 max-w-md">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
              <input
                type="text"
                placeholder="Search for an order.."
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:border-transparent transition-all"
                style={{
                  '--tw-ring-color': BRAND_COLORS.primary,
                } as React.CSSProperties & { '--tw-ring-color': string }}
                onFocus={(e) => {
                  e.currentTarget.style.borderColor = BRAND_COLORS.primary
                  e.currentTarget.style.boxShadow = `0 0 0 2px ${BRAND_COLORS.primary}40`
                }}
                onBlur={(e) => {
                  e.currentTarget.style.borderColor = ''
                  e.currentTarget.style.boxShadow = ''
                }}
              />
            </div>
          </div>
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          {orders.length === 0 ? (
            <div className="px-6 py-12 text-center">
              <div className="mx-auto w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
                <Package className="h-8 w-8 text-gray-400" />
              </div>
              <h3 className="text-sm font-medium text-gray-900">No orders</h3>
              <p className="mt-1 text-sm text-gray-500">Get started by creating a new order.</p>
            </div>
          ) : (
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    <input type="checkbox" className="rounded border-gray-300" />
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Order ID
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Date
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Product
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Customer
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Confirmation
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Shipping
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Total
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Action
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {orders.map((order) => (
                  <tr
                    key={order.id}
                    className="hover:bg-gray-50 transition-colors cursor-pointer"
                    onClick={() => (window.location.href = `/orders/${order.id}`)}
                  >
                    <td className="px-6 py-4 whitespace-nowrap">
                      <input type="checkbox" className="rounded border-gray-300" />
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {order.externalOrderId || order.id.substring(0, 8)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatDate(order.createdAt)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {order.productName || 'N/A'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <div
                          className="h-8 w-8 rounded-full flex items-center justify-center text-white text-xs font-medium mr-3"
                          style={{ backgroundColor: storeColor || BRAND_COLORS.primary }}
                        >
                          {order.customerName
                            .split(' ')
                            .map((n) => n[0])
                            .join('')
                            .toUpperCase()
                            .substring(0, 2)}
                        </div>
                        <div className="text-sm font-medium text-gray-900">{order.customerName}</div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(order.status)}`}>
                        <span className="h-2 w-2 rounded-full mr-2 bg-current"></span>
                        {getStatusText(order.status)}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                        <span className="h-2 w-2 rounded-full mr-2 bg-gray-500"></span>
                        Created
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {order.totalAmount} {order.currency}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <button className="text-gray-400 hover:text-gray-600">
                        <svg className="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                          <path d="M10 6a2 2 0 110-4 2 2 0 010 4zM10 12a2 2 0 110-4 2 2 0 010 4zM10 18a2 2 0 110-4 2 2 0 010 4z" />
                        </svg>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Pagination */}
        {orders.length > 0 && (
          <div className="px-6 py-4 border-t border-gray-200 flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <span className="text-sm text-gray-700">Shows</span>
              <select className="border border-gray-300 rounded-lg px-3 py-1 text-sm">
                <option>10 rows</option>
                <option>25 rows</option>
                <option>50 rows</option>
                <option>100 rows</option>
              </select>
            </div>
            <div className="flex items-center space-x-2">
              <button className="px-3 py-1 border border-gray-300 rounded-lg text-sm hover:bg-gray-50">
                Previous
              </button>
              <button 
                className="px-3 py-1 text-white rounded-lg text-sm transition-all"
                style={{ backgroundColor: BRAND_COLORS.primary }}
                onMouseEnter={(e) => e.currentTarget.style.backgroundColor = BRAND_COLORS.secondary}
                onMouseLeave={(e) => e.currentTarget.style.backgroundColor = BRAND_COLORS.primary}
              >
                1
              </button>
              <button className="px-3 py-1 border border-gray-300 rounded-lg text-sm hover:bg-gray-50">
                2
              </button>
              <button className="px-3 py-1 border border-gray-300 rounded-lg text-sm hover:bg-gray-50">
                3
              </button>
              <button className="px-3 py-1 border border-gray-300 rounded-lg text-sm hover:bg-gray-50">
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

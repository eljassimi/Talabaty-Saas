import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { Package, LogOut, Menu, X, Users, LayoutDashboard, Bell, ChevronDown, Settings, MessageCircle, User as UserIcon, DoorOpen } from 'lucide-react'
import { useState, useRef, useEffect } from 'react'
import { useStoreColor } from '../hooks/useStoreColor'

export default function Layout() {
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [profileDropdownOpen, setProfileDropdownOpen] = useState(false)
  const profileDropdownRef = useRef<HTMLDivElement>(null)
  const { storeColor, storeLogo, storeName } = useStoreColor()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const navigation = [
    { name: 'Home', href: '/', icon: LayoutDashboard },
    { name: 'Orders', href: '/orders', icon: Package },
    { name: 'Products', href: '/products', icon: Package },
    { name: 'Customers', href: '/customers', icon: Users },
    { name: 'Call Center', href: '/call-center', icon: Users },
    { name: 'WhatsApp', href: '/whatsapp', icon: MessageCircle },
  ]

  const isActive = (path: string) => {
    if (path === '/') {
      return location.pathname === '/'
    }
    return location.pathname.startsWith(path)
  }

  const getInitials = () => {
    if (!user) return 'U'
    return `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase() || 'U'
  }

  // Helper to convert hex to RGB for gradient
  const hexToRgb = (hex: string) => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
    return result ? {
      r: parseInt(result[1], 16),
      g: parseInt(result[2], 16),
      b: parseInt(result[3], 16)
    } : { r: 18, g: 49, b: 51 } // Default brand primary color RGB
  }

  const rgb = hexToRgb(storeColor)
  const sidebarStyle = {
    background: `linear-gradient(to bottom, ${storeColor}, rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, 0.8))`
  }

  // Close profile dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (profileDropdownRef.current && !profileDropdownRef.current.contains(event.target as Node)) {
        setProfileDropdownOpen(false)
      }
    }

    if (profileDropdownOpen) {
      document.addEventListener('mousedown', handleClickOutside)
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [profileDropdownOpen])

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
      {/* Mobile sidebar backdrop */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 z-40 lg:hidden transition-opacity"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <div
        className={`fixed inset-y-0 left-0 z-50 w-20 lg:w-24 shadow-2xl transform transition-transform duration-300 ease-in-out lg:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
        style={sidebarStyle}
      >
        <div className="flex flex-col h-full">
          {/* Logo Section */}
          <div className="flex items-center justify-center h-20 border-b border-white/20">
            {storeLogo ? (
              <img 
                src={storeLogo} 
                alt={storeName || 'Store'} 
                className="w-12 h-12 rounded-2xl object-cover shadow-lg border-2 border-white/30"
              />
            ) : (
              <div 
                className="w-12 h-12 rounded-2xl flex items-center justify-center shadow-lg"
                style={{ backgroundColor: storeColor }}
              >
                <Package className="h-6 w-6 text-white" />
              </div>
            )}
          </div>

          {/* Navigation */}
          <nav className="flex-1 px-2 py-4 space-y-2 overflow-y-auto">
            {navigation.map((item) => {
              const Icon = item.icon
              const active = isActive(item.href)
              return (
                <Link
                  key={item.name}
                  to={item.href}
                  onClick={() => setSidebarOpen(false)}
                  className={`flex items-center justify-center p-3 rounded-2xl transition-all duration-200 ${
                    active
                      ? 'bg-white shadow-lg'
                      : 'text-white/90 hover:bg-white/20 hover:text-white'
                  }`}
                  style={active ? { color: storeColor } : {}}
                  title={item.name}
                >
                  <Icon className={`h-6 w-6 ${active ? '' : ''}`} style={active ? { color: storeColor } : {}} />
                </Link>
              )
            })}
          </nav>

          {/* User section */}
          <div className="border-t border-white/20 p-3">
            <div className="flex items-center justify-center mb-3">
              <div 
                className="h-12 w-12 rounded-2xl flex items-center justify-center shadow-lg cursor-pointer"
                style={{ backgroundColor: storeColor }}
                onClick={() => setProfileDropdownOpen(!profileDropdownOpen)}
              >
                <span className="text-white font-bold text-sm">
                  {getInitials()}
                </span>
              </div>
            </div>
            <div className="flex items-center justify-center space-x-1">
              <Link
                to="/settings"
                className="flex items-center justify-center p-2.5 text-white rounded-xl hover:bg-white/20 transition-all duration-200"
                title="Settings"
              >
                <Settings className="h-5 w-5" />
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Main content */}
      <div className="lg:pl-24">
        {/* Top bar */}
        <div className="sticky top-0 z-30 bg-white shadow-sm border-b border-gray-200">
          <div className="flex items-center justify-between h-16 px-6">
            <button
              onClick={() => setSidebarOpen(true)}
              className="lg:hidden text-gray-600 hover:text-gray-900 transition-colors"
            >
              <Menu className="h-6 w-6" />
            </button>
            <div className="flex-1" />
            <div className="flex items-center space-x-4">
              <button className="relative p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-all">
                <Bell className="h-5 w-5" />
                <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full"></span>
              </button>
              <div className="relative" ref={profileDropdownRef}>
                <button
                  onClick={() => setProfileDropdownOpen(!profileDropdownOpen)}
                  className="flex items-center space-x-2 px-3 py-2 rounded-lg hover:bg-gray-100 transition-all"
                >
                  <div 
                    className="h-8 w-8 rounded-full flex items-center justify-center shadow-md"
                    style={{ backgroundColor: storeColor }}
                  >
                    <span className="text-white text-xs font-bold">{getInitials()}</span>
                  </div>
                  <div className="text-left hidden sm:block">
                    <p className="text-sm font-medium text-gray-900">{storeName || 'Store'}</p>
                    <p className="text-xs text-gray-500">{user?.role || 'User'}</p>
                  </div>
                  <ChevronDown className={`h-4 w-4 text-gray-600 transition-transform ${profileDropdownOpen ? 'rotate-180' : ''}`} />
                </button>
                
                {profileDropdownOpen && (
                  <div className="absolute right-0 mt-2 w-56 bg-white rounded-lg shadow-lg border border-gray-200 z-50">
                    <div className="px-4 py-3 border-b border-gray-200">
                      <p className="text-sm font-semibold text-gray-900">{user?.firstName} {user?.lastName}</p>
                      <p className="text-xs text-gray-500 truncate">{user?.email}</p>
                    </div>
                    <div className="py-1">
                      <Link
                        to="/settings"
                        onClick={() => setProfileDropdownOpen(false)}
                        className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 transition-colors"
                      >
                        <UserIcon className="h-4 w-4 mr-3 text-gray-500" />
                        Edit profile
                      </Link>
                      <Link
                        to="/settings"
                        onClick={() => setProfileDropdownOpen(false)}
                        className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 transition-colors"
                      >
                        <Settings className="h-4 w-4 mr-3 text-gray-500" />
                        Settings
                      </Link>
                      <div className="border-t border-gray-200 my-1"></div>
                      <button
                        onClick={() => {
                          setProfileDropdownOpen(false)
                          handleLogout()
                        }}
                        className="flex items-center w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 transition-colors"
                      >
                        <DoorOpen className="h-4 w-4 mr-3 text-gray-500" />
                        Log out
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Page content */}
        <main className="p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

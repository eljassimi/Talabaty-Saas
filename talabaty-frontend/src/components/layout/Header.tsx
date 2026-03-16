import { useState, useRef, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Bell, Menu, Search, ChevronDown, Settings, LogOut, Sun, Moon, Store as StoreIcon } from 'lucide-react'
import { useAuth } from '../../contexts/AuthContext'
import { useTheme } from '../../contexts/ThemeContext'
import { useStoreColor } from '../../hooks/useStoreColor'
import { storeService, Store } from '../../services/storeService'
import { userService } from '../../services/userService'

interface HeaderProps {
  onMenuClick: () => void
  storeCount?: number
}

export default function Header({ onMenuClick, storeCount = 0 }: HeaderProps) {
  const navigate = useNavigate()
  const { user, logout, updateUser } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const { storeColor, storeName } = useStoreColor()
  const [searchQuery, setSearchQuery] = useState('')
  const [profileOpen, setProfileOpen] = useState(false)
  const [storeDropdownOpen, setStoreDropdownOpen] = useState(false)
  const [stores, setStores] = useState<Store[]>([])
  const [switchingStore, setSwitchingStore] = useState(false)
  const profileRef = useRef<HTMLDivElement>(null)
  const storeRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (storeCount > 0) {
      storeService.getStores().then((data) => setStores(Array.isArray(data) ? data : [])).catch(console.error)
    }
  }, [storeCount])

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (profileRef.current && !profileRef.current.contains(e.target as Node)) setProfileOpen(false)
      if (storeRef.current && !storeRef.current.contains(e.target as Node)) setStoreDropdownOpen(false)
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleSwitchStore = async (storeId: string) => {
    if (storeId === user?.selectedStoreId || switchingStore) {
      setStoreDropdownOpen(false)
      return
    }
    setSwitchingStore(true)
    setStoreDropdownOpen(false)
    try {
      await userService.updateSelectedStore(storeId)
      updateUser({ selectedStoreId: storeId })
    } catch (e) {
      console.error(e)
    } finally {
      setSwitchingStore(false)
    }
  }

  const getInitials = () => {
    if (!user) return 'U'
    return `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase() || 'U'
  }

  const selectedStore = stores.find((s) => s.id === user?.selectedStoreId)
  const isDark = theme === 'dark'
  const headerBg = isDark ? '#222328' : '#FFFFFF'
  const headerBorder = isDark ? '#3d4048' : '#E6E8EC'
  const searchInputBg = isDark ? '#2A2D35' : '#FFFFFF'
  const searchInputBorder = isDark ? '#3d4048' : '#E6E8EC'
  const searchInputText = isDark ? '#f3f4f6' : '#111827'
  const searchIconColor = isDark ? '#9ca3af' : '#6B7280'
  // Dropdowns and buttons (theme-driven so light mode is never dark)
  const dropdownBg = isDark ? '#2A2D35' : '#FFFFFF'
  const dropdownBorder = isDark ? '#3d4048' : '#E6E8EC'
  const dropdownText = isDark ? '#f3f4f6' : '#111827'
  const dropdownTextMuted = isDark ? '#9ca3af' : '#6B7280'
  const iconColor = isDark ? '#9ca3af' : '#6B7280'

  return (
    <header
      className={`sticky top-0 z-30 h-16 border-b shrink-0 header-theme-${theme}`}
      style={{ backgroundColor: headerBg, borderColor: headerBorder }}
    >
      <div className="h-full flex items-center justify-between gap-4 px-4 lg:px-6">
        <button
          type="button"
          onClick={onMenuClick}
          className="lg:hidden p-2 rounded-lg header-icon-btn transition-colors"
          style={{ color: iconColor }}
          aria-label="Open menu"
        >
          <Menu className="h-6 w-6" />
        </button>

        <div className="flex-1 min-w-0 max-w-md">
          <div className="relative">
            <Search
              className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5"
              style={{ color: searchIconColor }}
            />
            <input
              type="search"
              placeholder="Search..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full h-10 pl-10 pr-4 rounded-xl border text-sm focus:outline-none focus:ring-2 focus:border-transparent transition-shadow"
              style={{
                backgroundColor: searchInputBg,
                borderColor: searchInputBorder,
                color: searchInputText,
                ['--tw-ring-color' as string]: storeColor,
                ['--brand-color' as string]: storeColor,
              }}
            />
          </div>
        </div>

        <div className="flex items-center gap-2 sm:gap-4">
          <button
            type="button"
            onClick={toggleTheme}
            className="p-2 rounded-lg header-icon-btn transition-colors"
            style={{ color: iconColor }}
            aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
          >
            {theme === 'dark' ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
          </button>
          <button
            type="button"
            className="relative p-2 rounded-lg header-icon-btn transition-colors"
            style={{ color: iconColor }}
            aria-label="Notifications"
          >
            <Bell className="h-5 w-5" />
            <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-red-500 rounded-full" />
          </button>

          {/* Store selector (only when multiple stores) */}
          {stores.length > 1 && (
            <div className="relative" ref={storeRef}>
              <button
                type="button"
                onClick={() => setStoreDropdownOpen(!storeDropdownOpen)}
                disabled={switchingStore}
                className="flex items-center gap-2 px-3 py-2 rounded-xl border header-store-trigger min-w-[140px] disabled:opacity-60 transition-colors"
                style={{
                  backgroundColor: dropdownBg,
                  borderColor: dropdownBorder,
                  color: dropdownText,
                }}
              >
                {selectedStore?.logoUrl ? (
                  <img
                    src={selectedStore.logoUrl}
                    alt=""
                    className="h-6 w-6 rounded object-cover"
                  />
                ) : (
                  <div
                    className="h-6 w-6 rounded flex items-center justify-center shrink-0"
                    style={{ backgroundColor: selectedStore?.color || storeColor }}
                  >
                    <span className="text-white text-xs font-bold">
                      {(selectedStore?.name || 'S')[0]}
                    </span>
                  </div>
                )}
                <span className="truncate text-sm font-medium">{selectedStore?.name || 'Store'}</span>
                <ChevronDown
                  className={`h-4 w-4 shrink-0 transition-transform ${storeDropdownOpen ? 'rotate-180' : ''}`}
                  style={{ color: iconColor }}
                />
              </button>
              {storeDropdownOpen && (
                <div
                  className="absolute right-0 mt-1 w-56 py-1 rounded-xl border shadow-lg z-50"
                  style={{ backgroundColor: dropdownBg, borderColor: dropdownBorder }}
                >
                  {stores.map((store) => (
                    <button
                      key={store.id}
                      type="button"
                      onClick={() => handleSwitchStore(store.id)}
                      className="w-full flex items-center gap-3 px-4 py-2.5 text-left header-dropdown-item text-sm transition-colors"
                      style={{ color: dropdownText }}
                    >
                      {store.logoUrl ? (
                        <img src={store.logoUrl} alt="" className="h-8 w-8 rounded object-cover" />
                      ) : (
                        <div
                          className="h-8 w-8 rounded flex items-center justify-center text-white text-xs font-bold"
                          style={{ backgroundColor: store.color || storeColor }}
                        >
                          {store.name[0]}
                        </div>
                      )}
                      <span className="font-medium truncate">{store.name}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}

          <div className="relative" ref={profileRef}>
            <button
              type="button"
              onClick={() => setProfileOpen(!profileOpen)}
              className="flex items-center gap-2 px-2 py-1.5 rounded-xl header-icon-btn transition-colors"
              style={{ color: dropdownText }}
            >
              <div
                className="h-8 w-8 rounded-full flex items-center justify-center text-white text-sm font-semibold shrink-0"
                style={{ backgroundColor: storeColor }}
              >
                {getInitials()}
              </div>
              <div className="hidden sm:block text-left">
                <p className="text-sm font-medium leading-tight" style={{ color: dropdownText }}>
                  {user?.firstName} {user?.lastName}
                </p>
                <p className="text-xs leading-tight" style={{ color: dropdownTextMuted }}>
                  {storeName || 'Store'}
                </p>
              </div>
              <ChevronDown
                className={`h-4 w-4 shrink-0 transition-transform ${profileOpen ? 'rotate-180' : ''}`}
                style={{ color: iconColor }}
              />
            </button>

            {profileOpen && (
              <div
                className="absolute right-0 mt-1 w-56 py-1 rounded-xl border shadow-lg z-50"
                style={{ backgroundColor: dropdownBg, borderColor: dropdownBorder }}
              >
                <div
                  className="px-4 py-3 border-b"
                  style={{ borderColor: dropdownBorder }}
                >
                  <p className="text-sm font-semibold" style={{ color: dropdownText }}>
                    {user?.firstName} {user?.lastName}
                  </p>
                  <p className="text-xs truncate" style={{ color: dropdownTextMuted }}>
                    {user?.email}
                  </p>
                </div>
                <div className="py-1">
                  <button
                    type="button"
                    onClick={() => {
                      setProfileOpen(false)
                      navigate('/select-store')
                    }}
                    className="flex items-center gap-3 w-full px-4 py-2 text-sm header-dropdown-item text-left transition-colors"
                    style={{ color: dropdownTextMuted }}
                  >
                    <StoreIcon className="h-4 w-4" />
                    Switch store
                  </button>
                  <Link
                    to="/settings"
                    onClick={() => setProfileOpen(false)}
                    className="flex items-center gap-3 px-4 py-2 text-sm header-dropdown-item w-full text-left transition-colors"
                    style={{ color: dropdownTextMuted }}
                  >
                    <Settings className="h-4 w-4" />
                    Settings
                  </Link>
                  <button
                    type="button"
                    onClick={() => {
                      setProfileOpen(false)
                      logout()
                      navigate('/login')
                    }}
                    className="flex items-center gap-3 w-full px-4 py-2 text-sm header-dropdown-item text-left transition-colors"
                    style={{ color: dropdownTextMuted }}
                  >
                    <LogOut className="h-4 w-4" />
                    Log out
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  )
}

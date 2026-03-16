import { Link, useLocation } from 'react-router-dom'
import {
  LayoutDashboard,
  Package,
  Store,
  Plug2,
  Users,
  MessageCircle,
  HelpCircle,
  Settings,
  ChevronLeft,
  Banknote,
  Wallet,
  type LucideIcon,
} from 'lucide-react'
import { useStoreColor } from '../../hooks/useStoreColor'
import { useTheme } from '../../contexts/ThemeContext'
import { useAuth } from '../../contexts/AuthContext'
import { getPermissions } from '../../utils/permissions'

export interface NavItem {
  name: string
  href: string
  icon: LucideIcon
  badge?: string | number
}

interface SidebarProps {
  open: boolean
  onClose: () => void
  navItems?: NavItem[]
}

function getNavItemsForRole(role: string | undefined): NavItem[] {
  const permissions = getPermissions(role)
  const items: NavItem[] = [
    { name: 'Dashboard', href: '/', icon: LayoutDashboard },
    { name: 'Orders', href: '/orders', icon: Package },
    { name: 'Stores', href: '/stores', icon: Store },
  ]
  if (permissions.canAccessIntegrations) {
    items.push({ name: 'Integrations', href: '/integrations', icon: Plug2 })
    items.push({ name: 'Automations', href: '/automations', icon: MessageCircle })
  }
  items.push({ name: 'Team', href: '/users', icon: Users })
  if (role === 'SUPPORT') {
    items.push({ name: 'My earnings', href: '/earnings', icon: Wallet })
  }
  if (permissions.canManagePaymentRequests) {
    items.push({ name: 'Payment requests', href: '/payment-requests', icon: Banknote })
  }
  return items
}

export default function Sidebar({ open, onClose, navItems: navItemsProp }: SidebarProps) {
  const location = useLocation()
  const { user } = useAuth()
  const themeItems = getNavItemsForRole(user?.role)
  const navItems = navItemsProp ?? themeItems
  const { theme } = useTheme()
  const { storeColor, storeLogo, storeName } = useStoreColor()
  const isDark = theme === 'dark'
  const sidebarBg = isDark ? '#222328' : '#FFFFFF'
  const sidebarBorder = isDark ? '#3d4048' : '#E6E8EC'

  const isActive = (path: string) => {
    if (path === '/') return location.pathname === '/'
    return location.pathname.startsWith(path)
  }

  return (
    <>
      {/* Mobile overlay */}
      {open && (
        <div
          className="fixed inset-0 bg-black/30 z-40 lg:hidden transition-opacity dark:bg-black/50"
          onClick={onClose}
          aria-hidden="true"
        />
      )}

      <aside
        className={`
          fixed inset-y-0 left-0 z-50 w-[240px] flex flex-col
          border-r transform transition-transform duration-300 ease-out
          lg:translate-x-0
          ${open ? 'translate-x-0' : '-translate-x-full'}
        `}
        style={{
          backgroundColor: sidebarBg,
          borderColor: sidebarBorder,
          boxShadow: open ? '4px 0 24px rgba(0,0,0,0.06)' : undefined,
        }}
      >
        {/* Logo / Store */}
        <div
          className="flex items-center gap-3 h-16 px-5 border-b shrink-0"
          style={{ borderColor: sidebarBorder }}
        >
          <button
            type="button"
            onClick={onClose}
            className="lg:hidden p-1.5 rounded-lg text-[#6B7280] dark:text-gray-400 hover:bg-[#F6F8FB] dark:hover:bg-[#2A2D35] hover:text-[#111827] dark:hover:text-gray-100"
            aria-label="Close menu"
          >
            <ChevronLeft className="h-5 w-5" />
          </button>
          {storeLogo ? (
            <img
              src={storeLogo}
              alt={storeName || 'Store'}
              className="h-9 w-9 rounded-lg object-cover border border-[#E6E8EC] dark:border-gray-600"
            />
          ) : (
            <div
              className="h-9 w-9 rounded-lg flex items-center justify-center shrink-0"
              style={{ backgroundColor: storeColor }}
            >
              <Store className="h-5 w-5 text-white" />
            </div>
          )}
          <span
            className="font-semibold truncate flex-1"
            style={{ color: isDark ? '#f3f4f6' : '#111827' }}
          >
            {storeName || 'Store'}
          </span>
        </div>

        {/* Nav */}
        <nav className="flex-1 overflow-y-auto py-4 px-3 space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon
            const active = isActive(item.href)
            return (
              <Link
                key={item.href}
                to={item.href}
                onClick={onClose}
                className={`
                  flex items-center gap-3 px-3 py-2.5 text-sm font-medium
                  transition-colors duration-200
                  rounded-r-xl rounded-l-md
                  ${active
                    ? isDark
                      ? 'bg-[#2A2D35] text-white dark:text-white'
                      : 'text-[#111827]'
                    : 'text-[#6B7280] dark:text-gray-400 hover:bg-[#F6F8FB] dark:hover:bg-[#2A2D35] hover:text-[#111827] dark:hover:text-gray-100 rounded-r-xl rounded-l-md'}
                `}
                style={
                  active && !isDark
                    ? {
                        backgroundColor: `${storeColor}22`,
                        color: storeColor,
                      }
                    : active && isDark
                    ? { backgroundColor: '#2A2D35', color: '#ffffff' }
                    : undefined
                }
              >
                <Icon
                  className="h-5 w-5 shrink-0"
                  style={active ? (isDark ? { color: '#ffffff' } : { color: storeColor }) : undefined}
                />
                <span className="truncate">{item.name}</span>
                {item.badge != null && (
                  <span className="ml-auto text-xs font-medium text-[#6B7280] dark:text-gray-400 bg-[#E6E8EC] dark:bg-[#2A2D35] px-2 py-0.5 rounded-md">
                    {item.badge}
                  </span>
                )}
              </Link>
            )
          })}
        </nav>

        {/* Bottom: Help & Support, Settings */}
        <div className="p-3 border-t shrink-0 space-y-1" style={{ borderColor: sidebarBorder }}>
          <Link
            to="/help"
            onClick={onClose}
            className={`
              flex items-center gap-3 px-3 py-2.5 rounded-r-xl rounded-l-md text-sm font-medium
              transition-colors duration-200
              ${location.pathname.startsWith('/help')
                ? isDark
                  ? 'bg-[#2A2D35] text-white'
                  : 'text-[#111827]'
                : 'text-[#6B7280] dark:text-gray-400 hover:bg-[#F6F8FB] dark:hover:bg-[#2A2D35] hover:text-[#111827] dark:hover:text-gray-100'}
            `}
            style={
              location.pathname.startsWith('/help')
                ? isDark
                  ? { backgroundColor: '#2A2D35', color: '#ffffff' }
                  : { backgroundColor: `${storeColor}22`, color: storeColor }
                : undefined
            }
          >
            <HelpCircle
              className="h-5 w-5 shrink-0"
              style={location.pathname.startsWith('/help') ? (isDark ? { color: '#ffffff' } : { color: storeColor }) : undefined}
            />
            <span>Help & Support</span>
          </Link>
          <Link
            to="/settings"
            onClick={onClose}
            className={`
              flex items-center gap-3 px-3 py-2.5 rounded-r-xl rounded-l-md text-sm font-medium
              transition-colors duration-200
              ${location.pathname.startsWith('/settings')
                ? isDark
                  ? 'bg-[#2A2D35] text-white'
                  : 'text-[#111827]'
                : 'text-[#6B7280] dark:text-gray-400 hover:bg-[#F6F8FB] dark:hover:bg-[#2A2D35] hover:text-[#111827] dark:hover:text-gray-100'}
            `}
            style={
              location.pathname.startsWith('/settings')
                ? isDark
                  ? { backgroundColor: '#2A2D35', color: '#ffffff' }
                  : { backgroundColor: `${storeColor}22`, color: storeColor }
                : undefined
            }
          >
            <Settings
              className="h-5 w-5 shrink-0"
              style={location.pathname.startsWith('/settings') ? (isDark ? { color: '#ffffff' } : { color: storeColor }) : undefined}
            />
            <span>Settings</span>
          </Link>
        </div>
      </aside>
    </>
  )
}

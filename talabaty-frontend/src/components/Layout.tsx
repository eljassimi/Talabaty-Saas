import { useState, useEffect } from 'react'
import { Outlet } from 'react-router-dom'
import { useStoreColor } from '../hooks/useStoreColor'
import { useTheme } from '../contexts/ThemeContext'
import Sidebar from './layout/Sidebar'
import Header from './layout/Header'
import { storeService } from '../services/storeService'

export default function Layout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [storeCount, setStoreCount] = useState(0)
  const { theme } = useTheme()
  const { storeColor } = useStoreColor()
  const isLight = theme === 'light'

  useEffect(() => {
    storeService.getStores().then((data) => setStoreCount(Array.isArray(data) ? data.length : 0)).catch(() => setStoreCount(0))
  }, [])

  useEffect(() => {
    document.documentElement.style.setProperty('--brand-color', storeColor)
  }, [storeColor])

  return (
    <div
      className="min-h-screen dark:bg-[#222328]"
      style={isLight ? { backgroundColor: '#FFFFFF' } : undefined}
      data-layout="saas-dashboard"
    >
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <div className="lg:pl-[240px] flex flex-col min-h-screen">
        <Header onMenuClick={() => setSidebarOpen((o) => !o)} storeCount={storeCount} />
        <main
          className="flex-1 p-4 lg:p-6 lg:p-8 overflow-auto dark:bg-[#222328]"
          style={isLight ? { backgroundColor: '#FFFFFF' } : undefined}
        >
          <Outlet />
        </main>
      </div>
    </div>
  )
}

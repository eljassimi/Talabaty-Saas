import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import { ThemeProvider } from './contexts/ThemeContext'
import Login from './pages/Login'
import Signup from './pages/Signup'
import Dashboard from './pages/Dashboard'
import Stores from './pages/Stores'
import StoreDetail from './pages/StoreDetail'
import Orders from './pages/Orders'
import OrderDetail from './pages/OrderDetail'
import ShippingProviders from './pages/ShippingProviders'
import Integrations from './pages/Integrations'
import Automations from './pages/Automations'
import Users from './pages/Users'
import ChangePassword from './pages/ChangePassword'
import SelectStore from './pages/SelectStore'
import Settings from './pages/Settings'
import Help from './pages/Help'
import Earnings from './pages/Earnings'
import PaymentRequests from './pages/PaymentRequests'
import Layout from './components/Layout'
import TalabatyLogoSpinner from './components/TalabatyLogoSpinner'

function ProtectedRoute({ children, requireStore = true }: { children: React.ReactNode; requireStore?: boolean }) {
  const { user, loading } = useAuth()

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-white dark:bg-[#222328]">
        <TalabatyLogoSpinner spinning size={88} />
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  // Redirect to change password if required (unless already on change password page)
  if (user.mustChangePassword && window.location.pathname !== '/change-password') {
    return <Navigate to="/change-password" replace />
  }

  // Redirect to store selection if no store selected and store is required
  // Check both user.selectedStoreId and localStorage to handle state updates
  const selectedStoreId = user.selectedStoreId || (() => {
    try {
      const storedUser = localStorage.getItem('user')
      if (storedUser) {
        const parsed = JSON.parse(storedUser)
        return parsed.selectedStoreId
      }
    } catch (e) {
      // Ignore parse errors
    }
    return null
  })()

  if (requireStore && !selectedStoreId && window.location.pathname !== '/select-store') {
    return <Navigate to="/select-store" replace />
  }

  return <>{children}</>
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/signup" element={<Signup />} />
      <Route
        path="/change-password"
        element={
          <ProtectedRoute requireStore={false}>
            <ChangePassword />
          </ProtectedRoute>
        }
      />
      <Route
        path="/select-store"
        element={
          <ProtectedRoute requireStore={false}>
            <SelectStore />
          </ProtectedRoute>
        }
      />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="stores" element={<Stores />} />
        <Route path="stores/:id" element={<StoreDetail />} />
        <Route path="orders" element={<Orders />} />
        <Route path="orders/:id" element={<OrderDetail />} />
        <Route path="shipping" element={<ShippingProviders />} />
        <Route path="integrations" element={<Integrations />} />
        <Route path="automations" element={<Automations />} />
        <Route path="users" element={<Users />} />
        <Route path="earnings" element={<Earnings />} />
        <Route path="payment-requests" element={<PaymentRequests />} />
        <Route path="help" element={<Help />} />
        <Route path="settings" element={<Settings />} />
      </Route>
    </Routes>
  )
}

function App() {
  return (
    <Router>
      <ThemeProvider>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </ThemeProvider>
    </Router>
  )
}

export default App


import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useTheme } from '../contexts/ThemeContext'
import { userService } from '../services/userService'
import { BRAND_COLORS } from '../constants/brand'
import { Lock, AlertCircle, Sun, Moon } from 'lucide-react'

export default function ChangePassword() {
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { updateUser } = useAuth()
  const { theme, toggleTheme } = useTheme()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (newPassword !== confirmPassword) {
      setError('New passwords do not match')
      return
    }

    if (newPassword.length < 8) {
      setError('Password must be at least 8 characters long')
      return
    }

    setLoading(true)

    try {
      // Pass null explicitly for currentPassword since this is a first-time password change
      // The backend will skip validation when currentPassword is null/empty or mustChangePassword is true
      const updatedUser = await userService.changePassword(null, newPassword)
      // Update user in context and localStorage with the updated user data
      updateUser({ 
        mustChangePassword: false,
        ...updatedUser
      })
      // Always redirect to store selection after password change
      navigate('/select-store', { replace: true })
    } catch (err: any) {
      setError(err.response?.data?.error || err.response?.data?.message || 'Failed to change password')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative min-h-screen flex items-center justify-center bg-white dark:bg-[#222328] py-12 px-4 sm:px-6 lg:px-8">
      <button
        type="button"
        onClick={toggleTheme}
        className="absolute top-6 right-6 p-2 rounded-lg text-gray-500 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-[#2A2D35] transition-colors"
        aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
      >
        {theme === 'dark' ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
      </button>

      <div className="max-w-md w-full space-y-8 rounded-2xl border border-gray-200 dark:border-[#3d4048] bg-white dark:bg-[#2A2D35] p-8 shadow-[0_8px_30px_rgba(17,24,39,0.08)] dark:shadow-none">
        <div className="text-center">
          <div className="flex justify-center">
            <div className="p-3 rounded-lg" style={{ backgroundColor: BRAND_COLORS.primary }}>
              <Lock className="h-8 w-8 text-white" />
            </div>
          </div>
          <h2 className="mt-6 text-3xl font-extrabold text-gray-900 dark:text-gray-100">Change Password</h2>
          <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
            You must change your password before continuing
          </p>
        </div>

        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center">
              <AlertCircle className="h-5 w-5 mr-2" />
              {error}
            </div>
          )}

          <div className="rounded-md shadow-sm space-y-4">
            <div>
              <label htmlFor="new-password" className="block text-sm font-medium text-gray-700 dark:text-gray-200 mb-1">
                New Password
              </label>
              <input
                id="new-password"
                name="new-password"
                type="password"
                required
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="appearance-none relative block w-full px-3 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#222328] placeholder-gray-500 dark:placeholder-gray-400 text-gray-900 dark:text-gray-100 rounded-lg focus:outline-none focus:z-10 sm:text-sm"
                style={{ ['--tw-ring-color' as string]: BRAND_COLORS.primary }}
                placeholder="Enter new password (min 8 characters)"
              />
            </div>
            <div>
              <label htmlFor="confirm-password" className="block text-sm font-medium text-gray-700 dark:text-gray-200 mb-1">
                Confirm New Password
              </label>
              <input
                id="confirm-password"
                name="confirm-password"
                type="password"
                required
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="appearance-none relative block w-full px-3 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#222328] placeholder-gray-500 dark:placeholder-gray-400 text-gray-900 dark:text-gray-100 rounded-lg focus:outline-none focus:z-10 sm:text-sm"
                style={{ ['--tw-ring-color' as string]: BRAND_COLORS.primary }}
                placeholder="Confirm new password"
              />
            </div>
          </div>

          <div>
            <button
              type="submit"
              disabled={loading}
              className="group relative w-full flex justify-center py-2.5 px-4 border border-transparent text-sm font-medium rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
              style={{
                background: `linear-gradient(135deg, ${BRAND_COLORS.primary} 0%, ${BRAND_COLORS.secondary} 100%)`,
                ['--tw-ring-color' as string]: BRAND_COLORS.primary,
              }}
            >
              {loading ? 'Changing Password...' : 'Change Password'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}


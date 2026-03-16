import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useTheme } from '../contexts/ThemeContext'
import { BRAND_COLORS } from '../constants/brand'
import TalabatyLogoSvg from '../images/talabaty-logo.svg'
import GridDistortion from '../components/GridDistortion'
import { Eye, EyeOff, Sun, Moon } from 'lucide-react'

export default function Signup() {
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phoneNumber: '',
    accountName: '',
    accountType: 'INDIVIDUAL' as 'INDIVIDUAL' | 'BUSINESS',
  })
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { signup } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const navigate = useNavigate()

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    })
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      // Always set accountType to INDIVIDUAL
      await signup({ ...formData, accountType: 'INDIVIDUAL' })
      navigate('/')
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to create account')
    } finally {
      setLoading(false)
    }
  }

  const handleGoogleSignUp = () => {
    // TODO: Implement Google OAuth
    console.log('Google sign up clicked')
  }

  // Create a gradient image URL using canvas for the distortion effect
  const gradientImageUrl = `data:image/svg+xml,${encodeURIComponent(`
    <svg width="1920" height="1080" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="grad" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" style="stop-color:${BRAND_COLORS.primary};stop-opacity:1" />
          <stop offset="50%" style="stop-color:${BRAND_COLORS.secondary};stop-opacity:1" />
          <stop offset="100%" style="stop-color:${BRAND_COLORS.primary}dd;stop-opacity:1" />
        </linearGradient>
      </defs>
      <rect width="100%" height="100%" fill="url(#grad)"/>
    </svg>
  `)}`

  return (
    <div className="min-h-screen flex bg-white dark:bg-[#222328]">
      {/* Left Side - Gradient with Distortion */}
      <div className="hidden lg:flex lg:w-1/2 relative overflow-hidden" style={{ background: `linear-gradient(135deg, ${BRAND_COLORS.primary} 0%, ${BRAND_COLORS.secondary} 50%, ${BRAND_COLORS.primary}dd 100%)` }}>
        <div style={{ width: '100%', height: '100%', position: 'relative', cursor: 'none' }}>
          <GridDistortion
            imageSrc={gradientImageUrl}
            grid={15}
            mouse={0.2}
            strength={0.3}
            relaxation={0.85}
            className="custom-class"
          />
        </div>
        {/* Text Overlay */}
        <div className="absolute inset-0 flex flex-col justify-between p-12 text-white z-10 pointer-events-none">
          <div className="mb-4">
            <img 
              src={TalabatyLogoSvg} 
              alt="Talabaty" 
              className="h-12 w-auto opacity-90"
            />
          </div>
          <div>
            <p className="text-sm mb-2 opacity-90">You can easily</p>
            <h2 className="text-4xl font-bold leading-tight">
              Get access your personal hub for clarity and productivity
            </h2>
          </div>
        </div>
      </div>

      {/* Right Side - Form */}
      <div className="relative flex-1 flex items-center justify-center p-8 bg-white dark:bg-[#222328] overflow-y-auto">
        <button
          type="button"
          onClick={toggleTheme}
          className="absolute top-6 right-6 p-2 rounded-lg text-gray-500 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-[#2A2D35] transition-colors"
          aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
        >
          {theme === 'dark' ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
        </button>
        <div className="w-full max-w-md my-8">
          {/* Logo */}
          <div className="mb-8">
            <div className="mb-4">
              <img 
                src={TalabatyLogoSvg} 
                alt="Talabaty" 
                className="h-10 w-auto"
              />
            </div>
          </div>

          {/* Title */}
          <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-2">Create an account</h1>
          <p className="text-gray-600 dark:text-gray-400 mb-8">
            Access your tasks, notes, and projects anytime, anywhere - and keep everything flowing in one place.
          </p>

          {/* Error Message */}
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-6 animate-fadeIn">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Name Fields */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label htmlFor="firstName" className="block text-sm font-medium text-gray-900 dark:text-gray-100 mb-2">
                  First Name
                </label>
                <input
                  id="firstName"
                  name="firstName"
                  type="text"
                  required
                  value={formData.firstName}
                  onChange={handleChange}
                  className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-[#2A2D35] text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:border-transparent transition-all"
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
              <div>
                <label htmlFor="lastName" className="block text-sm font-medium text-gray-900 dark:text-gray-100 mb-2">
                  Last Name
                </label>
                <input
                  id="lastName"
                  name="lastName"
                  type="text"
                  required
                  value={formData.lastName}
                  onChange={handleChange}
                  className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-[#2A2D35] text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:border-transparent transition-all"
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

            {/* Email Field */}
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-900 dark:text-gray-100 mb-2">
                Your email
              </label>
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                required
                value={formData.email}
                onChange={handleChange}
                className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-[#2A2D35] text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:border-transparent transition-all"
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
                placeholder="Enter your email"
              />
            </div>

            {/* Password Field */}
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-900 dark:text-gray-100 mb-2">
                Password
              </label>
              <div className="relative">
                <input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  required
                  value={formData.password}
                  onChange={handleChange}
                  className="w-full px-4 py-3 pr-12 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-[#2A2D35] text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:border-transparent transition-all"
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
                  placeholder="Enter your password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
                >
                  {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
            </div>

            {/* Phone Number */}
            <div>
              <label htmlFor="phoneNumber" className="block text-sm font-medium text-gray-900 dark:text-gray-100 mb-2">
                Phone Number
              </label>
              <input
                id="phoneNumber"
                name="phoneNumber"
                type="tel"
                required
                value={formData.phoneNumber}
                onChange={handleChange}
                className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-[#2A2D35] text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:border-transparent transition-all"
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
                placeholder="Enter your phone number"
              />
            </div>

            {/* Account Name */}
            <div>
              <label htmlFor="accountName" className="block text-sm font-medium text-gray-900 dark:text-gray-100 mb-2">
                Account Name
              </label>
              <input
                id="accountName"
                name="accountName"
                type="text"
                required
                value={formData.accountName}
                onChange={handleChange}
                className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-[#2A2D35] text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:border-transparent transition-all"
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
                placeholder="Enter account name"
              />
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 px-4 rounded-lg text-white font-semibold focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-lg hover:shadow-xl"
              style={{
                background: `linear-gradient(135deg, ${BRAND_COLORS.primary} 0%, ${BRAND_COLORS.secondary} 100%)`,
                '--tw-ring-color': BRAND_COLORS.primary,
              } as React.CSSProperties & { '--tw-ring-color': string }}
            >
              {loading ? 'Creating account...' : 'Get Started'}
            </button>
          </form>

          {/* Divider */}
          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-300"></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-2 bg-white dark:bg-[#222328] text-gray-500 dark:text-gray-400">or continue with</span>
            </div>
          </div>

          {/* Google Sign Up Button */}
          <div className="mb-6">
            <button
              type="button"
              onClick={handleGoogleSignUp}
              className="w-full flex items-center justify-center gap-3 py-3 px-4 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-[#2A2D35] hover:bg-gray-50 dark:hover:bg-[#3d4048] transition-colors font-medium text-gray-700 dark:text-gray-200"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
              </svg>
              <span>Continue with Google</span>
            </button>
          </div>

          {/* Sign In Link */}
          <div className="text-center text-sm">
            <span className="text-gray-600 dark:text-gray-400">Already have an account? </span>
            <Link 
              to="/login" 
              className="font-semibold transition-colors"
              style={{ color: BRAND_COLORS.primary }}
              onMouseEnter={(e) => e.currentTarget.style.color = BRAND_COLORS.secondary}
              onMouseLeave={(e) => e.currentTarget.style.color = BRAND_COLORS.primary}
            >
              Sign in
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

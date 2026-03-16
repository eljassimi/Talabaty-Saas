import { useState, useRef } from 'react'
import { storeService, Store } from '../services/storeService'
import { useTheme } from '../contexts/ThemeContext'
import { BRAND_COLORS } from '../constants/brand'
import { X, Upload, Image as ImageIcon } from 'lucide-react'

interface CreateStoreModalProps {
  onClose: () => void
  onSuccess: (store?: Store) => void
}

export default function CreateStoreModal({ onClose, onSuccess }: CreateStoreModalProps) {
  const [formData, setFormData] = useState({
    name: '',
    managerId: '',
    logoUrl: '',
    color: '#123133', // Default blue color
  })
  const [logoPreview, setLogoPreview] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const fileInputRef = useRef<HTMLInputElement>(null)
  const { theme } = useTheme()
  const isDark = theme === 'dark'

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      // Validate file type
      if (!file.type.startsWith('image/')) {
        setError('Please select an image file')
        return
      }
      // Validate file size (max 2MB)
      if (file.size > 2 * 1024 * 1024) {
        setError('Image size must be less than 2MB')
        return
      }
      
      const reader = new FileReader()
      reader.onloadend = () => {
        const base64String = reader.result as string
        setFormData({ ...formData, logoUrl: base64String })
        setLogoPreview(base64String)
        setError('')
      }
      reader.onerror = () => {
        setError('Failed to read image file')
      }
      reader.readAsDataURL(file)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const createData: any = { 
        name: formData.name,
        color: formData.color
      }
      if (formData.managerId && formData.managerId.trim() !== '') {
        createData.managerId = formData.managerId.trim()
      }
      if (formData.logoUrl) {
        createData.logoUrl = formData.logoUrl
      }
      const newStore = await storeService.createStore(createData)
      onSuccess(newStore)
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to create store')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/40 overflow-y-auto h-full w-full z-50 flex items-center justify-center p-4">
      <div
        className="relative rounded-xl w-full max-w-md border shadow-xl dark:shadow-none"
        style={{
          backgroundColor: isDark ? '#2A2D35' : '#FFFFFF',
          borderColor: isDark ? '#3d4048' : '#E5E7EB',
        }}
      >
        <div
          className="flex items-center justify-between p-6 border-b"
          style={{ borderColor: isDark ? '#3d4048' : '#E5E7EB' }}
        >
          <h3 className="text-lg font-bold" style={{ color: isDark ? '#F3F4F6' : '#111827' }}>Create New Store</h3>
          <button 
            onClick={onClose} 
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              {error}
            </div>
          )}

          <div>
            <label
              htmlFor="name"
              className="block text-sm font-medium mb-2"
              style={{ color: isDark ? '#E5E7EB' : '#374151' }}
            >
              Store Name *
            </label>
            <input
              id="name"
              type="text"
              required
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              className="block w-full px-3 py-2 border rounded-lg shadow-sm focus:outline-none focus:ring-2 sm:text-sm"
              style={{
                backgroundColor: isDark ? '#222328' : '#FFFFFF',
                borderColor: isDark ? '#4B5563' : '#D1D5DB',
                color: isDark ? '#F3F4F6' : '#111827',
                ['--tw-ring-color' as string]: BRAND_COLORS.primary,
              }}
              placeholder="Enter store name"
            />
          </div>

          {/* Logo Upload */}
          <div>
            <label
              className="block text-sm font-medium mb-2"
              style={{ color: isDark ? '#E5E7EB' : '#374151' }}
            >
              Store Logo
            </label>
            <div className="flex items-center space-x-4">
              {logoPreview ? (
                <div className="relative">
                  <img 
                    src={logoPreview} 
                    alt="Logo preview" 
                    className="w-20 h-20 object-cover rounded-lg border-2 border-gray-200 dark:border-[#3d4048]"
                  />
                  <button
                    type="button"
                    onClick={() => {
                      setLogoPreview(null)
                      setFormData({ ...formData, logoUrl: '' })
                      if (fileInputRef.current) {
                        fileInputRef.current.value = ''
                      }
                    }}
                    className="absolute -top-2 -right-2 bg-red-500 text-white rounded-full p-1 hover:bg-red-600"
                  >
                    <X className="h-3 w-3" />
                  </button>
                </div>
              ) : (
                <div 
                  onClick={() => fileInputRef.current?.click()}
                  className="w-20 h-20 border-2 border-dashed rounded-lg flex items-center justify-center cursor-pointer transition-colors"
                  style={{
                    borderColor: isDark ? '#4B5563' : '#D1D5DB',
                    backgroundColor: isDark ? '#222328' : '#FFFFFF',
                  }}
                >
                  <ImageIcon className="h-8 w-8 text-gray-400" />
                </div>
              )}
              <div className="flex-1">
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  onChange={handleFileChange}
                  className="hidden"
                />
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="inline-flex items-center px-4 py-2 border rounded-lg shadow-sm text-sm font-medium focus:outline-none focus:ring-2"
                  style={{
                    backgroundColor: isDark ? '#222328' : '#FFFFFF',
                    borderColor: isDark ? '#4B5563' : '#D1D5DB',
                    color: isDark ? '#E5E7EB' : '#374151',
                    ['--tw-ring-color' as string]: BRAND_COLORS.primary,
                  }}
                >
                  <Upload className="h-4 w-4 mr-2" />
                  {logoPreview ? 'Change Logo' : 'Upload Logo'}
                </button>
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  PNG, JPG up to 2MB
                </p>
              </div>
            </div>
          </div>

          {/* Color Picker */}
          <div>
            <label
              htmlFor="color"
              className="block text-sm font-medium mb-2"
              style={{ color: isDark ? '#E5E7EB' : '#374151' }}
            >
              Store Color
            </label>
            <div className="flex items-center space-x-3">
              <input
                id="color"
                type="color"
                value={formData.color}
                onChange={(e) => setFormData({ ...formData, color: e.target.value })}
                className="h-10 w-20 border rounded-lg cursor-pointer"
                style={{ borderColor: isDark ? '#4B5563' : '#D1D5DB' }}
              />
              <input
                type="text"
                value={formData.color}
                onChange={(e) => setFormData({ ...formData, color: e.target.value })}
                className="flex-1 px-3 py-2 border rounded-lg shadow-sm focus:outline-none focus:ring-2 sm:text-sm"
                style={{
                  backgroundColor: isDark ? '#222328' : '#FFFFFF',
                  borderColor: isDark ? '#4B5563' : '#D1D5DB',
                  color: isDark ? '#F3F4F6' : '#111827',
                  ['--tw-ring-color' as string]: BRAND_COLORS.primary,
                }}
                placeholder="#123133"
                pattern="^#[0-9A-Fa-f]{6}$"
              />
            </div>
            <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
              Choose a color theme for your store
            </p>
          </div>

          <div>
            <label
              htmlFor="managerId"
              className="block text-sm font-medium mb-2"
              style={{ color: isDark ? '#E5E7EB' : '#374151' }}
            >
              Manager ID (Optional)
            </label>
            <input
              id="managerId"
              type="text"
              value={formData.managerId}
              onChange={(e) => setFormData({ ...formData, managerId: e.target.value })}
              placeholder="Enter manager UUID (optional)"
              className="block w-full px-3 py-2 border rounded-lg shadow-sm focus:outline-none focus:ring-2 sm:text-sm"
              style={{
                backgroundColor: isDark ? '#222328' : '#FFFFFF',
                borderColor: isDark ? '#4B5563' : '#D1D5DB',
                color: isDark ? '#F3F4F6' : '#111827',
                ['--tw-ring-color' as string]: BRAND_COLORS.primary,
              }}
            />
            <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
              Optional: You can assign a manager later.
            </p>
          </div>

          <div
            className="flex items-center justify-end space-x-3 pt-4 border-t"
            style={{ borderColor: isDark ? '#3d4048' : '#E5E7EB' }}
          >
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium border rounded-lg transition-colors"
              style={{
                backgroundColor: isDark ? '#222328' : '#FFFFFF',
                borderColor: isDark ? '#4B5563' : '#D1D5DB',
                color: isDark ? '#E5E7EB' : '#374151',
              }}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 text-sm font-medium text-white rounded-lg disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              style={{ backgroundColor: BRAND_COLORS.secondary }}
            >
              {loading ? 'Creating...' : 'Create Store'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

import { useState, useEffect } from 'react'
import type React from 'react'
import { storeService, CreateShippingProviderRequest, ShippingProvider } from '../services/storeService'
import { useStoreColor } from '../hooks/useStoreColor'
import { X } from 'lucide-react'
import { getProviderConfig, getProviderLogo } from '../utils/deliveryProviders'

interface ShippingProviderFormProps {
  storeId: string
  providerName?: string
  providerType?: string
  existingProvider?: ShippingProvider
  onClose: () => void
  onSuccess: () => void
}

export default function ShippingProviderForm({
  storeId,
  providerName,
  providerType,
  existingProvider,
  onClose,
  onSuccess,
}: ShippingProviderFormProps) {
  const { storeColor } = useStoreColor()
  const providerConfig = providerName ? getProviderConfig(providerName) : null
  const isEditMode = !!existingProvider
  
  const [formData, setFormData] = useState<CreateShippingProviderRequest>({
    customerId: existingProvider?.customerId || '',
    apiKey: '',
    displayName: existingProvider?.displayName || providerConfig?.displayName || '',
    providerType: existingProvider?.providerType || providerConfig?.providerType || providerType || '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  
  useEffect(() => {
    if (providerConfig && !existingProvider) {
      setFormData(prev => ({
        ...prev,
        displayName: providerConfig.displayName,
        providerType: providerConfig.providerType,
      }))
    }
  }, [providerConfig, existingProvider])

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value })
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      // Prepare form data - apiKey is required for new providers, optional for editing
      if (!isEditMode && !formData.apiKey) {
        setError('API Key is required')
        setLoading(false)
        return
      }
      
      const submitData: CreateShippingProviderRequest = {
        customerId: formData.customerId,
        displayName: formData.displayName,
        providerType: formData.providerType,
        apiKey: formData.apiKey || '',
      }
      
      const provider = await storeService.createShippingProvider(storeId, submitData)
      console.log(isEditMode ? 'Updated provider:' : 'Created provider:', provider) // Debug log
      onSuccess()
    } catch (err: any) {
      console.error('Error saving provider:', err) // Debug log
      setError(err.response?.data?.message || err.response?.data?.error || err.message || `Failed to ${isEditMode ? 'update' : 'create'} shipping provider`)
    } finally {
      setLoading(false)
    }
  }

  const logo = providerConfig?.logo || 
               (existingProvider ? getProviderLogo(existingProvider.providerType, existingProvider.displayName) : null) ||
               null
  const title = isEditMode 
    ? `Configure ${existingProvider?.displayName || 'Provider'}`
    : `Configure ${providerConfig?.displayName || 'Provider'}`

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-full max-w-md shadow-lg rounded-md bg-white">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            {logo ? (
              <img 
                src={logo}
                alt={title}
                className="h-10 w-10 object-contain"
                onError={(e) => {
                  // Fallback if image fails to load
                  e.currentTarget.style.display = 'none'
                }}
              />
            ) : null}
            <h3 className="text-lg font-bold text-gray-900">{title}</h3>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X className="h-6 w-6" />
          </button>
        </div>

        {error && (
          <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="displayName" className="block text-sm font-medium text-gray-700 mb-1">
              Display Name
            </label>
            <input
              id="displayName"
              name="displayName"
              type="text"
              value={formData.displayName}
              onChange={handleChange}
              className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
            />
          </div>

          <div>
            <label htmlFor="customerId" className="block text-sm font-medium text-gray-700 mb-1">
              Customer ID *
            </label>
            <input
              id="customerId"
              name="customerId"
              type="text"
              required
              value={formData.customerId}
              onChange={handleChange}
              placeholder={`Enter ${providerConfig?.displayName || 'Provider'} Customer ID`}
              className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
              style={{ 
                '--tw-ring-color': storeColor,
              } as React.CSSProperties}
            />
            <p className="mt-1 text-xs text-gray-500">
              Find your Customer ID in your {providerConfig?.displayName || 'provider'} dashboard
            </p>
          </div>

          <div>
            <label htmlFor="apiKey" className="block text-sm font-medium text-gray-700 mb-1">
              API Key {!isEditMode && '*'}
            </label>
            <input
              id="apiKey"
              name="apiKey"
              type="password"
              required={!isEditMode}
              value={formData.apiKey}
              onChange={handleChange}
              placeholder={isEditMode ? "Leave blank to keep existing API key" : `Enter ${providerConfig?.displayName || 'Provider'} API Key`}
              className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
              style={{ 
                '--tw-ring-color': storeColor,
              } as React.CSSProperties}
            />
            <p className="mt-1 text-xs text-gray-500">
              {isEditMode 
                ? "Leave blank to keep the existing API key unchanged"
                : `Generate your API key in ${providerConfig?.displayName || 'provider'} dashboard → Account → Generate API key`}
            </p>
          </div>

          <div className="flex items-center justify-end space-x-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 text-sm font-medium text-white rounded-md hover:opacity-90 disabled:opacity-50 transition-opacity"
              style={{ backgroundColor: storeColor }}
            >
              {loading ? 'Saving...' : 'Save Configuration'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}


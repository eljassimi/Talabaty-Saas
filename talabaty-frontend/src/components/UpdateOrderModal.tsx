import { useState, useEffect } from 'react'
import { orderService, Order, UpdateOrderRequest } from '../services/orderService'
import { X } from 'lucide-react'

interface UpdateOrderModalProps {
  order: Order
  onClose: () => void
  onSuccess: () => void
}

export default function UpdateOrderModal({ order, onClose, onSuccess }: UpdateOrderModalProps) {
  const [loading, setLoading] = useState(false)
  const [formData, setFormData] = useState<UpdateOrderRequest>({
    customerName: order.customerName,
    customerPhone: order.customerPhone,
    destinationAddress: order.destinationAddress,
    city: order.city,
    totalAmount: order.totalAmount,
    currency: order.currency,
    productName: order.productName,
    productId: order.productId,
  })

  useEffect(() => {
    setFormData({
      customerName: order.customerName,
      customerPhone: order.customerPhone,
      destinationAddress: order.destinationAddress,
      city: order.city,
      totalAmount: order.totalAmount,
      currency: order.currency,
      productName: order.productName,
      productId: order.productId,
    })
  }, [order])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)

    try {
      await orderService.updateOrder(order.id, formData)
      onSuccess()
      onClose()
    } catch (error: any) {
      console.error('Error updating order:', error)
      alert(error.response?.data?.error || 'Failed to update order')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-96 shadow-lg rounded-md bg-white">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-bold text-gray-900">Update Order</h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-500"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Customer Name
            </label>
            <input
              type="text"
              required
              value={formData.customerName || ''}
              onChange={(e) => setFormData({ ...formData, customerName: e.target.value })}
              className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Product Name
            </label>
            <input
              type="text"
              value={formData.productName || ''}
              onChange={(e) => setFormData({ ...formData, productName: e.target.value })}
              className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Product ID
            </label>
            <input
              type="text"
              value={formData.productId || ''}
              onChange={(e) => setFormData({ ...formData, productId: e.target.value })}
              className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Phone Number
            </label>
            <input
              type="tel"
              required
              value={formData.customerPhone || ''}
              onChange={(e) => setFormData({ ...formData, customerPhone: e.target.value })}
              className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              City
            </label>
            <input
              type="text"
              value={formData.city || ''}
              onChange={(e) => setFormData({ ...formData, city: e.target.value })}
              className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Delivery Address
            </label>
            <textarea
              required
              rows={3}
              value={formData.destinationAddress || ''}
              onChange={(e) => setFormData({ ...formData, destinationAddress: e.target.value })}
              className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Total Amount
              </label>
              <input
                type="number"
                step="0.01"
                required
                value={formData.totalAmount || 0}
                onChange={(e) => setFormData({ ...formData, totalAmount: parseFloat(e.target.value) })}
                className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Currency
              </label>
              <select
                value={formData.currency || 'USD'}
                onChange={(e) => setFormData({ ...formData, currency: e.target.value })}
                className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
              >
                <option value="USD">USD</option>
                <option value="EUR">EUR</option>
                <option value="MAD">MAD</option>
              </select>
            </div>
          </div>

          <div className="flex justify-end space-x-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:opacity-50"
            >
              {loading ? 'Updating...' : 'Update Order'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}


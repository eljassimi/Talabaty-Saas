import { useState, useEffect } from 'react';
import { orderService, Order, SendToShippingRequest } from '../services/orderService';
import { shippingService } from '../services/shippingService';
import { X } from 'lucide-react';
interface SendToShippingModalProps {
    orderId: string;
    order: Order;
    onClose: () => void;
    onSuccess: () => void;
}
export default function SendToShippingModal({ orderId, order, onClose, onSuccess, }: SendToShippingModalProps) {
    const [formData, setFormData] = useState<SendToShippingRequest>({
        cityId: '',
        note: '',
        nature: '',
        stock: 1,
        open: 1,
        fragile: 0,
        replace: 0,
    });
    const [cities, setCities] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [loadingCities, setLoadingCities] = useState(true);
    const [error, setError] = useState('');
    useEffect(() => {
        loadCities();
    }, []);
    const loadCities = async () => {
        try {
            const data = await shippingService.getCities();
            setCities(data);
        }
        catch (error) {
            console.error('Error loading cities:', error);
        }
        finally {
            setLoadingCities(false);
        }
    };
    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
        const value = e.target.type === 'number' ? parseInt(e.target.value) || 0 : e.target.value;
        setFormData({ ...formData, [e.target.name]: value });
    };
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            await orderService.sendOrderToShipping(orderId, formData);
            onSuccess();
        }
        catch (err: any) {
            setError(err.response?.data?.error || 'Failed to send order to shipping');
        }
        finally {
            setLoading(false);
        }
    };
    return (<div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-full max-w-2xl shadow-lg rounded-md bg-white max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            <img src="https://ozoneexpress.ma/wp/wp-content/uploads/2025/07/Untitled-design-38.png" alt="Ozon Express" className="h-10 w-10 object-contain"/>
            <h3 className="text-lg font-bold text-gray-900">Send Order to Ozon Express</h3>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X className="h-6 w-6"/>
          </button>
        </div>

        {error && (<div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
            {error}
          </div>)}

        <div className="mb-4 p-4 bg-gray-50 rounded-lg">
          <h4 className="text-sm font-medium text-gray-900 mb-2">Order Information</h4>
          <div className="text-sm text-gray-600 space-y-1">
            <p>
              <span className="font-medium">Customer:</span> {order.customerName}
            </p>
            <p>
              <span className="font-medium">Phone:</span> {order.customerPhone}
            </p>
            <p>
              <span className="font-medium">Address:</span> {order.destinationAddress}
            </p>
            <p>
              <span className="font-medium">Amount:</span> {order.totalAmount} {order.currency}
            </p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="cityId" className="block text-sm font-medium text-gray-700 mb-1">
              City *
            </label>
            {loadingCities ? (<div className="text-sm text-gray-500">Loading cities...</div>) : (<select id="cityId" name="cityId" required value={formData.cityId} onChange={handleChange} className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm">
                <option value="">Select a city</option>
                {cities.map((city) => (<option key={city.id} value={city.id.toString()}>
                    {city.name} (ID: {city.id})
                  </option>))}
              </select>)}
          </div>

          <div>
            <label htmlFor="nature" className="block text-sm font-medium text-gray-700 mb-1">
              Nature (Description)
            </label>
            <input id="nature" name="nature" type="text" value={formData.nature} onChange={handleChange} placeholder="Description of contents" className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"/>
          </div>

          <div>
            <label htmlFor="note" className="block text-sm font-medium text-gray-700 mb-1">
              Note (Special Instructions)
            </label>
            <textarea id="note" name="note" rows={3} value={formData.note} onChange={handleChange} placeholder="Special delivery instructions" className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"/>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="stock" className="block text-sm font-medium text-gray-700 mb-1">
                Stock (1=Stock, 0=Pickup)
              </label>
              <select id="stock" name="stock" value={formData.stock} onChange={handleChange} className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm">
                <option value={1}>Stock</option>
                <option value={0}>Pickup</option>
              </select>
            </div>

            <div>
              <label htmlFor="open" className="block text-sm font-medium text-gray-700 mb-1">
                Open Parcel (1=Yes, 2=No)
              </label>
              <select id="open" name="open" value={formData.open} onChange={handleChange} className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm">
                <option value={1}>Open</option>
                <option value={2}>Don't Open</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="fragile" className="block text-sm font-medium text-gray-700 mb-1">
                Fragile (1=Yes, 0=No)
              </label>
              <select id="fragile" name="fragile" value={formData.fragile} onChange={handleChange} className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm">
                <option value={0}>No</option>
                <option value={1}>Yes</option>
              </select>
            </div>

            <div>
              <label htmlFor="replace" className="block text-sm font-medium text-gray-700 mb-1">
                Replace (1=Yes, 0=No)
              </label>
              <select id="replace" name="replace" value={formData.replace} onChange={handleChange} className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm">
                <option value={0}>No</option>
                <option value={1}>Yes</option>
              </select>
            </div>
          </div>

          <div className="flex items-center justify-end space-x-3 pt-4">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50">
              Cancel
            </button>
            <button type="submit" disabled={loading || loadingCities} className="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-md hover:bg-primary-700 disabled:opacity-50">
              {loading ? 'Sending...' : 'Send to Shipping'}
            </button>
          </div>
        </form>
      </div>
    </div>);
}

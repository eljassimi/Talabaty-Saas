import { useState, useEffect } from 'react';
import { orderService, SendToShippingRequest, Order } from '../services/orderService';
import { shippingService } from '../services/shippingService';
import { findCityId, getUniqueCitiesFromOrders } from '../utils/cityMapping';
import { X, Truck, CheckCircle, XCircle, AlertCircle, Info } from 'lucide-react';
interface BatchSendToShippingModalProps {
    orders: Order[];
    onClose: () => void;
    onSuccess: (result: any) => void;
}
export default function BatchSendToShippingModal({ orders, onClose, onSuccess, }: BatchSendToShippingModalProps) {
    const [formData, setFormData] = useState<SendToShippingRequest>({
        cityId: '',
        note: '',
        nature: '',
        stock: 0,
        open: 1,
        fragile: 0,
        replace: 0,
    });
    const [cities, setCities] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [loadingCities, setLoadingCities] = useState(true);
    const [error, setError] = useState('');
    const [result, setResult] = useState<any>(null);
    const [detectedCity, setDetectedCity] = useState<{
        name: string;
        id: number;
    } | null>(null);
    const [multipleCities, setMultipleCities] = useState<string[]>([]);
    useEffect(() => {
        loadCities();
        if (orders && orders.length > 0) {
            detectCityFromOrders();
        }
    }, [orders]);
    const detectCityFromOrders = () => {
        if (!orders || orders.length === 0)
            return;
        const uniqueCities = getUniqueCitiesFromOrders(orders);
        if (uniqueCities.length === 0) {
            setDetectedCity(null);
            setMultipleCities([]);
            return;
        }
        if (uniqueCities.length === 1) {
            const cityName = uniqueCities[0];
            const cityId = findCityId(cityName);
            if (cityId) {
                setDetectedCity({ name: cityName, id: cityId });
                setFormData(prev => {
                    const updated = { ...prev, cityId: cityId.toString() };
                    console.log('Setting cityId to:', cityId.toString(), 'for city:', cityName);
                    return updated;
                });
                setMultipleCities([]);
            }
            else {
                setDetectedCity(null);
                setMultipleCities([cityName]);
            }
        }
        else {
            setDetectedCity(null);
            setMultipleCities(uniqueCities);
            const cityCounts: Record<string, number> = {};
            uniqueCities.forEach(city => {
                cityCounts[city] = orders.filter(o => o.city === city).length;
            });
            const mostCommonCity = Object.entries(cityCounts)
                .sort((a, b) => b[1] - a[1])[0]?.[0];
            if (mostCommonCity) {
                const cityId = findCityId(mostCommonCity);
                if (cityId) {
                    setDetectedCity({ name: mostCommonCity, id: cityId });
                    setFormData(prev => {
                        const updated = { ...prev, cityId: cityId.toString() };
                        console.log('Setting cityId to:', cityId.toString(), 'for most common city:', mostCommonCity);
                        return updated;
                    });
                }
            }
        }
    };
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
        setResult(null);
        console.log('Form data before submit:', formData);
        console.log('Detected city:', detectedCity);
        if (!formData.cityId || formData.cityId.trim() === '') {
            console.error('City ID is missing!', { formData, detectedCity });
            setError('Please select a city. The city should be auto-detected, but if not, please select it manually.');
            setLoading(false);
            return;
        }
        if (!orders || orders.length === 0) {
            setError('No orders selected');
            setLoading(false);
            return;
        }
        try {
            const orderIds = orders.map(order => order.id);
            const submitData: SendToShippingRequest = {
                cityId: formData.cityId.trim(),
                note: formData.note?.trim() || undefined,
                nature: formData.nature?.trim() || undefined,
                stock: 0,
                open: 1,
                fragile: formData.fragile !== undefined ? formData.fragile : 0,
                replace: 0,
            };
            console.log('Sending batch request:', {
                orderIds,
                orderCount: orderIds.length,
                submitData,
                cityId: submitData.cityId
            });
            const response = await orderService.sendOrdersToShipping(orderIds, submitData);
            console.log('Batch response:', response);
            setResult(response);
            if (response.failureCount === 0) {
                setTimeout(() => {
                    onSuccess(response);
                }, 2000);
            }
        }
        catch (err: any) {
            console.error('Error sending orders to shipping:', err);
            console.error('Error response:', err.response);
            console.error('Error data:', err.response?.data);
            let errorMessage = 'Failed to send orders to shipping';
            if (err.response?.data) {
                if (err.response.data.message) {
                    errorMessage = err.response.data.message;
                }
                else if (err.response.data.error) {
                    errorMessage = err.response.data.error;
                }
                else if (typeof err.response.data === 'string') {
                    errorMessage = err.response.data;
                }
                else {
                    errorMessage = JSON.stringify(err.response.data);
                }
            }
            else if (err.message) {
                errorMessage = err.message;
            }
            setError(errorMessage);
        }
        finally {
            setLoading(false);
        }
    };
    if (!orders || orders.length === 0) {
        return null;
    }
    return (<div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-full max-w-2xl shadow-lg rounded-md bg-white max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            <img src="https://ozoneexpress.ma/wp/wp-content/uploads/2025/07/Untitled-design-38.png" alt="Ozon Express" className="h-10 w-10 object-contain"/>
            <h3 className="text-lg font-bold text-gray-900">
              Send {orders.length} Orders to Ozon Express
            </h3>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X className="h-6 w-6"/>
          </button>
        </div>

        {error && !result && (<div className="mb-3 bg-red-50 border border-red-200 text-red-700 px-3 py-2.5 rounded-lg">
            <div className="flex items-start">
              <XCircle className="h-4 w-4 mr-2 mt-0.5 flex-shrink-0"/>
              <div className="flex-1">
                <p className="font-medium text-sm mb-1">Error sending orders</p>
                <p className="text-xs">{error}</p>
                {!formData.cityId && (<p className="text-xs mt-2 font-medium">⚠ City ID is missing. Please ensure a city is auto-detected or selected.</p>)}
              </div>
            </div>
          </div>)}

        {result && (<div className="mb-4 bg-white border border-gray-200 rounded-lg p-4">
            <div className="flex items-center justify-between mb-3">
              <h4 className="text-sm font-semibold text-gray-900">Batch Send Results</h4>
              <span className={`text-xs font-medium px-2 py-1 rounded-full ${result.failureCount === 0
                ? 'bg-green-100 text-green-800'
                : 'bg-yellow-100 text-yellow-800'}`}>
                {result.successCount} succeeded, {result.failureCount} failed
              </span>
            </div>
            
            {result.successful && result.successful.length > 0 && (<div className="mb-3">
                <p className="text-xs font-medium text-gray-700 mb-2">Successful Orders:</p>
                <div className="space-y-1 max-h-32 overflow-y-auto">
                  {result.successful.map((item: any, idx: number) => (<div key={idx} className="flex items-center text-xs text-gray-600 bg-green-50 px-2 py-1 rounded">
                      <CheckCircle className="h-3 w-3 mr-2 text-green-600"/>
                      Order {item.orderId?.substring(0, 8)}... 
                      {item['TRACKING-NUMBER'] && ` - Tracking: ${item['TRACKING-NUMBER']}`}
                    </div>))}
                </div>
              </div>)}

            {result.failed && result.failed.length > 0 && (<div>
                <p className="text-xs font-medium text-gray-700 mb-2">Failed Orders:</p>
                <div className="space-y-1 max-h-32 overflow-y-auto">
                  {result.failed.map((item: any, idx: number) => (<div key={idx} className="flex items-start text-xs text-red-600 bg-red-50 px-2 py-1 rounded">
                      <XCircle className="h-3 w-3 mr-2 mt-0.5 flex-shrink-0"/>
                      <span>Order {item.orderId?.substring(0, 8)}... - {item.error}</span>
                    </div>))}
                </div>
              </div>)}

            {result.failureCount === 0 && (<div className="mt-3 pt-3 border-t border-gray-200">
                <p className="text-sm text-green-700 flex items-center">
                  <CheckCircle className="h-4 w-4 mr-2"/>
                  All orders sent successfully! Closing in 2 seconds...
                </p>
              </div>)}
          </div>)}

        {!result && (<form onSubmit={handleSubmit} className="space-y-3">
            <div className="mb-3 p-3 bg-blue-50 rounded-lg border border-blue-200">
              <div className="flex items-start">
                <AlertCircle className="h-4 w-4 text-blue-600 mr-2 mt-0.5 flex-shrink-0"/>
                <div className="text-xs text-blue-800">
                  <p className="font-medium">Sending {orders.length} orders to Ozon Express</p>
                  <p className="mt-1">All selected orders will be sent with the same shipping configuration.</p>
                  {detectedCity && (<p className="mt-1 text-green-700">
                      ✓ City auto-detected: {detectedCity.name} (ID: {detectedCity.id})
                    </p>)}
                </div>
              </div>
            </div>

            {multipleCities.length > 1 && (<div className="mb-3 p-2.5 bg-yellow-50 rounded-lg border border-yellow-200">
                <div className="flex items-start">
                  <AlertCircle className="h-3.5 w-3.5 text-yellow-600 mr-2 mt-0.5 flex-shrink-0"/>
                  <div className="text-xs text-yellow-800">
                    <p className="font-medium mb-1">Multiple cities detected:</p>
                    <ul className="list-disc list-inside space-y-0.5">
                      {multipleCities.map((city, idx) => {
                    const cityId = findCityId(city);
                    const orderCount = orders.filter(o => o.city === city).length;
                    return (<li key={idx}>
                            {city} ({orderCount} {orderCount === 1 ? 'order' : 'orders'})
                            {cityId ? (<span className="text-green-700 ml-1">✓ ID: {cityId}</span>) : (<span className="text-red-700 ml-1">⚠ Not found</span>)}
                          </li>);
                })}
                    </ul>
                    {detectedCity && (<p className="mt-1.5 text-xs">
                        Most common: <span className="font-medium">{detectedCity.name}</span> (ID: {detectedCity.id}) pre-selected
                      </p>)}
                  </div>
                </div>
              </div>)}

            {!detectedCity && multipleCities.length === 0 && (<div className="mb-3 p-2.5 bg-gray-50 rounded-lg border border-gray-200">
                <div className="flex items-start">
                  <Info className="h-3.5 w-3.5 text-gray-600 mr-2 mt-0.5 flex-shrink-0"/>
                  <p className="text-xs text-gray-700">No city information found. Please select a city manually.</p>
                </div>
              </div>)}

            
            {multipleCities.length === 1 && !detectedCity && (<div className="mb-3 p-2.5 bg-red-50 rounded-lg border border-red-200">
                <div className="flex items-start">
                  <AlertCircle className="h-3.5 w-3.5 text-red-600 mr-2 mt-0.5 flex-shrink-0"/>
                  <div className="text-xs text-red-800">
                    <p className="font-medium">City "{multipleCities[0]}" not found in mapping</p>
                    <p className="mt-1">Contact support to add this city or select a valid city manually.</p>
                  </div>
                </div>
              </div>)}

            
            {loadingCities && (<div className="text-sm text-gray-500 mb-3">Loading cities...</div>)}
            {!loadingCities && !formData.cityId && (<div className="mb-3 p-2.5 bg-red-50 rounded-lg border border-red-200">
                <div className="flex items-start">
                  <AlertCircle className="h-3.5 w-3.5 text-red-600 mr-2 mt-0.5 flex-shrink-0"/>
                  <p className="text-xs text-red-800">No city selected. Please wait for auto-detection or select a city manually.</p>
                </div>
              </div>)}
            {!loadingCities && (<div className="hidden">
                <select id="cityId" name="cityId" required value={formData.cityId} onChange={handleChange}>
                  <option value="">Select a city</option>
                  {cities.map((city) => (<option key={city.id} value={city.id.toString()}>
                      {city.name} (ID: {city.id})
                    </option>))}
                </select>
              </div>)}

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label htmlFor="nature" className="block text-sm font-medium text-gray-700 mb-1">
                  Nature (Description)
                </label>
                <input id="nature" name="nature" type="text" value={formData.nature} onChange={handleChange} placeholder="Description of contents" className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"/>
              </div>

              <div>
                <label htmlFor="fragile" className="block text-sm font-medium text-gray-700 mb-1">
                  Fragile (1=Yes, 0=No)
                </label>
                <select id="fragile" name="fragile" value={formData.fragile} onChange={handleChange} className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm">
                  <option value={0}>No</option>
                  <option value={1}>Yes</option>
                </select>
              </div>
            </div>

            <div>
              <label htmlFor="note" className="block text-sm font-medium text-gray-700 mb-1">
                Note (Special Instructions)
              </label>
              <textarea id="note" name="note" rows={2} value={formData.note} onChange={handleChange} placeholder="Special delivery instructions" className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"/>
            </div>

            <div className="flex items-center justify-end space-x-3 pt-4">
              <button type="button" onClick={onClose} className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50">
                Cancel
              </button>
              <button type="submit" disabled={loading || loadingCities} className="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-md hover:bg-primary-700 disabled:opacity-50 flex items-center">
                {loading ? (<>
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                    Sending...
                  </>) : (<>
                    <Truck className="h-4 w-4 mr-2"/>
                    Send to Shipping
                  </>)}
              </button>
            </div>
          </form>)}

        {result && result.failureCount > 0 && (<div className="flex items-center justify-end space-x-3 pt-4 border-t border-gray-200">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50">
              Close
            </button>
            <button type="button" onClick={() => {
                setResult(null);
                setError('');
            }} className="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-md hover:bg-primary-700">
              Try Again
            </button>
          </div>)}
      </div>
    </div>);
}

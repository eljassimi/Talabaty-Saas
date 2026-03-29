import { useState } from 'react';
import { X } from 'lucide-react';
import { orderService, Order, OrderStatus, UpdateStatusRequest, SendToShippingRequest } from '../services/orderService';
import { useStoreColor } from '../hooks/useStoreColor';
import { findCityId } from '../utils/cityMapping';
import { cityExistsInDeliveryPlatform } from '../utils/deliveryCities';
interface UpdateOrderStatusModalProps {
    order: Order;
    onClose: () => void;
    onSuccess: () => void;
}
const statusOptions: OrderStatus[] = ['ENCOURS', 'CONFIRMED', 'CONCLED', 'APPEL_1', 'APPEL_2'];
const statusLabels: Record<OrderStatus, string> = {
    'ENCOURS': 'En cours',
    'CONFIRMED': 'Confirmed',
    'CONCLED': 'Conclu',
    'APPEL_1': 'Appel 1',
    'APPEL_2': 'Appel 2'
};
export default function UpdateOrderStatusModal({ order, onClose, onSuccess }: UpdateOrderStatusModalProps) {
    const { storeColor } = useStoreColor();
    const [status, setStatus] = useState<OrderStatus>(order.status);
    const [note, setNote] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        try {
            const request: UpdateStatusRequest = {
                status,
                note: note.trim() || undefined,
            };
            await orderService.updateOrderStatus(order.id, request);
            if (status === 'CONFIRMED' && order.status !== 'CONFIRMED') {
                try {
                    if (!order.city || order.city === 'N/A' || !cityExistsInDeliveryPlatform(order.city)) {
                        console.log(`City "${order.city || 'N/A'}" not found in delivery platform, skipping auto-ship`);
                        return;
                    }
                    const cityId = order.city ? findCityId(order.city) : null;
                    if (cityId) {
                        const shippingData: SendToShippingRequest = {
                            cityId: cityId.toString(),
                            stock: 0,
                            open: 1,
                            fragile: 0,
                            replace: 0,
                            note: note.trim() || undefined,
                        };
                        await orderService.sendOrderToShipping(order.id, shippingData);
                        await new Promise(resolve => setTimeout(resolve, 500));
                    }
                }
                catch (shippingError: any) {
                    console.error('Failed to automatically send to shipping:', shippingError);
                }
            }
            onSuccess();
            onClose();
        }
        catch (err: any) {
            setError(err.response?.data?.message || err.message || 'Failed to update order status');
        }
        finally {
            setLoading(false);
        }
    };
    return (<div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-2xl max-w-md w-full mx-4 max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
          <h2 className="text-xl font-bold text-gray-900">Modifier le statut</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition-colors">
            <X className="h-6 w-6"/>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {error && (<div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              {error}
            </div>)}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Statut *
            </label>
            <select value={status} onChange={(e) => setStatus(e.target.value as OrderStatus)} className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:border-transparent transition-all" style={{
            '--tw-ring-color': storeColor,
        } as React.CSSProperties & {
            '--tw-ring-color': string;
        }} onFocus={(e) => {
            e.currentTarget.style.borderColor = storeColor;
            e.currentTarget.style.boxShadow = `0 0 0 2px ${storeColor}40`;
        }} onBlur={(e) => {
            e.currentTarget.style.borderColor = '';
            e.currentTarget.style.boxShadow = '';
        }} required>
              {statusOptions.map((s) => (<option key={s} value={s}>
                  {statusLabels[s] || s}
                </option>))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Note (optionnel)
            </label>
            <textarea value={note} onChange={(e) => setNote(e.target.value)} rows={3} className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:border-transparent transition-all resize-none" style={{
            '--tw-ring-color': storeColor,
        } as React.CSSProperties & {
            '--tw-ring-color': string;
        }} onFocus={(e) => {
            e.currentTarget.style.borderColor = storeColor;
            e.currentTarget.style.boxShadow = `0 0 0 2px ${storeColor}40`;
        }} onBlur={(e) => {
            e.currentTarget.style.borderColor = '';
            e.currentTarget.style.boxShadow = '';
        }} placeholder="Ajouter une note..."/>
          </div>

          <div className="flex items-center justify-end space-x-3 pt-4 border-t border-gray-200">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors">
              Annuler
            </button>
            <button type="submit" disabled={loading} className="px-4 py-2 text-sm font-medium text-white rounded-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed" style={{ backgroundColor: storeColor }} onMouseEnter={(e) => {
            if (!loading) {
                const rgb = hexToRgb(storeColor);
                e.currentTarget.style.backgroundColor = `rgb(${Math.max(0, rgb.r - 20)}, ${Math.max(0, rgb.g - 20)}, ${Math.max(0, rgb.b - 20)})`;
            }
        }} onMouseLeave={(e) => {
            e.currentTarget.style.backgroundColor = storeColor;
        }}>
              {loading ? 'Modification...' : 'Modifier'}
            </button>
          </div>
        </form>
      </div>
    </div>);
}
function hexToRgb(hex: string) {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
    } : { r: 2, g: 132, b: 199 };
}

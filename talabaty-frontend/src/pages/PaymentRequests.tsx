import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { supportService, type PaymentRequestDto } from '../services/supportService';
import { getPermissions } from '../utils/permissions';
import { Banknote, Check, X, Loader2 } from 'lucide-react';
export default function PaymentRequests() {
    const { user } = useAuth();
    const [requests, setRequests] = useState<PaymentRequestDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [processingId, setProcessingId] = useState<string | null>(null);
    const [note, setNote] = useState('');
    const [error, setError] = useState<string | null>(null);
    const permissions = getPermissions(user?.role);
    useEffect(() => {
        if (!permissions.canManagePaymentRequests)
            return;
        supportService
            .getAllPaymentRequests()
            .then(setRequests)
            .catch(() => setError('Failed to load payment requests'))
            .finally(() => setLoading(false));
    }, [permissions.canManagePaymentRequests]);
    const handleMarkPaid = async (id: string) => {
        setProcessingId(id);
        setError(null);
        try {
            const updated = await supportService.markAsPaid(id, note || undefined);
            setRequests((prev) => prev.map((r) => (r.id === id ? updated : r)));
            setNote('');
        }
        catch (e: unknown) {
            const err = e as {
                response?: {
                    data?: {
                        error?: string;
                    };
                };
                message?: string;
            };
            setError(err.response?.data?.error || err.message || 'Failed to mark as paid');
        }
        finally {
            setProcessingId(null);
        }
    };
    const handleReject = async (id: string) => {
        setProcessingId(id);
        setError(null);
        try {
            const updated = await supportService.rejectRequest(id, note || undefined);
            setRequests((prev) => prev.map((r) => (r.id === id ? updated : r)));
            setNote('');
        }
        catch (e: unknown) {
            const err = e as {
                response?: {
                    data?: {
                        error?: string;
                    };
                };
                message?: string;
            };
            setError(err.response?.data?.error || err.message || 'Failed to reject');
        }
        finally {
            setProcessingId(null);
        }
    };
    if (!permissions.canManagePaymentRequests) {
        return (<div className="p-6">
        <p className="text-gray-500">You don&apos;t have permission to view payment requests.</p>
      </div>);
    }
    if (loading) {
        return (<div className="flex items-center justify-center p-12">
        <Loader2 className="h-8 w-8 animate-spin text-gray-400"/>
      </div>);
    }
    return (<div className="p-6 max-w-4xl mx-auto">
      <h1 className="text-2xl font-semibold text-gray-900 dark:text-white flex items-center gap-2">
        <Banknote className="h-7 w-7"/>
        Payment requests
      </h1>
      <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
        Support team payment requests. Mark as paid when you have sent the payment, or reject with a note.
      </p>

      {error && (<div className="mt-4 p-3 rounded-lg bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300 text-sm">
          {error}
        </div>)}

      <div className="mt-6">
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Note for next action (optional)</label>
        <input type="text" value={note} onChange={(e) => setNote(e.target.value)} placeholder="e.g. Paid via bank transfer" className="w-full max-w-md rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#222328] px-4 py-2 text-gray-900 dark:text-white"/>
      </div>

      <div className="mt-8 overflow-x-auto">
        {requests.length === 0 ? (<p className="text-gray-500">No payment requests.</p>) : (<table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead>
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Support</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Store</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Amount</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Status</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Requested</th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              {requests.map((r) => (<tr key={r.id}>
                  <td className="px-4 py-3 text-sm text-gray-900 dark:text-white">{r.userName ?? '—'}</td>
                  <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-300">{r.storeName ?? '—'}</td>
                  <td className="px-4 py-3 text-sm font-medium text-gray-900 dark:text-white">
                    {Number(r.amountRequested).toFixed(2)} MAD
                  </td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${r.status === 'PENDING'
                    ? 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300'
                    : r.status === 'PAID'
                        ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300'
                        : 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300'}`}>
                      {r.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">
                    {r.requestedAt ? new Date(r.requestedAt).toLocaleString() : '—'}
                  </td>
                  <td className="px-4 py-3 text-right">
                    {r.status === 'PENDING' && (<span className="flex justify-end gap-2">
                        <button onClick={() => handleMarkPaid(r.id)} disabled={!!processingId} className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-green-600 text-white text-sm font-medium hover:bg-green-700 disabled:opacity-50">
                          {processingId === r.id ? <Loader2 className="h-4 w-4 animate-spin"/> : <Check className="h-4 w-4"/>}
                          Mark paid
                        </button>
                        <button onClick={() => handleReject(r.id)} disabled={!!processingId} className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-gray-200 dark:bg-gray-600 text-gray-800 dark:text-white text-sm font-medium hover:bg-gray-300 dark:hover:bg-gray-500 disabled:opacity-50">
                          {processingId === r.id ? <Loader2 className="h-4 w-4 animate-spin"/> : <X className="h-4 w-4"/>}
                          Reject
                        </button>
                      </span>)}
                  </td>
                </tr>))}
            </tbody>
          </table>)}
      </div>
    </div>);
}

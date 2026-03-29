import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { supportService, type SupportBalance, type PaymentRequestDto } from '../services/supportService';
import { getPermissions } from '../utils/permissions';
import { Wallet, Send, Loader2 } from 'lucide-react';
export default function Earnings() {
    const { user } = useAuth();
    const [balance, setBalance] = useState<SupportBalance | null>(null);
    const [requests, setRequests] = useState<PaymentRequestDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [requesting, setRequesting] = useState(false);
    const [requestAmount, setRequestAmount] = useState('');
    const [error, setError] = useState<string | null>(null);
    const storeId = user?.selectedStoreId;
    const permissions = getPermissions(user?.role);
    const isSupport = user?.role === 'SUPPORT';
    useEffect(() => {
        if (!storeId) {
            setLoading(false);
            return;
        }
        let cancelled = false;
        setLoading(true);
        Promise.all([
            supportService.getBalance(storeId),
            supportService.getMyPaymentRequests(storeId),
        ])
            .then(([bal, reqs]) => {
            if (!cancelled) {
                setBalance(bal);
                setRequests(reqs);
            }
        })
            .catch((e) => {
            if (!cancelled)
                setError(e.response?.data?.error || e.message || 'Failed to load');
        })
            .finally(() => {
            if (!cancelled)
                setLoading(false);
        });
        return () => { cancelled = true; };
    }, [storeId]);
    const handleRequestPayment = async () => {
        if (!storeId || !requestAmount)
            return;
        const amount = parseFloat(requestAmount);
        if (isNaN(amount) || amount <= 0) {
            setError('Enter a valid positive amount');
            return;
        }
        setRequesting(true);
        setError(null);
        try {
            await supportService.requestPayment(storeId, amount);
            setRequestAmount('');
            const [bal, reqs] = await Promise.all([
                supportService.getBalance(storeId),
                supportService.getMyPaymentRequests(storeId),
            ]);
            setBalance(bal);
            setRequests(reqs);
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
            setError(err.response?.data?.error || err.message || 'Request failed');
        }
        finally {
            setRequesting(false);
        }
    };
    if (!isSupport && !permissions.canManagePaymentRequests) {
        return (<div className="p-6">
        <p className="text-gray-500">You don&apos;t have access to earnings or payment requests.</p>
      </div>);
    }
    if (!storeId) {
        return (<div className="p-6">
        <p className="text-gray-500">Please select a store first.</p>
      </div>);
    }
    if (loading && !balance) {
        return (<div className="flex items-center justify-center p-12">
        <Loader2 className="h-8 w-8 animate-spin text-gray-400"/>
      </div>);
    }
    const balanceNum = balance ? Number(balance.balance) : 0;
    const totalEarned = balance ? Number(balance.totalEarned) : 0;
    const totalPaid = balance ? Number(balance.totalPaid) : 0;
    return (<div className="max-w-2xl mx-auto p-6">
      <h1 className="text-2xl font-semibold text-gray-900 dark:text-white flex items-center gap-2">
        <Wallet className="h-7 w-7"/>
        My earnings
      </h1>
      <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
        Revenue from confirmed and delivered orders you handled. Request payment when you want to be paid.
      </p>

      <div className="mt-8 grid gap-6">
        <div className="bg-white dark:bg-[#2A2D35] rounded-xl border border-gray-200 dark:border-gray-700 p-6">
          <h2 className="text-sm font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Balance</h2>
          <p className="mt-2 text-3xl font-bold text-gray-900 dark:text-white">{balanceNum.toFixed(2)} MAD</p>
          <p className="mt-1 text-sm text-gray-500">
            Total earned: {totalEarned.toFixed(2)} MAD · Already paid: {totalPaid.toFixed(2)} MAD
          </p>
        </div>

        <div className="bg-white dark:bg-[#2A2D35] rounded-xl border border-gray-200 dark:border-gray-700 p-6">
          <h2 className="text-sm font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Request payment</h2>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-300">Request an amount up to your current balance.</p>
          {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
          <div className="mt-4 flex flex-wrap gap-3 items-center">
            <input type="number" min="0" step="0.01" placeholder="Amount (MAD)" value={requestAmount} onChange={(e) => setRequestAmount(e.target.value)} className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#222328] px-4 py-2 text-gray-900 dark:text-white"/>
            <button onClick={() => setRequestAmount(String(balanceNum))} className="text-sm text-blue-600 dark:text-blue-400 hover:underline">
              Use full balance
            </button>
            <button onClick={handleRequestPayment} disabled={requesting || balanceNum <= 0} className="inline-flex items-center gap-2 px-4 py-2 bg-gray-900 dark:bg-white text-white dark:text-gray-900 rounded-lg font-medium hover:opacity-90 disabled:opacity-50">
              {requesting ? <Loader2 className="h-4 w-4 animate-spin"/> : <Send className="h-4 w-4"/>}
              Request payment
            </button>
          </div>
        </div>

        <div className="bg-white dark:bg-[#2A2D35] rounded-xl border border-gray-200 dark:border-gray-700 p-6">
          <h2 className="text-sm font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">My payment requests</h2>
          {requests.length === 0 ? (<p className="mt-4 text-sm text-gray-500">No payment requests yet.</p>) : (<ul className="mt-4 space-y-3">
              {requests.map((r) => (<li key={r.id} className="flex justify-between items-center py-2 border-b border-gray-100 dark:border-gray-700 last:border-0">
                  <div>
                    <span className="font-medium">{Number(r.amountRequested).toFixed(2)} MAD</span>
                    <span className="ml-2 text-sm text-gray-500">
                      {new Date(r.requestedAt).toLocaleDateString()} · {r.status}
                    </span>
                  </div>
                </li>))}
            </ul>)}
        </div>
      </div>
    </div>);
}

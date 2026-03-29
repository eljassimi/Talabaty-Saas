import { useEffect, useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import { orderService, Order } from '../services/orderService';
import { useStoreColor } from '../hooks/useStoreColor';
import { Package, CheckCircle, Truck, Clock, XCircle, Plus, TrendingUp, } from 'lucide-react';
import StatCard from '../components/dashboard/StatCard';
import ChartCard from '../components/dashboard/ChartCard';
import OrdersChart from '../components/dashboard/OrdersChart';
import RevenueChart from '../components/dashboard/RevenueChart';
import OrdersByCityChart from '../components/dashboard/OrdersByCityChart';
import type { TrendDirection } from '../components/dashboard/StatCard';
import { BRAND_COLORS } from '../constants/brand';
import { getSelectedStoreIdFromStorage } from '../utils/selectedStore';
export default function Dashboard() {
    const { user } = useAuth();
    const activeStoreId = (user?.selectedStoreId && String(user.selectedStoreId).trim()) ||
        getSelectedStoreIdFromStorage() ||
        null;
    const { theme } = useTheme();
    const { storeColor } = useStoreColor();
    const isDark = theme === 'dark';
    const textPrimary = isDark ? '#F9FAFB' : '#111827';
    const textSecondary = isDark ? '#9CA3AF' : '#6B7280';
    const inputBg = isDark ? '#2A2D35' : '#FFFFFF';
    const inputBorder = isDark ? '#3d4048' : '#E5E7EB';
    const [orders, setOrders] = useState<Order[]>([]);
    const [loading, setLoading] = useState(true);
    const [dateRange, setDateRange] = useState(() => {
        const to = new Date();
        const from = new Date(to);
        from.setDate(from.getDate() - 14);
        return { from, to };
    });
    useEffect(() => {
        if (activeStoreId) {
            loadData(activeStoreId);
        }
        else {
            setLoading(false);
        }
    }, [activeStoreId]);
    const loadData = async (storeId: string) => {
        if (!storeId)
            return;
        try {
            const ordersData = await orderService.getOrdersByStore(storeId).catch(() => []);
            setOrders(ordersData);
        }
        catch (error) {
            console.error('Error loading dashboard data:', error);
        }
        finally {
            setLoading(false);
        }
    };
    const filteredOrders = useMemo(() => {
        return orders.filter((o) => {
            const d = new Date(o.createdAt);
            return d >= dateRange.from && d <= dateRange.to;
        });
    }, [orders, dateRange]);
    const stats = useMemo(() => {
        const total = filteredOrders.length;
        const confirmed = filteredOrders.filter((o) => o.status === 'CONFIRMED').length;
        const delivered = filteredOrders.filter((o) => o.status === 'CONCLED').length;
        const pending = filteredOrders.filter((o) => ['ENCOURS', 'APPEL_1', 'APPEL_2'].includes(o.status)).length;
        const canceled = filteredOrders.filter((o) => ['CANCELLED', 'CANCELED'].includes(o.status)).length;
        const revenue = filteredOrders.reduce((sum, o) => sum + (o.totalAmount ?? 0), 0);
        const successRate = total > 0 ? Math.round((delivered / total) * 100) : 0;
        const currency = orders[0]?.currency ?? 'MAD';
        return {
            totalOrders: total,
            confirmed,
            delivered,
            pending,
            canceled,
            revenue,
            successRate,
            currency,
        };
    }, [filteredOrders, orders]);
    const trendNeutral: {
        value: string;
        direction: TrendDirection;
    } = { value: '—', direction: 'neutral' };
    if (loading) {
        return (<div className="flex items-center justify-center min-h-[320px]">
        <div className="animate-spin rounded-full h-12 w-12 border-2 border-transparent" style={{
                borderTopColor: BRAND_COLORS.primary,
                borderRightColor: BRAND_COLORS.secondary,
            }}/>
      </div>);
    }
    return (<div className="space-y-6">
      
      <div className="flex flex-col gap-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold" style={{ color: textPrimary }}>Dashboard</h1>
            <p className="mt-1 text-sm" style={{ color: textSecondary }}>
              Store performance and key metrics
            </p>
          </div>
          <Link to="/orders" className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium text-white transition-opacity hover:opacity-90 shrink-0" style={{ backgroundColor: storeColor }}>
            <Plus className="h-4 w-4"/>
            New Order
          </Link>
        </div>
        
        <div className="flex items-center gap-2 text-sm" style={{ color: textSecondary }}>
          <span>Filter by creation time</span>
          <input type="date" value={dateRange.from.toISOString().slice(0, 10)} onChange={(e) => setDateRange((r) => ({ ...r, from: new Date(e.target.value) }))} className="h-9 px-3 rounded-lg border focus:outline-none focus:ring-2 focus:ring-offset-0" style={{
            backgroundColor: inputBg,
            borderColor: inputBorder,
            color: textPrimary,
            ['--tw-ring-color' as string]: storeColor,
        }}/>
          <span>to</span>
          <input type="date" value={dateRange.to.toISOString().slice(0, 10)} onChange={(e) => setDateRange((r) => ({ ...r, to: new Date(e.target.value) }))} className="h-9 px-3 rounded-lg border focus:outline-none focus:ring-2 focus:ring-offset-0" style={{
            backgroundColor: inputBg,
            borderColor: inputBorder,
            color: textPrimary,
            ['--tw-ring-color' as string]: storeColor,
        }}/>
        </div>
      </div>

      
      <div className="grid gap-5" style={{
            gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
        }}>
        <StatCard title="Orders" value={stats.totalOrders} subtitle="Total orders" trend={trendNeutral} icon={<Package className="h-5 w-5" style={{ color: storeColor }}/>}/>
        <StatCard title="Confirmed" value={stats.confirmed} subtitle="Confirmed orders" trend={trendNeutral} icon={<CheckCircle className="h-5 w-5 text-emerald-600"/>}/>
        <StatCard title="Delivered" value={stats.delivered} subtitle="Completed deliveries" trend={trendNeutral} icon={<Truck className="h-5 w-5 text-blue-600"/>}/>
        <StatCard title="Pending" value={stats.pending} subtitle="In progress" trend={trendNeutral} icon={<Clock className="h-5 w-5 text-amber-600"/>}/>
        <StatCard title="Canceled" value={stats.canceled} subtitle="Canceled orders" trend={trendNeutral} icon={<XCircle className="h-5 w-5 text-red-600"/>}/>
        <StatCard title="Success Rate" value={`${stats.successRate}%`} subtitle="Delivered / total" trend={trendNeutral} icon={<TrendingUp className="h-5 w-5" style={{ color: storeColor }}/>}/>
        <StatCard title="Revenue" value={`${stats.revenue.toFixed(2)} ${stats.currency}`} subtitle="Total revenue" trend={trendNeutral} icon={<Package className="h-5 w-5" style={{ color: storeColor }}/>}/>
      </div>

      
      <div className="grid gap-5" style={{
            gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
        }}>
        <ChartCard title="Orders" subtitle="By day in selected range">
          <OrdersChart orders={filteredOrders} brandColor={storeColor} days={14} isDark={isDark}/>
        </ChartCard>
        <ChartCard title="Revenue" subtitle={`${stats.currency} in selected range`}>
          <RevenueChart orders={filteredOrders} brandColor={storeColor} days={14} isDark={isDark}/>
        </ChartCard>
        <ChartCard title="Orders by city" subtitle="Distribution">
          <OrdersByCityChart orders={filteredOrders} brandColor={storeColor} isDark={isDark}/>
        </ChartCard>
      </div>
    </div>);
}

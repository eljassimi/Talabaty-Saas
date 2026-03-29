import { TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
export type TrendDirection = 'up' | 'down' | 'neutral';
interface StatCardProps {
    title: string;
    value: string | number;
    subtitle?: string;
    trend?: {
        value: string;
        direction: TrendDirection;
    };
    icon?: React.ReactNode;
}
export default function StatCard({ title, value, subtitle, trend, icon }: StatCardProps) {
    const { theme } = useTheme();
    const isDark = theme === 'dark';
    const trendColors = {
        up: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-400',
        down: 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-400',
        neutral: 'bg-gray-200 text-gray-600 dark:bg-[#3d4048] dark:text-gray-400',
    };
    const TrendIcon = trend?.direction === 'up' ? TrendingUp : trend?.direction === 'down' ? TrendingDown : Minus;
    return (<div className="rounded-[12px] border p-5 shadow-[0_1px_3px_rgba(0,0,0,0.06)] hover:shadow-[0_4px_12px_rgba(0,0,0,0.08)] transition-shadow duration-200" style={{
            backgroundColor: isDark ? '#2A2D35' : '#FFFFFF',
            borderColor: isDark ? '#3d4048' : '#E5E7EB',
            boxShadow: isDark ? 'none' : undefined,
        }}>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <p className="text-sm font-medium truncate" style={{ color: isDark ? '#9CA3AF' : '#6B7280' }}>{title}</p>
          <p className="mt-1 text-2xl font-bold tabular-nums tracking-tight" style={{ color: isDark ? '#F9FAFB' : '#111827' }}>{value}</p>
          {subtitle && (<p className="mt-1 text-xs truncate" style={{ color: isDark ? '#9CA3AF' : '#6B7280' }}>{subtitle}</p>)}
        </div>
        <div className="flex items-center gap-2 shrink-0">
          {trend != null && (<span className={`inline-flex items-center gap-1 px-2 py-1 rounded-md text-xs font-medium ${trendColors[trend.direction]}`}>
              <TrendIcon className="h-3.5 w-3.5"/>
              {trend.value}
            </span>)}
          {icon && (<div className="h-10 w-10 rounded-lg flex items-center justify-center" style={{
                backgroundColor: isDark ? '#3d4048' : '#F3F4F6',
                color: isDark ? '#D1D5DB' : '#4B5563',
            }}>
              {icon}
            </div>)}
        </div>
      </div>
    </div>);
}

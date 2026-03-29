import { ReactNode } from 'react';
import { useTheme } from '../../contexts/ThemeContext';
interface ChartCardProps {
    title: string;
    subtitle?: string;
    children: ReactNode;
    className?: string;
}
export default function ChartCard({ title, subtitle, children, className = '' }: ChartCardProps) {
    const { theme } = useTheme();
    const isDark = theme === 'dark';
    return (<div className={`rounded-[12px] border p-5 shadow-[0_1px_3px_rgba(0,0,0,0.06)] overflow-hidden ${className}`} style={{
            backgroundColor: isDark ? '#2A2D35' : '#FFFFFF',
            borderColor: isDark ? '#3d4048' : '#E5E7EB',
            boxShadow: isDark ? 'none' : undefined,
        }}>
      <div className="mb-4">
        <h3 className="text-sm font-semibold" style={{ color: isDark ? '#F9FAFB' : '#111827' }}>{title}</h3>
        {subtitle && <p className="mt-0.5 text-xs" style={{ color: isDark ? '#9CA3AF' : '#6B7280' }}>{subtitle}</p>}
      </div>
      <div className="min-h-[200px] flex items-center justify-center">{children}</div>
    </div>);
}

import { TrendingUp, TrendingDown, Minus } from 'lucide-react'

export type TrendDirection = 'up' | 'down' | 'neutral'

interface StatCardProps {
  title: string
  value: string | number
  subtitle?: string
  trend?: {
    value: string
    direction: TrendDirection
  }
  icon?: React.ReactNode
}

export default function StatCard({ title, value, subtitle, trend, icon }: StatCardProps) {
  const trendColors = {
    up: 'bg-emerald-100 text-emerald-700',
    down: 'bg-red-100 text-red-700',
    neutral: 'bg-[#E6E8EC] text-[#6B7280]',
  }

  const TrendIcon = trend?.direction === 'up' ? TrendingUp : trend?.direction === 'down' ? TrendingDown : Minus

  return (
    <div className="bg-white rounded-[12px] border border-[#E6E8EC] p-5 shadow-[0_1px_3px_rgba(0,0,0,0.06)] hover:shadow-[0_4px_12px_rgba(0,0,0,0.08)] transition-shadow duration-200">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <p className="text-sm font-medium text-[#6B7280] truncate">{title}</p>
          <p className="mt-1 text-2xl font-bold text-[#111827] tabular-nums tracking-tight">{value}</p>
          {subtitle && (
            <p className="mt-1 text-xs text-[#6B7280] truncate">{subtitle}</p>
          )}
        </div>
        <div className="flex items-center gap-2 shrink-0">
          {trend != null && (
            <span
              className={`inline-flex items-center gap-1 px-2 py-1 rounded-md text-xs font-medium ${trendColors[trend.direction]}`}
            >
              <TrendIcon className="h-3.5 w-3.5" />
              {trend.value}
            </span>
          )}
          {icon && (
            <div className="h-10 w-10 rounded-lg bg-[#F6F8FB] flex items-center justify-center text-[#6B7280]">
              {icon}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

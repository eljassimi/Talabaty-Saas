import { ReactNode } from 'react'

interface ChartCardProps {
  title: string
  subtitle?: string
  children: ReactNode
  className?: string
}

export default function ChartCard({ title, subtitle, children, className = '' }: ChartCardProps) {
  return (
    <div
      className={`bg-white rounded-[12px] border border-[#E6E8EC] p-5 shadow-[0_1px_3px_rgba(0,0,0,0.06)] overflow-hidden ${className}`}
    >
      <div className="mb-4">
        <h3 className="text-sm font-semibold text-[#111827]">{title}</h3>
        {subtitle && <p className="mt-0.5 text-xs text-[#6B7280]">{subtitle}</p>}
      </div>
      <div className="min-h-[200px] flex items-center justify-center">{children}</div>
    </div>
  )
}

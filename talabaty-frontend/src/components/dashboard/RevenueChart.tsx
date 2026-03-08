import { useMemo } from 'react'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  type ChartOptions,
} from 'chart.js'
import { Bar } from 'react-chartjs-2'
import type { Order } from '../../services/orderService'

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend)

interface RevenueChartProps {
  orders: Order[]
  brandColor: string
  days?: number
}

function groupRevenueByDay(
  orders: Order[],
  days: number
): { labels: string[]; values: number[] } {
  const now = new Date()
  const start = new Date(now)
  start.setDate(start.getDate() - days)
  start.setHours(0, 0, 0, 0)

  const map = new Map<string, number>()
  for (let d = 0; d <= days; d++) {
    const date = new Date(start)
    date.setDate(date.getDate() + d)
    const key = date.toISOString().slice(0, 10)
    map.set(key, 0)
  }

  orders.forEach((o) => {
    const key = new Date(o.createdAt).toISOString().slice(0, 10)
    if (map.has(key)) {
      const current = map.get(key) ?? 0
      map.set(key, current + (o.totalAmount ?? 0))
    }
  })

  const sorted = [...map.entries()].sort((a, b) => a[0].localeCompare(b[0]))
  return {
    labels: sorted.map(([date]) => {
      const d = new Date(date)
      return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
    }),
    values: sorted.map(([, sum]) => sum),
  }
}

export default function RevenueChart({ orders, brandColor, days = 14 }: RevenueChartProps) {
  const { labels, values } = useMemo(
    () => groupRevenueByDay(orders, days),
    [orders, days]
  )

  const data = useMemo(
    () => ({
      labels,
      datasets: [
        {
          label: 'Revenue',
          data: values,
          backgroundColor: brandColor,
          borderRadius: 6,
        },
      ],
    }),
    [labels, values, brandColor]
  )

  const options: ChartOptions<'bar'> = useMemo(
    () => ({
      responsive: true,
      maintainAspectRatio: true,
      aspectRatio: 2,
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: '#fff',
          titleColor: '#111827',
          bodyColor: '#6B7280',
          borderColor: '#E6E8EC',
          borderWidth: 1,
          callbacks: {
            label: (ctx) => {
              const v = ctx.raw as number
              return `${Number(v).toFixed(2)} ${orders[0]?.currency ?? 'MAD'}`
            },
          },
        },
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: { color: '#6B7280', font: { size: 11 } },
        },
        y: {
          beginAtZero: true,
          grid: { color: '#F6F8FB' },
          ticks: { color: '#6B7280', font: { size: 11 } },
        },
      },
    }),
    [orders]
  )

  if (labels.length === 0) {
    return (
      <div className="flex items-center justify-center h-[200px] text-[#6B7280] text-sm">
        No revenue data for this period
      </div>
    )
  }

  return <Bar data={data} options={options} />
}

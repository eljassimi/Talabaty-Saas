import { useMemo } from 'react'
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  type ChartOptions,
} from 'chart.js'
import { Doughnut } from 'react-chartjs-2'
import type { Order } from '../../services/orderService'

ChartJS.register(ArcElement, Tooltip, Legend)

interface OrdersByCityChartProps {
  orders: Order[]
  brandColor: string
}

function getCityCounts(orders: Order[]): { label: string; count: number }[] {
  const map = new Map<string, number>()
  orders.forEach((o) => {
    const city = (o.city && o.city.trim()) || 'Unknown'
    map.set(city, (map.get(city) ?? 0) + 1)
  })
  return [...map.entries()]
    .map(([label, count]) => ({ label, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 8)
}

// Generate shades from brand color for multiple segments
function colorShades(hex: string, count: number): string[] {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  if (!result) return [hex]
  let r = parseInt(result[1], 16)
  let g = parseInt(result[2], 16)
  let b = parseInt(result[3], 16)
  const out: string[] = []
  for (let i = 0; i < count; i++) {
    const factor = 1 - (i * 0.12)
    out.push(
      `rgb(${Math.round(r * factor)}, ${Math.round(g * factor)}, ${Math.round(b * factor)})`
    )
  }
  return out
}

export default function OrdersByCityChart({ orders, brandColor }: OrdersByCityChartProps) {
  const cityData = useMemo(() => getCityCounts(orders), [orders])
  const colors = useMemo(() => colorShades(brandColor, Math.max(cityData.length, 1)), [brandColor, cityData.length])

  const data = useMemo(
    () => ({
      labels: cityData.map((c) => c.label),
      datasets: [
        {
          data: cityData.map((c) => c.count),
          backgroundColor: colors,
          borderWidth: 0,
        },
      ],
    }),
    [cityData, colors]
  )

  const options: ChartOptions<'doughnut'> = useMemo(
    () => ({
      responsive: true,
      maintainAspectRatio: true,
      cutout: '65%',
      plugins: {
        legend: {
          position: 'bottom',
          labels: { color: '#6B7280', font: { size: 11 }, padding: 12 },
        },
        tooltip: {
          backgroundColor: '#fff',
          titleColor: '#111827',
          bodyColor: '#6B7280',
          borderColor: '#E6E8EC',
          borderWidth: 1,
        },
      },
    }),
    []
  )

  if (cityData.length === 0) {
    return (
      <div className="flex items-center justify-center h-[200px] text-[#6B7280] text-sm">
        No data
      </div>
    )
  }

  return (
    <div className="h-[240px] flex items-center justify-center">
      <Doughnut data={data} options={options} />
    </div>
  )
}

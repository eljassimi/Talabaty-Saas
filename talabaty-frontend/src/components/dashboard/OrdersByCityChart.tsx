import { useMemo } from 'react';
import { Chart as ChartJS, ArcElement, Tooltip, Legend, type ChartOptions, } from 'chart.js';
import { Doughnut } from 'react-chartjs-2';
import type { Order } from '../../services/orderService';
ChartJS.register(ArcElement, Tooltip, Legend);
interface OrdersByCityChartProps {
    orders: Order[];
    brandColor: string;
    isDark?: boolean;
}
function getCityCounts(orders: Order[]): {
    label: string;
    count: number;
}[] {
    const map = new Map<string, number>();
    orders.forEach((o) => {
        const city = (o.city && o.city.trim()) || 'Unknown';
        map.set(city, (map.get(city) ?? 0) + 1);
    });
    return [...map.entries()]
        .map(([label, count]) => ({ label, count }))
        .sort((a, b) => b.count - a.count)
        .slice(0, 8);
}
function colorShades(hex: string, count: number, isDark?: boolean): string[] {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    if (!result)
        return [hex];
    let r = parseInt(result[1], 16);
    let g = parseInt(result[2], 16);
    let b = parseInt(result[3], 16);
    const out: string[] = [];
    const minFactor = isDark ? 0.7 : 0.4;
    for (let i = 0; i < count; i++) {
        const factor = 1 - (i * (isDark ? 0.06 : 0.12));
        const f = Math.max(minFactor, factor);
        const clamp = (x: number) => Math.min(255, Math.max(0, Math.round(x)));
        out.push(`rgb(${clamp(r * f)}, ${clamp(g * f)}, ${clamp(b * f)})`);
    }
    return out;
}
export default function OrdersByCityChart({ orders, brandColor, isDark = false }: OrdersByCityChartProps) {
    const cityData = useMemo(() => getCityCounts(orders), [orders]);
    const colors = useMemo(() => colorShades(brandColor, Math.max(cityData.length, 1), isDark), [brandColor, cityData.length, isDark]);
    const data = useMemo(() => ({
        labels: cityData.map((c) => c.label),
        datasets: [
            {
                data: cityData.map((c) => c.count),
                backgroundColor: colors,
                borderWidth: 0,
            },
        ],
    }), [cityData, colors]);
    const legendColor = isDark ? '#D1D5DB' : '#6B7280';
    const options: ChartOptions<'doughnut'> = useMemo(() => ({
        responsive: true,
        maintainAspectRatio: true,
        cutout: '65%',
        plugins: {
            legend: {
                position: 'bottom',
                labels: { color: legendColor, font: { size: 11 }, padding: 12 },
                usePointStyle: true,
            },
            tooltip: {
                backgroundColor: isDark ? '#2A2D35' : '#fff',
                titleColor: isDark ? '#F3F4F6' : '#111827',
                bodyColor: isDark ? '#D1D5DB' : '#6B7280',
                borderColor: isDark ? '#3d4048' : '#E6E8EC',
                borderWidth: 1,
            },
        },
    }), [isDark, legendColor]);
    if (cityData.length === 0) {
        return (<div className="flex items-center justify-center h-[200px] text-gray-500 dark:text-gray-400 text-sm">
        No data
      </div>);
    }
    return (<div className="h-[240px] flex items-center justify-center">
      <Doughnut data={data} options={options}/>
    </div>);
}

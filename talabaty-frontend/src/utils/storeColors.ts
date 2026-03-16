// Utility functions for store color styling
import type React from 'react'

export function getButtonStyle(color: string, variant: 'primary' | 'secondary' | 'outline' = 'primary') {
  const styles: Record<string, React.CSSProperties> = {
    primary: {
      backgroundColor: color,
      color: '#ffffff',
      borderColor: color,
    },
    secondary: {
      backgroundColor: `${color}15`,
      color: color,
      borderColor: color,
    },
    outline: {
      backgroundColor: 'transparent',
      color: color,
      borderColor: color,
    },
  }
  return styles[variant]
}

export function getHoverStyle(color: string, variant: 'primary' | 'secondary' | 'outline' = 'primary') {
  const rgb = hexToRgb(color)
  const styles: Record<string, React.CSSProperties> = {
    primary: {
      backgroundColor: darkenColor(color, 10),
    },
    secondary: {
      backgroundColor: `${color}25`,
    },
    outline: {
      backgroundColor: `${color}10`,
    },
  }
  return styles[variant]
}

function hexToRgb(hex: string) {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : { r: 2, g: 132, b: 199 }
}

function darkenColor(hex: string, percent: number) {
  const rgb = hexToRgb(hex)
  const r = Math.max(0, Math.min(255, Math.floor(rgb.r * (1 - percent / 100))))
  const g = Math.max(0, Math.min(255, Math.floor(rgb.g * (1 - percent / 100))))
  const b = Math.max(0, Math.min(255, Math.floor(rgb.b * (1 - percent / 100))))
  return `rgb(${r}, ${g}, ${b})`
}


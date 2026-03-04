import { ReactNode } from 'react'
import { useStoreColor } from '../hooks/useStoreColor'

interface ButtonProps {
  children: ReactNode
  onClick?: () => void
  type?: 'button' | 'submit' | 'reset'
  variant?: 'primary' | 'secondary' | 'outline'
  className?: string
  disabled?: boolean
  style?: React.CSSProperties
}

// Helper to convert hex to RGB
function hexToRgb(hex: string) {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : { r: 2, g: 132, b: 199 }
}

export default function Button({ 
  children, 
  onClick, 
  type = 'button', 
  variant = 'primary',
  className = '',
  disabled = false,
  style = {}
}: ButtonProps) {
  const { storeColor } = useStoreColor()

  const getButtonStyle = () => {
    const baseStyle: React.CSSProperties = {
      ...style,
    }

    if (variant === 'primary') {
      baseStyle.backgroundColor = storeColor
      baseStyle.color = '#ffffff'
      baseStyle.borderColor = storeColor
    } else if (variant === 'secondary') {
      baseStyle.backgroundColor = `${storeColor}15`
      baseStyle.color = storeColor
      baseStyle.borderColor = storeColor
    } else if (variant === 'outline') {
      baseStyle.backgroundColor = 'transparent'
      baseStyle.color = storeColor
      baseStyle.borderColor = storeColor
    }

    return baseStyle
  }

  const handleMouseEnter = (e: React.MouseEvent<HTMLButtonElement>) => {
    if (disabled) return
    const rgb = hexToRgb(storeColor)
    const darker = `rgb(${Math.max(0, rgb.r - 20)}, ${Math.max(0, rgb.g - 20)}, ${Math.max(0, rgb.b - 20)})`
    
    if (variant === 'primary') {
      e.currentTarget.style.backgroundColor = darker
    } else if (variant === 'secondary') {
      e.currentTarget.style.backgroundColor = `${storeColor}25`
    } else if (variant === 'outline') {
      e.currentTarget.style.backgroundColor = `${storeColor}10`
    }
  }

  const handleMouseLeave = (e: React.MouseEvent<HTMLButtonElement>) => {
    if (disabled) return
    const style = getButtonStyle()
    e.currentTarget.style.backgroundColor = style.backgroundColor as string
  }

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={className}
      style={getButtonStyle()}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      {children}
    </button>
  )
}


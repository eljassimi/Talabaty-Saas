import logo from '../images/talabaty-logo.svg'

interface TalabatyLogoSpinnerProps {
  /** Continuous spin (for loading states) */
  spinning?: boolean
  /** One-time rotation (for page transition). When true, plays once then resets. */
  rotateOnce?: boolean
  /** Size in pixels (width; height scales to maintain aspect). Default 40. */
  size?: number
  className?: string
}

export default function TalabatyLogoSpinner({
  spinning = false,
  rotateOnce = false,
  size = 40,
  className = '',
}: TalabatyLogoSpinnerProps) {
  // Logo aspect ~65:31
  const height = Math.round((size * 31) / 65)

  return (
    <div
      className={`
        inline-flex items-center justify-center flex-shrink-0
        ${spinning ? 'talabaty-logo-spin' : ''}
        ${rotateOnce ? 'talabaty-logo-rotate-once' : ''}
        ${className}
      `}
      style={{ width: size, height: size }}
      aria-hidden
    >
      <img
        src={logo}
        alt="Talabaty"
        className="max-w-full max-h-full w-auto h-auto object-contain"
        style={{ width: size, height }}
      />
    </div>
  )
}

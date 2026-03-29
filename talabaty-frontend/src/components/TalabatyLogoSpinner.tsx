import logo from '../images/talabaty-logo.svg';
interface TalabatyLogoSpinnerProps {
    spinning?: boolean;
    rotateOnce?: boolean;
    size?: number;
    className?: string;
}
export default function TalabatyLogoSpinner({ spinning = false, rotateOnce = false, size = 40, className = '', }: TalabatyLogoSpinnerProps) {
    const height = Math.round((size * 31) / 65);
    return (<div className={`
        inline-flex items-center justify-center flex-shrink-0
        ${spinning ? 'talabaty-logo-spin' : ''}
        ${rotateOnce ? 'talabaty-logo-rotate-once' : ''}
        ${className}
      `} style={{ width: size, height: size }} aria-hidden>
      <img src={logo} alt="Talabaty" className="max-w-full max-h-full w-auto h-auto object-contain" style={{ width: size, height }}/>
    </div>);
}

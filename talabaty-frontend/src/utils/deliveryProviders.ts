// Delivery provider configurations
import CathedisLogo from '../images/cathedis.svg'
import OzoneExpressLogo from '../images/ozone-express.svg'
import DigylogLogo from '../images/digylog.svg'
import AmeexLogo from '../images/ameex.svg'
import SendItLogo from '../images/send-it.svg'
import KargoExpressLogo from '../images/kargo-express.svg'
import OlivraisonLogo from '../images/olivraison.svg'
import SpeedafLogo from '../images/speedaf.svg'
import QuickLivraisonLogo from '../images/quick-livraison.svg'
import LivoLogo from '../images/local-delivery.svg' // Using local-delivery as placeholder for Livo
import OnesstaLogo from '../images/onessta.svg'

export interface ProviderConfig {
  name: string
  logo: string
  providerType: string
  displayName: string
}

export const DELIVERY_PROVIDERS: Record<string, ProviderConfig> = {
  'Cathedis': {
    name: 'Cathedis',
    logo: CathedisLogo,
    providerType: 'CATHEDIS',
    displayName: 'Cathedis',
  },
  'Ozone Express': {
    name: 'Ozone Express',
    logo: OzoneExpressLogo,
    providerType: 'OZON_EXPRESS',
    displayName: 'Ozon Express',
  },
  'Digylog': {
    name: 'Digylog',
    logo: DigylogLogo,
    providerType: 'DIGYLOG',
    displayName: 'Digylog',
  },
  'Ameex': {
    name: 'Ameex',
    logo: AmeexLogo,
    providerType: 'AMEEX',
    displayName: 'Ameex',
  },
  'Send it': {
    name: 'Send it',
    logo: SendItLogo,
    providerType: 'SEND_IT',
    displayName: 'Send it',
  },
  'KargoExpress': {
    name: 'KargoExpress',
    logo: KargoExpressLogo,
    providerType: 'KARGO_EXPRESS',
    displayName: 'KargoExpress',
  },
  'Olivraison': {
    name: 'Olivraison',
    logo: OlivraisonLogo,
    providerType: 'OLIVRAISON',
    displayName: 'Olivraison',
  },
  'Speedaf': {
    name: 'Speedaf',
    logo: SpeedafLogo,
    providerType: 'SPEEDAF',
    displayName: 'Speedaf',
  },
  'Quick Livraison': {
    name: 'Quick Livraison',
    logo: QuickLivraisonLogo,
    providerType: 'QUICK_LIVRAISON',
    displayName: 'Quick Livraison',
  },
  'Livo': {
    name: 'Livo',
    logo: LivoLogo,
    providerType: 'LIVO',
    displayName: 'Livo',
  },
  'Onessta': {
    name: 'Onessta',
    logo: OnesstaLogo,
    providerType: 'ONESSTA',
    displayName: 'Onessta',
  },
}

export function getProviderConfig(name: string): ProviderConfig | null {
  return DELIVERY_PROVIDERS[name] || null
}

export function getProviderLogo(providerType: string, displayName?: string): string | null {
  // Try to find by providerType first
  for (const config of Object.values(DELIVERY_PROVIDERS)) {
    if (config.providerType === providerType) {
      return config.logo
    }
  }
  
  // Try to find by displayName
  if (displayName) {
    for (const config of Object.values(DELIVERY_PROVIDERS)) {
      if (config.displayName.toLowerCase() === displayName.toLowerCase() || 
          config.name.toLowerCase() === displayName.toLowerCase()) {
        return config.logo
      }
    }
  }
  
  return null
}


import { useEffect, useState } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { storeService } from '../services/storeService'
import { BRAND_COLORS } from '../constants/brand'

export function useStoreColor() {
  const { user } = useAuth()
  const [storeColor, setStoreColor] = useState(BRAND_COLORS.primary) // Default brand color
  const [storeLogo, setStoreLogo] = useState<string | null>(null)
  const [storeName, setStoreName] = useState<string | null>(null)

  useEffect(() => {
    if (user?.selectedStoreId) {
      storeService.getStore(user.selectedStoreId)
        .then((store) => {
          setStoreColor(store.color || BRAND_COLORS.primary)
          setStoreLogo(store.logoUrl || null)
          setStoreName(store.name || null)
        })
        .catch(console.error)
    } else {
      setStoreColor(BRAND_COLORS.primary)
      setStoreLogo(null)
      setStoreName(null)
    }
  }, [user?.selectedStoreId])

  const hexToRgb = (hex: string) => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
    return result ? {
      r: parseInt(result[1], 16),
      g: parseInt(result[2], 16),
      b: parseInt(result[3], 16)
    } : { r: 18, g: 49, b: 51 } // Default to brand primary color RGB
  }

  return { storeColor, storeLogo, storeName, hexToRgb }
}


import citiesData from '../data/cities.json'

interface CityData {
  ID: number
  REF: string
  NAME: string
  'DELIVERED-PRICE': number
  'RETURNED-PRICE': number
  'REFUSED-PRICE': number
}

interface CitiesResponse {
  CITIES: Record<string, CityData>
}

const citiesResponse = citiesData as CitiesResponse

// Get all city names from the JSON file
const getAllCityNames = (): string[] => {
  const cities = citiesResponse.CITIES
  return Object.values(cities).map(city => city.NAME.toLowerCase().trim())
}

// Check if a city exists in the delivery platform
export const cityExistsInDeliveryPlatform = (cityName: string | null | undefined): boolean => {
  if (!cityName) return false
  
  const normalizedCity = cityName.toLowerCase().trim()
  const allCityNames = getAllCityNames()
  
  // Check for exact match
  if (allCityNames.includes(normalizedCity)) {
    return true
  }
  
  // Check for partial matches (city name contains or is contained in delivery city)
  return allCityNames.some(deliveryCity => 
    deliveryCity === normalizedCity ||
    deliveryCity.includes(normalizedCity) ||
    normalizedCity.includes(deliveryCity)
  )
}

// Get city ID by name (for backward compatibility with cityMapping)
export const getCityIdByName = (cityName: string | null | undefined): number | null => {
  if (!cityName) return null
  
  const normalizedCity = cityName.toLowerCase().trim()
  const cities = citiesResponse.CITIES
  
  // Try exact match first
  for (const city of Object.values(cities)) {
    if (city.NAME.toLowerCase().trim() === normalizedCity) {
      return city.ID
    }
  }
  
  // Try partial match
  for (const city of Object.values(cities)) {
    const deliveryCityName = city.NAME.toLowerCase().trim()
    if (deliveryCityName.includes(normalizedCity) || normalizedCity.includes(deliveryCityName)) {
      return city.ID
    }
  }
  
  return null
}


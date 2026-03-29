import citiesData from '../data/cities.json';
interface CityData {
    ID: number;
    REF: string;
    NAME: string;
    'DELIVERED-PRICE': number;
    'RETURNED-PRICE': number;
    'REFUSED-PRICE': number;
}
interface CitiesResponse {
    CITIES: Record<string, CityData>;
}
const citiesResponse = citiesData as CitiesResponse;
const getAllCityNames = (): string[] => {
    const cities = citiesResponse.CITIES;
    return Object.values(cities).map(city => city.NAME.toLowerCase().trim());
};
export const cityExistsInDeliveryPlatform = (cityName: string | null | undefined): boolean => {
    if (!cityName)
        return false;
    const normalizedCity = cityName.toLowerCase().trim();
    const allCityNames = getAllCityNames();
    if (allCityNames.includes(normalizedCity)) {
        return true;
    }
    return allCityNames.some(deliveryCity => deliveryCity === normalizedCity ||
        deliveryCity.includes(normalizedCity) ||
        normalizedCity.includes(deliveryCity));
};
export const getCityIdByName = (cityName: string | null | undefined): number | null => {
    if (!cityName)
        return null;
    const normalizedCity = cityName.toLowerCase().trim();
    const cities = citiesResponse.CITIES;
    for (const city of Object.values(cities)) {
        if (city.NAME.toLowerCase().trim() === normalizedCity) {
            return city.ID;
        }
    }
    for (const city of Object.values(cities)) {
        const deliveryCityName = city.NAME.toLowerCase().trim();
        if (deliveryCityName.includes(normalizedCity) || normalizedCity.includes(deliveryCityName)) {
            return city.ID;
        }
    }
    return null;
};

package ma.talabaty.talabaty.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to map city names to Ozon Express city IDs
 * This matches the cityMapping.ts file in the frontend
 */
public class CityMappingUtil {
    
    private static final Map<String, Integer> CITY_MAPPING = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        // Initialize city mapping (matching frontend cityMapping.ts)
        CITY_MAPPING.put("agadir", 37);
        CITY_MAPPING.put("ait melloul", 49);
        CITY_MAPPING.put("al hoceima", 55);
        CITY_MAPPING.put("safi", 61);
        CITY_MAPPING.put("beni mellal", 73);
        CITY_MAPPING.put("boujdour", 91);
        CITY_MAPPING.put("casablanca", 97);
        CITY_MAPPING.put("dakhla", 103);
        CITY_MAPPING.put("el jadida", 109);
        CITY_MAPPING.put("fes", 127);
        CITY_MAPPING.put("fès", 127); // Alternative spelling
        CITY_MAPPING.put("fnideq", 133);
        CITY_MAPPING.put("fquih ben salah", 139);
        CITY_MAPPING.put("inzegane", 151);
        CITY_MAPPING.put("kasba tadla", 157);
        CITY_MAPPING.put("khouribga", 169);
        CITY_MAPPING.put("larache", 187);
        CITY_MAPPING.put("m diq", 193);
        CITY_MAPPING.put("marrakech", 199);
        CITY_MAPPING.put("martil", 205);
        CITY_MAPPING.put("meknes", 211);
        CITY_MAPPING.put("nador", 217);
        CITY_MAPPING.put("ouarzazat", 223);
        CITY_MAPPING.put("oujda", 229);
        CITY_MAPPING.put("ain harouda", 235);
        CITY_MAPPING.put("cabo negro", 271);
        CITY_MAPPING.put("tanger", 289);
        CITY_MAPPING.put("tetouan", 313);
        CITY_MAPPING.put("azrou-ville", 327);
        CITY_MAPPING.put("ifran", 333);
        CITY_MAPPING.put("imouzar kandre", 339);
        CITY_MAPPING.put("mohammedia", 345);
        CITY_MAPPING.put("ain leuh", 364);
        CITY_MAPPING.put("sidi aadi - azrou", 370);
        CITY_MAPPING.put("tiznit", 376);
        CITY_MAPPING.put("taroudant", 382);
        CITY_MAPPING.put("errahma ville", 403);
        CITY_MAPPING.put("tamaris", 409);
        CITY_MAPPING.put("dar bouazza", 415);
        CITY_MAPPING.put("bouskoura", 421);
        CITY_MAPPING.put("jamaat shaim", 427);
        CITY_MAPPING.put("nouaceur", 433);
        CITY_MAPPING.put("sebt gzoula", 439);
        CITY_MAPPING.put("souiria", 445);
        CITY_MAPPING.put("sidi kacem", 457);
        CITY_MAPPING.put("sidi sliman", 463);
        CITY_MAPPING.put("bouznika", 472);
        CITY_MAPPING.put("tit mellil", 478);
        // Add more cities as needed - this is a subset, you may want to add all cities from cityMapping.ts
    }
    
    /**
     * Find city ID from city name (case-insensitive, with fuzzy matching)
     * @param cityName The city name to look up
     * @return The city ID if found, null otherwise
     */
    public static Integer findCityId(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            return null;
        }
        
        String normalized = cityName.toLowerCase().trim();
        
        // Direct match
        if (CITY_MAPPING.containsKey(normalized)) {
            return CITY_MAPPING.get(normalized);
        }
        
        // Try removing extra spaces and special characters
        normalized = normalized.replaceAll("\\s+", " ").trim();
        if (CITY_MAPPING.containsKey(normalized)) {
            return CITY_MAPPING.get(normalized);
        }
        
        // Try partial match (contains)
        for (Map.Entry<String, Integer> entry : CITY_MAPPING.entrySet()) {
            if (normalized.contains(entry.getKey()) || entry.getKey().contains(normalized)) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Extract city from order metadata and find its ID
     * @param metadata JSON metadata string
     * @return City ID if found, null otherwise
     */
    public static Integer findCityIdFromMetadata(String metadata) {
        if (metadata == null || metadata.trim().isEmpty()) {
            return null;
        }
        
        try {
            JsonNode metadataNode = objectMapper.readTree(metadata);
            if (metadataNode.has("city")) {
                String cityName = metadataNode.get("city").asText();
                return findCityId(cityName);
            }
        } catch (Exception e) {
            // If parsing fails, return null
        }
        
        return null;
    }
}


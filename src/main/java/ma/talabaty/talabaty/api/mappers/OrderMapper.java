package ma.talabaty.talabaty.api.mappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.talabaty.talabaty.api.dtos.OrderDto;
import ma.talabaty.talabaty.domain.orders.model.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderDto toDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId() != null ? order.getId().toString() : null);
        dto.setStoreId(order.getStore() != null ? order.getStore().getId().toString() : null);
        dto.setStoreName(order.getStore() != null ? order.getStore().getName() : null);
        dto.setSource(order.getSource());
        dto.setExternalOrderId(order.getExternalOrderId());
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerPhone(order.getCustomerPhone());
        dto.setDestinationAddress(order.getDestinationAddress());
        dto.setCity(order.getCity());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCurrency(order.getCurrency());
        dto.setMetadata(order.getMetadata());
        
        // Use direct fields first, fallback to metadata if not set
        dto.setProductName(order.getProductName());
        dto.setProductId(order.getProductId());
        
        // Extract city from metadata if not set directly (fallback for old orders)
        if (dto.getCity() == null && order.getMetadata() != null && !order.getMetadata().trim().isEmpty()) {
            try {
                JsonNode metadataNode = objectMapper.readTree(order.getMetadata());
                if (metadataNode.has("city")) {
                    dto.setCity(metadataNode.get("city").asText());
                }
                // Fallback: if productName/productId are not set directly, try metadata
                if (dto.getProductName() == null) {
                    if (metadataNode.has("productName")) {
                        dto.setProductName(metadataNode.get("productName").asText());
                    } else if (metadataNode.has("product_name")) {
                        dto.setProductName(metadataNode.get("product_name").asText());
                    } else if (metadataNode.has("product")) {
                        dto.setProductName(metadataNode.get("product").asText());
                    }
                }
                if (dto.getProductId() == null && metadataNode.has("productId")) {
                    dto.setProductId(metadataNode.get("productId").asText());
                } else if (dto.getProductId() == null && metadataNode.has("product_id")) {
                    dto.setProductId(metadataNode.get("product_id").asText());
                }
            } catch (Exception e) {
                // If metadata parsing fails, just continue without extracting fields
                // The raw metadata will still be available
            }
        }
        
        // Map assignedTo user
        if (order.getAssignedTo() != null) {
            dto.setAssignedToUserId(order.getAssignedTo().getId().toString());
            dto.setAssignedToName(order.getAssignedTo().getFirstName() + " " + order.getAssignedTo().getLastName());
        }
        
        // Map tracking number
        dto.setOzonTrackingNumber(order.getOzonTrackingNumber());
        
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        return dto;
    }
}


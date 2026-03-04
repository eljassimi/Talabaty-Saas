package ma.talabaty.talabaty.api.mappers;

import ma.talabaty.talabaty.api.dtos.StoreDto;
import ma.talabaty.talabaty.domain.stores.model.Store;
import org.springframework.stereotype.Component;

@Component
public class StoreMapper {

    public StoreDto toDto(Store store) {
        StoreDto dto = new StoreDto();
        dto.setId(store.getId() != null ? store.getId().toString() : null);
        dto.setName(store.getName());
        dto.setAccountId(store.getAccount() != null ? store.getAccount().getId().toString() : null);
        dto.setManagerId(store.getManager() != null ? store.getManager().getId().toString() : null);
        if (store.getManager() != null) {
            dto.setManagerName(store.getManager().getFirstName() + " " + store.getManager().getLastName());
        }
        dto.setStatus(store.getStatus());
        dto.setTimezone(store.getTimezone());
        dto.setLogoUrl(store.getLogoUrl());
        dto.setColor(store.getColor());
        dto.setCreatedAt(store.getCreatedAt());
        dto.setUpdatedAt(store.getUpdatedAt());
        return dto;
    }
}


package ma.talabaty.talabaty.api.mappers;

import ma.talabaty.talabaty.api.dtos.UserDto;
import ma.talabaty.talabaty.domain.users.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId() != null ? user.getId().toString() : null);
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setAccountId(user.getAccount() != null ? user.getAccount().getId().toString() : null);
        dto.setMustChangePassword(user.getMustChangePassword());
        dto.setSelectedStoreId(user.getSelectedStoreId() != null ? user.getSelectedStoreId().toString() : null);
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}


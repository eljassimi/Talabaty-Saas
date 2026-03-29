package ma.talabaty.talabaty.api.mappers;

import ma.talabaty.talabaty.api.dtos.TeamMemberDto;
import ma.talabaty.talabaty.domain.teams.model.StoreTeamMember;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

    public TeamMemberDto toDto(StoreTeamMember member) {
        TeamMemberDto dto = new TeamMemberDto();
        dto.setId(member.getId() != null ? member.getId().toString() : null);
        dto.setStoreId(member.getStore() != null ? member.getStore().getId().toString() : null);
        
        
        if (member.getUser() != null) {
            dto.setUserId(member.getUser().getId().toString());
            dto.setEmail(member.getUser().getEmail());
            dto.setFirstName(member.getUser().getFirstName());
            dto.setLastName(member.getUser().getLastName());
        }
        
        
        dto.setExternalMemberEmail(member.getExternalMemberEmail());
        
        dto.setRole(member.getRole());
        dto.setInvitationStatus(member.getInvitationStatus());
        
        if (member.getAddedBy() != null) {
            dto.setAddedBy(member.getAddedBy().getId().toString());
        }
        
        dto.setCreatedAt(member.getCreatedAt());
        dto.setUpdatedAt(member.getUpdatedAt());
        
        return dto;
    }
}


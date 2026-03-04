package ma.talabaty.talabaty.api.dtos;

import ma.talabaty.talabaty.domain.teams.model.StoreTeamRole;
import ma.talabaty.talabaty.domain.teams.model.TeamInvitationStatus;

import java.time.OffsetDateTime;

public class TeamMemberDto {
    private String id;
    private String storeId;
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String externalMemberEmail;
    private StoreTeamRole role;
    private TeamInvitationStatus invitationStatus;
    private String addedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getExternalMemberEmail() {
        return externalMemberEmail;
    }

    public void setExternalMemberEmail(String externalMemberEmail) {
        this.externalMemberEmail = externalMemberEmail;
    }

    public StoreTeamRole getRole() {
        return role;
    }

    public void setRole(StoreTeamRole role) {
        this.role = role;
    }

    public TeamInvitationStatus getInvitationStatus() {
        return invitationStatus;
    }

    public void setInvitationStatus(TeamInvitationStatus invitationStatus) {
        this.invitationStatus = invitationStatus;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}


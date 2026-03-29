package ma.talabaty.talabaty.domain.teams.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import ma.talabaty.talabaty.domain.stores.model.Store;
import ma.talabaty.talabaty.domain.users.model.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "store_team_members")
public class StoreTeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "external_member_email", length = 180)
    private String externalMemberEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StoreTeamRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "invitation_status", nullable = false, length = 32)
    private TeamInvitationStatus invitationStatus = TeamInvitationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by")
    private User addedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public User getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(User addedBy) {
        this.addedBy = addedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}


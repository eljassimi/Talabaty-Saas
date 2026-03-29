package ma.talabaty.talabaty.domain.teams.repository;

import ma.talabaty.talabaty.domain.teams.model.StoreTeamMember;
import ma.talabaty.talabaty.domain.teams.model.StoreTeamRole;
import ma.talabaty.talabaty.domain.teams.model.TeamInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreTeamMemberRepository extends JpaRepository<StoreTeamMember, java.util.UUID> {
    @Query("SELECT tm FROM StoreTeamMember tm LEFT JOIN FETCH tm.user LEFT JOIN FETCH tm.addedBy WHERE tm.store.id = :storeId")
    List<StoreTeamMember> findByStoreId(@Param("storeId") UUID storeId);
    
    @Query("SELECT tm FROM StoreTeamMember tm WHERE tm.user.id = :userId")
    List<StoreTeamMember> findByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT tm FROM StoreTeamMember tm WHERE tm.store.id = :storeId AND tm.role = :role")
    List<StoreTeamMember> findByStoreIdAndRole(@Param("storeId") UUID storeId, @Param("role") StoreTeamRole role);

    
    @Query("SELECT DISTINCT tm FROM StoreTeamMember tm JOIN FETCH tm.user u WHERE tm.store.id = :storeId AND tm.role IN :roles AND tm.user IS NOT NULL ORDER BY u.id")
    List<StoreTeamMember> findSupportMembersWithUserByStoreId(@Param("storeId") UUID storeId, @Param("roles") List<StoreTeamRole> roles);
    
    @Query("SELECT tm FROM StoreTeamMember tm WHERE tm.store.id = :storeId AND tm.user.id = :userId")
    Optional<StoreTeamMember> findByStoreIdAndUserId(@Param("storeId") UUID storeId, @Param("userId") UUID userId);
    
    @Query("SELECT tm FROM StoreTeamMember tm WHERE tm.store.id = :storeId AND tm.invitationStatus = :status")
    List<StoreTeamMember> findByStoreIdAndInvitationStatus(@Param("storeId") UUID storeId, @Param("status") TeamInvitationStatus status);
}


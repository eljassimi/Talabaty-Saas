package ma.talabaty.talabaty.api.controllers;

import ma.talabaty.talabaty.api.dtos.TeamMemberDto;
import ma.talabaty.talabaty.api.mappers.TeamMapper;
import ma.talabaty.talabaty.core.security.AuthenticationHelper;
import ma.talabaty.talabaty.core.security.JwtUser;
import ma.talabaty.talabaty.core.security.PermissionChecker;
import ma.talabaty.talabaty.domain.stores.model.Store;
import ma.talabaty.talabaty.domain.stores.service.StoreService;
import ma.talabaty.talabaty.domain.teams.model.StoreTeamMember;
import ma.talabaty.talabaty.domain.teams.model.StoreTeamRole;
import ma.talabaty.talabaty.domain.teams.service.TeamService;
import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.model.UserRole;
import ma.talabaty.talabaty.domain.users.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stores/{storeId}/team")
public class TeamController {

    private final TeamService teamService;
    private final PermissionChecker permissionChecker;
    private final StoreService storeService;
    private final UserRepository userRepository;
    private final TeamMapper teamMapper;

    public TeamController(TeamService teamService, PermissionChecker permissionChecker,
                         StoreService storeService, UserRepository userRepository, TeamMapper teamMapper) {
        this.teamService = teamService;
        this.permissionChecker = permissionChecker;
        this.storeService = storeService;
        this.userRepository = userRepository;
        this.teamMapper = teamMapper;
    }

    private UUID getUserIdFromAuth(Authentication authentication) {
        if (authentication.getPrincipal() instanceof JwtUser) {
            JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
            return UUID.fromString(jwtUser.getUserId());
        }
        // Fallback for backward compatibility
        return UUID.fromString(authentication.getName());
    }

    @PostMapping("/invite-user")
    public ResponseEntity<TeamMemberDto> inviteUser(
            @PathVariable String storeId,
            @RequestBody InviteUserRequest request,
            Authentication authentication) {
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        Store store = storeService.findByAccountIdAndId(accountId, UUID.fromString(storeId))
                .orElseThrow(() -> new RuntimeException("Store not found"));
        
        // Check permission
        if (!permissionChecker.canManageTeamMembers(user.getRole(), userId, store)) {
            throw new AccessDeniedException("You don't have permission to manage team members");
        }
        
        UUID addedBy = getUserIdFromAuth(authentication);
        StoreTeamMember member = teamService.inviteUser(
                UUID.fromString(storeId),
                UUID.fromString(request.getUserId()),
                request.getRole(),
                addedBy
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(teamMapper.toDto(member));
    }

    @PostMapping("/invite-external")
    public ResponseEntity<TeamMemberDto> inviteExternalMember(
            @PathVariable String storeId,
            @RequestBody InviteExternalRequest request,
            Authentication authentication) {
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        Store store = storeService.findByAccountIdAndId(accountId, UUID.fromString(storeId))
                .orElseThrow(() -> new RuntimeException("Store not found"));
        
        // Check permission
        if (!permissionChecker.canManageTeamMembers(user.getRole(), userId, store)) {
            throw new AccessDeniedException("You don't have permission to manage team members");
        }
        
        UUID addedBy = getUserIdFromAuth(authentication);
        StoreTeamMember member = teamService.inviteExternalMember(
                UUID.fromString(storeId),
                request.getEmail(),
                request.getRole(),
                addedBy
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(teamMapper.toDto(member));
    }

    /**
     * Create or add a team member to a store
     * If user exists, just add them to the store (if not already a member)
     * If user doesn't exist, create them and add to the store
     */
    @PostMapping("/create-member")
    public ResponseEntity<CreateTeamMemberResponseDto> createTeamMember(
            @PathVariable String storeId,
            @RequestBody CreateTeamMemberRequest request,
            Authentication authentication) {
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        // Find store by ID (not restricted to account - allows cross-account team management)
        Store store = storeService.findById(UUID.fromString(storeId))
                .orElseThrow(() -> new RuntimeException("Store not found"));
        
        // Check permission to manage team members (includes manager check)
        if (!permissionChecker.canManageTeamMembers(currentUser.getRole(), userId, store)) {
            throw new AccessDeniedException("You don't have permission to manage team members");
        }

        // Validate role
        if (request.getRole() != UserRole.MANAGER && request.getRole() != UserRole.SUPPORT) {
            throw new RuntimeException("Only MANAGER and SUPPORT roles can be assigned to team members");
        }

        UUID addedBy = getUserIdFromAuth(authentication);
        TeamService.CreateTeamMemberResponse response = teamService.createOrAddTeamMember(
                UUID.fromString(storeId),
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getRole(),
                accountId,
                addedBy
        );
        
        CreateTeamMemberResponseDto responseDto = new CreateTeamMemberResponseDto();
        responseDto.setMember(teamMapper.toDto(response.getMember()));
        responseDto.setUserWasCreated(response.isUserWasCreated());
        responseDto.setGeneratedPassword(response.getGeneratedPassword());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping
    public ResponseEntity<List<TeamMemberDto>> getTeamMembers(@PathVariable String storeId) {
        List<StoreTeamMember> members = teamService.getStoreTeamMembers(UUID.fromString(storeId));
        List<TeamMemberDto> dtos = members.stream()
                .map(teamMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{memberId}/accept")
    public ResponseEntity<TeamMemberDto> acceptInvitation(@PathVariable String memberId) {
        StoreTeamMember member = teamService.acceptInvitation(UUID.fromString(memberId));
        return ResponseEntity.ok(teamMapper.toDto(member));
    }

    @PutMapping("/{memberId}/role")
    public ResponseEntity<TeamMemberDto> updateRole(
            @PathVariable String memberId,
            @PathVariable String storeId,
            @RequestBody UpdateRoleRequest request,
            Authentication authentication) {
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        Store store = storeService.findByAccountIdAndId(accountId, UUID.fromString(storeId))
                .orElseThrow(() -> new RuntimeException("Store not found"));
        
        // Check permission
        if (!permissionChecker.canManageTeamMembers(user.getRole(), userId, store)) {
            throw new AccessDeniedException("You don't have permission to manage team members");
        }
        
        StoreTeamMember member = teamService.updateMemberRole(UUID.fromString(memberId), request.getRole());
        return ResponseEntity.ok(teamMapper.toDto(member));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String memberId,
            @PathVariable String storeId,
            Authentication authentication) {
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        Store store = storeService.findByAccountIdAndId(accountId, UUID.fromString(storeId))
                .orElseThrow(() -> new RuntimeException("Store not found"));
        
        // Check permission
        if (!permissionChecker.canManageTeamMembers(user.getRole(), userId, store)) {
            throw new AccessDeniedException("You don't have permission to manage team members");
        }
        
        teamService.removeMember(UUID.fromString(memberId));
        return ResponseEntity.noContent().build();
    }

    /**
     * Create team members in bulk (managers and supports)
     * Only ACCOUNT_OWNER can create team members
     */
    @PostMapping("/bulk-create")
    public ResponseEntity<TeamService.BulkCreateTeamResponse> bulkCreateTeamMembers(
            @PathVariable String storeId,
            @RequestBody TeamService.BulkCreateTeamRequest request,
            Authentication authentication) {
        UUID addedBy = getUserIdFromAuth(authentication);
        TeamService.BulkCreateTeamResponse response = teamService.bulkCreateTeamMembers(
                UUID.fromString(storeId),
                request,
                addedBy
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public static class InviteUserRequest {
        private String userId;
        private StoreTeamRole role;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public StoreTeamRole getRole() { return role; }
        public void setRole(StoreTeamRole role) { this.role = role; }
    }

    public static class InviteExternalRequest {
        private String email;
        private StoreTeamRole role;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public StoreTeamRole getRole() { return role; }
        public void setRole(StoreTeamRole role) { this.role = role; }
    }

    public static class UpdateRoleRequest {
        private StoreTeamRole role;

        public StoreTeamRole getRole() { return role; }
        public void setRole(StoreTeamRole role) { this.role = role; }
    }

    public static class CreateTeamMemberRequest {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private UserRole role;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public UserRole getRole() { return role; }
        public void setRole(UserRole role) { this.role = role; }
    }

    public static class CreateTeamMemberResponseDto {
        private TeamMemberDto member;
        private boolean userWasCreated;
        private String generatedPassword;

        public TeamMemberDto getMember() { return member; }
        public void setMember(TeamMemberDto member) { this.member = member; }
        public boolean isUserWasCreated() { return userWasCreated; }
        public void setUserWasCreated(boolean userWasCreated) { this.userWasCreated = userWasCreated; }
        public String getGeneratedPassword() { return generatedPassword; }
        public void setGeneratedPassword(String generatedPassword) { this.generatedPassword = generatedPassword; }
    }

}


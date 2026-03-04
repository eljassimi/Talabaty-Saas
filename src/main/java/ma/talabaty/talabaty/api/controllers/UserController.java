package ma.talabaty.talabaty.api.controllers;

import ma.talabaty.talabaty.api.dtos.UserDto;
import ma.talabaty.talabaty.api.mappers.UserMapper;
import ma.talabaty.talabaty.core.security.AuthenticationHelper;
import ma.talabaty.talabaty.core.security.JwtUser;
import ma.talabaty.talabaty.core.security.PermissionChecker;
import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.model.UserRole;
import ma.talabaty.talabaty.domain.users.model.UserStatus;
import ma.talabaty.talabaty.domain.users.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final PermissionChecker permissionChecker;

    public UserController(UserService userService, UserMapper userMapper, PermissionChecker permissionChecker) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.permissionChecker = permissionChecker;
    }

    /**
     * Get all users in the current account (excluding ACCOUNT_OWNER)
     * Only ACCOUNT_OWNER and PLATFORM_ADMIN can access this
     */
    @GetMapping
    public ResponseEntity<List<UserDto>> getAccountUsers(Authentication authentication) {
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check permission
        if (!permissionChecker.canManageUsers(user.getRole())) {
            throw new AccessDeniedException("You don't have permission to view users");
        }
        
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        // Exclude ACCOUNT_OWNER from the users list
        List<User> users = userService.getAccountUsersExcludingOwners(accountId);
        List<UserDto> dtos = users.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get all banned users in the current account
     * Only ACCOUNT_OWNER and PLATFORM_ADMIN can access this
     * PLATFORM_ADMIN can see all banned users (including ACCOUNT_OWNER)
     * ACCOUNT_OWNER can only see banned users excluding ACCOUNT_OWNER
     */
    @GetMapping("/banned")
    public ResponseEntity<List<UserDto>> getBannedUsers(Authentication authentication) {
        UUID currentUserId = AuthenticationHelper.getUserIdFromAuth(authentication);
        
        // Verify current user is ACCOUNT_OWNER or PLATFORM_ADMIN
        User currentUser = userService.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        // Check permission
        if (!permissionChecker.canManageUsers(currentUser.getRole())) {
            throw new AccessDeniedException("You don't have permission to view banned users");
        }

        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        List<User> bannedUsers;
        
        // PLATFORM_ADMIN can see all banned users (including ACCOUNT_OWNER)
        // ACCOUNT_OWNER can only see banned users excluding ACCOUNT_OWNER
        if (currentUser.getRole() == UserRole.PLATFORM_ADMIN) {
            bannedUsers = userService.getBannedUsers(accountId);
        } else {
            bannedUsers = userService.getBannedUsersExcludingOwners(accountId);
        }
        
        List<UserDto> dtos = bannedUsers.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Create a new user (Manager or Support) by the admin
     * Only ACCOUNT_OWNER can create team members
     */
    @PostMapping
    public ResponseEntity<CreateUserResponse> createUser(
            @RequestBody CreateUserRequest request,
            Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID currentUserId = AuthenticationHelper.getUserIdFromAuth(authentication);
        
        // Verify current user is ACCOUNT_OWNER
        User currentUser = userService.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        // Check permission
        if (!permissionChecker.canManageUsers(currentUser.getRole())) {
            throw new AccessDeniedException("You don't have permission to create users");
        }

        // Validate role
        if (request.getRole() != UserRole.MANAGER && request.getRole() != UserRole.SUPPORT) {
            throw new RuntimeException("Only MANAGER and SUPPORT roles can be assigned to team members");
        }

        // Check if password was auto-generated
        boolean passwordWasAutoGenerated = request.getPassword() == null || request.getPassword().isEmpty();
        
        User user = userService.createUser(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                null, // Phone number not provided in CreateUserRequest
                accountId,
                request.getRole()
        );

        UserDto userDto = userMapper.toDto(user);
        
        // Always return CreateUserResponse for consistency
        CreateUserResponse response = new CreateUserResponse();
        response.setUser(userDto);
        // If password was auto-generated, include it in the response
        if (passwordWasAutoGenerated) {
            response.setGeneratedPassword(userService.getLastGeneratedPassword());
        } else {
            response.setGeneratedPassword(null);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Ban a user
     * PLATFORM_ADMIN can ban everyone (including ACCOUNT_OWNER)
     * ACCOUNT_OWNER can ban users but not other ACCOUNT_OWNERs
     */
    @PutMapping("/{userId}/ban")
    public ResponseEntity<UserDto> banUser(
            @PathVariable String userId,
            Authentication authentication) {
        UUID currentUserId = AuthenticationHelper.getUserIdFromAuth(authentication);
        UUID targetUserId = UUID.fromString(userId);
        
        // Verify current user is ACCOUNT_OWNER or PLATFORM_ADMIN
        User currentUser = userService.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        // Check permission
        if (!permissionChecker.canManageUsers(currentUser.getRole())) {
            throw new AccessDeniedException("You don't have permission to ban users");
        }

        // Prevent self-ban
        if (currentUserId.equals(targetUserId)) {
            throw new RuntimeException("You cannot ban yourself");
        }

        // Get target user
        User targetUser = userService.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ACCOUNT_OWNER cannot ban other ACCOUNT_OWNERs (only PLATFORM_ADMIN can)
        if (currentUser.getRole() == UserRole.ACCOUNT_OWNER && targetUser.getRole() == UserRole.ACCOUNT_OWNER) {
            throw new RuntimeException("Account owners cannot ban other account owners. Only platform admins can do this.");
        }

        User user = userService.banUser(targetUserId);
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    /**
     * Unban a user
     * Only ACCOUNT_OWNER can unban users
     */
    @PutMapping("/{userId}/unban")
    public ResponseEntity<UserDto> unbanUser(
            @PathVariable String userId,
            Authentication authentication) {
        UUID currentUserId = AuthenticationHelper.getUserIdFromAuth(authentication);
        
        // Verify current user is ACCOUNT_OWNER
        User currentUser = userService.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        if (currentUser.getRole() != UserRole.ACCOUNT_OWNER && currentUser.getRole() != UserRole.PLATFORM_ADMIN) {
            throw new RuntimeException("Only account owners can unban users");
        }

        User user = userService.unbanUser(UUID.fromString(userId));
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    /**
     * Update user status (for confirmation, activation, etc.)
     * Managers and Supports can confirm their own invitations
     * ACCOUNT_OWNER can update any user status
     */
    @PutMapping("/{userId}/status")
    public ResponseEntity<UserDto> updateUserStatus(
            @PathVariable String userId,
            @RequestBody UpdateStatusRequest request,
            Authentication authentication) {
        UUID currentUserId = AuthenticationHelper.getUserIdFromAuth(authentication);
        UUID targetUserId = UUID.fromString(userId);
        
        User currentUser = userService.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        // If updating own status, allow for confirmation (INVITED -> ACTIVE)
        if (currentUserId.equals(targetUserId)) {
            User targetUser = userService.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Allow users to confirm their own invitation
            if (targetUser.getStatus() == UserStatus.INVITED && request.getStatus() == UserStatus.ACTIVE) {
                User updatedUser = userService.updateUserStatus(targetUserId, request.getStatus());
                return ResponseEntity.ok(userMapper.toDto(updatedUser));
            }
            
            // Users cannot change their own status to anything else
            throw new RuntimeException("You can only confirm your own invitation");
        }
        
        // Only ACCOUNT_OWNER can update other users' status
        if (currentUser.getRole() != UserRole.ACCOUNT_OWNER && currentUser.getRole() != UserRole.PLATFORM_ADMIN) {
            throw new RuntimeException("Only account owners can update user status");
        }

        User user = userService.updateUserStatus(targetUserId, request.getStatus());
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    /**
     * Update selected store for the current user
     */
    @PutMapping("/selected-store")
    public ResponseEntity<UserDto> updateSelectedStore(
            @RequestBody UpdateSelectedStoreRequest request,
            Authentication authentication) {
        UUID currentUserId = AuthenticationHelper.getUserIdFromAuth(authentication);
        
        User user = userService.updateSelectedStore(
                currentUserId,
                request.getStoreId() != null ? UUID.fromString(request.getStoreId()) : null
        );
        
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    public static class CreateUserRequest {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private UserRole role;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
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

        public UserRole getRole() {
            return role;
        }

        public void setRole(UserRole role) {
            this.role = role;
        }
    }

    public static class UpdateStatusRequest {
        private UserStatus status;

        public UserStatus getStatus() {
            return status;
        }

        public void setStatus(UserStatus status) {
            this.status = status;
        }
    }

    public static class UpdateSelectedStoreRequest {
        private String storeId;

        public String getStoreId() {
            return storeId;
        }

        public void setStoreId(String storeId) {
            this.storeId = storeId;
        }
    }

    public static class CreateUserResponse {
        private UserDto user;
        private String generatedPassword;

        public UserDto getUser() {
            return user;
        }

        public void setUser(UserDto user) {
            this.user = user;
        }

        public String getGeneratedPassword() {
            return generatedPassword;
        }

        public void setGeneratedPassword(String generatedPassword) {
            this.generatedPassword = generatedPassword;
        }
    }
}


package ma.talabaty.talabaty.api.controllers;

import jakarta.validation.Valid;
import ma.talabaty.talabaty.api.dtos.AuthRequest;
import ma.talabaty.talabaty.api.dtos.AuthResponse;
import ma.talabaty.talabaty.api.dtos.SignupRequest;
import ma.talabaty.talabaty.api.dtos.UserDto;
import ma.talabaty.talabaty.api.mappers.UserMapper;
import ma.talabaty.talabaty.core.security.JwtTokenProvider;
import ma.talabaty.talabaty.domain.accounts.service.AccountService;
import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.model.UserRole;
import ma.talabaty.talabaty.domain.users.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://127.0.0.1:3000",
    "http://localhost:3001",
    "http://127.0.0.1:3001",
    "http://localhost:3002",
    "http://127.0.0.1:3002"
}, maxAge = 3600)
public class AuthController {

    private final UserService userService;
    private final AccountService accountService;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;

    public AuthController(UserService userService, AccountService accountService,
                         JwtTokenProvider tokenProvider, UserMapper userMapper) {
        this.userService = userService;
        this.accountService = accountService;
        this.tokenProvider = tokenProvider;
        this.userMapper = userMapper;
    }

    /**
     * GET /api/auth - Info for browser or API explorers (actual auth uses POST).
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> authInfo() {
        return ResponseEntity.ok(Map.of(
                "message", "Talabaty Auth API",
                "endpoints", Map.of(
                        "signup", "POST /api/auth/signup",
                        "login", "POST /api/auth/login",
                        "refresh", "POST /api/auth/refresh",
                        "change-password", "POST /api/auth/change-password (authenticated)"
                )
        ));
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        // Create account
        var account = accountService.createAccount(request.getAccountName(), request.getAccountType());
        
        // Create user as account owner
        User user = userService.createUser(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhoneNumber(),
                account.getId(),
                UserRole.ACCOUNT_OWNER
        );

        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(user.getId().toString(), user.getEmail(), account.getId().toString());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId().toString());
        Long expiresIn = 3600L; // 1 hour

        UserDto userDto = userMapper.toDto(user);
        AuthResponse response = new AuthResponse(accessToken, refreshToken, expiresIn, userDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!userService.validatePassword(user, request.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Update last login time
        userService.updateLastLogin(user.getId());

        String accessToken = tokenProvider.generateAccessToken(user.getId().toString(), user.getEmail(), user.getAccount().getId().toString());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId().toString());
        Long expiresIn = 3600L;

        UserDto userDto = userMapper.toDto(user);
        AuthResponse response = new AuthResponse(accessToken, refreshToken, expiresIn, userDto);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String userId = tokenProvider.getUserIdFromToken(refreshToken);
        User user = userService.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = tokenProvider.generateAccessToken(user.getId().toString(), user.getEmail(), user.getAccount().getId().toString());
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getId().toString());
        Long expiresIn = 3600L;

        UserDto userDto = userMapper.toDto(user);
        AuthResponse response = new AuthResponse(newAccessToken, newRefreshToken, expiresIn, userDto);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<UserDto> changePassword(
            @RequestBody ChangePasswordRequest request,
            org.springframework.security.core.Authentication authentication) {
        String userId = authentication.getName();
        if (authentication.getPrincipal() instanceof ma.talabaty.talabaty.core.security.JwtUser) {
            ma.talabaty.talabaty.core.security.JwtUser jwtUser = (ma.talabaty.talabaty.core.security.JwtUser) authentication.getPrincipal();
            userId = jwtUser.getUserId();
        }

        // Allow null/empty currentPassword for first-time password changes
        // Pass null if not provided, so the service can properly detect it
        String currentPassword = (request.getCurrentPassword() != null && !request.getCurrentPassword().isEmpty()) 
                                ? request.getCurrentPassword() 
                                : null;
        User user = userService.changePassword(
                java.util.UUID.fromString(userId),
                currentPassword,
                request.getNewPassword()
        );

        return ResponseEntity.ok(userMapper.toDto(user));
    }

    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}


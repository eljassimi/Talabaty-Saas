package ma.talabaty.talabaty.domain.users.service;

import ma.talabaty.talabaty.domain.accounts.model.Account;
import ma.talabaty.talabaty.domain.accounts.repository.AccountRepository;
import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.model.UserRole;
import ma.talabaty.talabaty.domain.users.model.UserStatus;
import ma.talabaty.talabaty.domain.users.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    
    
    private static final ThreadLocal<String> lastGeneratedPassword = new ThreadLocal<>();

    public UserService(UserRepository userRepository, AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(String email, String password, String firstName, String lastName, String phoneNumber, UUID accountId, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User with email " + email + " already exists");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        
        String finalPassword = password;
        boolean mustChangePassword = false;
        if (password == null || password.isEmpty()) {
            finalPassword = generateTemporaryPassword();
            mustChangePassword = true;
            
            lastGeneratedPassword.set(finalPassword);
        } else {
            
            lastGeneratedPassword.remove();
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(finalPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber);
        user.setAccount(account);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(mustChangePassword);

        return userRepository.save(user);
    }

    private String generateTemporaryPassword() {
        
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
    
    
    public String getLastGeneratedPassword() {
        String password = lastGeneratedPassword.get();
        lastGeneratedPassword.remove(); 
        return password;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public boolean validatePassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    public List<User> getAccountUsers(UUID accountId) {
        return userRepository.findByAccountId(accountId);
    }

    public List<User> getAccountUsersExcludingOwners(UUID accountId) {
        return userRepository.findByAccountIdExcludingRole(accountId, UserRole.ACCOUNT_OWNER);
    }

    public List<User> getBannedUsers(UUID accountId) {
        return userRepository.findByAccountIdAndStatus(accountId, UserStatus.BANNED);
    }

    public List<User> getBannedUsersExcludingOwners(UUID accountId) {
        List<User> allBanned = userRepository.findByAccountIdAndStatus(accountId, UserStatus.BANNED);
        return allBanned.stream()
                .filter(user -> user.getRole() != UserRole.ACCOUNT_OWNER)
                .collect(Collectors.toList());
    }

    public User banUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.BANNED);
        return userRepository.save(user);
    }

    public User unbanUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    public User updateUserStatus(UUID userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(status);
        return userRepository.save(user);
    }

    public User changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        
        boolean skipValidation = (user.getMustChangePassword() != null && user.getMustChangePassword()) 
                                || (currentPassword == null || currentPassword.isEmpty());
        
        if (!skipValidation) {
            
            if (!validatePassword(user, currentPassword)) {
                throw new RuntimeException("Current password is incorrect");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        return userRepository.save(user);
    }

    public User updateSelectedStore(UUID userId, UUID storeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setSelectedStoreId(storeId);
        return userRepository.save(user);
    }

    public void updateLastLogin(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setLastLoginAt(java.time.OffsetDateTime.now());
        userRepository.save(user);
    }
}


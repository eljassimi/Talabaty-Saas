package ma.talabaty.talabaty.domain.teams.service;

import ma.talabaty.talabaty.domain.stores.model.Store;
import ma.talabaty.talabaty.domain.stores.repository.StoreRepository;
import ma.talabaty.talabaty.domain.orders.model.Order;
import ma.talabaty.talabaty.domain.orders.repository.OrderRepository;
import ma.talabaty.talabaty.domain.teams.model.StoreTeamMember;
import ma.talabaty.talabaty.domain.teams.model.StoreTeamRole;
import ma.talabaty.talabaty.domain.teams.model.TeamInvitationStatus;
import ma.talabaty.talabaty.domain.teams.repository.StoreTeamMemberRepository;
import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.model.UserRole;
import ma.talabaty.talabaty.domain.users.model.UserStatus;
import ma.talabaty.talabaty.domain.users.repository.UserRepository;
import ma.talabaty.talabaty.domain.users.service.UserService;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TeamService {

    private final StoreTeamMemberRepository teamMemberRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final OrderRepository orderRepository;

    public TeamService(StoreTeamMemberRepository teamMemberRepository, StoreRepository storeRepository,
                      UserRepository userRepository, UserService userService, OrderRepository orderRepository) {
        this.teamMemberRepository = teamMemberRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.orderRepository = orderRepository;
    }

    public StoreTeamMember inviteUser(UUID storeId, UUID userId, StoreTeamRole role, UUID addedByUserId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        
        Optional<StoreTeamMember> existing = teamMemberRepository.findByStoreIdAndUserId(storeId, userId);
        if (existing.isPresent()) {
            throw new RuntimeException("User is already a member of this store");
        }

        StoreTeamMember member = new StoreTeamMember();
        member.setStore(store);
        member.setUser(user);
        member.setRole(role);
        member.setInvitationStatus(TeamInvitationStatus.PENDING);
        if (addedByUserId != null) {
            User addedByUser = userRepository.findById(addedByUserId)
                    .orElseThrow(() -> new RuntimeException("Added by user not found"));
            member.setAddedBy(addedByUser);
        }

        return teamMemberRepository.save(member);
    }

    public StoreTeamMember inviteExternalMember(UUID storeId, String email, StoreTeamRole role, UUID addedByUserId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        StoreTeamMember member = new StoreTeamMember();
        member.setStore(store);
        member.setExternalMemberEmail(email);
        member.setRole(role);
        member.setInvitationStatus(TeamInvitationStatus.PENDING);
        if (addedByUserId != null) {
            User addedByUser = userRepository.findById(addedByUserId)
                    .orElseThrow(() -> new RuntimeException("Added by user not found"));
            member.setAddedBy(addedByUser);
        }

        return teamMemberRepository.save(member);
    }

    public StoreTeamMember acceptInvitation(UUID memberId) {
        StoreTeamMember member = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Team member not found"));

        member.setInvitationStatus(TeamInvitationStatus.ACCEPTED);
        return teamMemberRepository.save(member);
    }

    public void removeMember(UUID memberId) {
        StoreTeamMember member = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Team member not found"));

        
        if (member.getUser() != null && member.getStore() != null
                && member.getUser().getId() != null && member.getStore().getId() != null) {
            java.util.UUID storeId = member.getStore().getId();
            java.util.UUID userId = member.getUser().getId();
            java.util.List<Order> assignedOrders = orderRepository.findByStoreIdAndAssignedToUserId(storeId, userId);
            for (Order order : assignedOrders) {
                order.setAssignedTo(null);
            }
            if (!assignedOrders.isEmpty()) {
                orderRepository.saveAll(assignedOrders);
            }
        }

        teamMemberRepository.delete(member);
    }

    public List<StoreTeamMember> getStoreTeamMembers(UUID storeId) {
        return teamMemberRepository.findByStoreId(storeId);
    }

    public Optional<StoreTeamMember> findByStoreIdAndUserId(UUID storeId, UUID userId) {
        return teamMemberRepository.findByStoreIdAndUserId(storeId, userId);
    }

    public StoreTeamMember updateMemberRole(UUID memberId, StoreTeamRole newRole) {
        StoreTeamMember member = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Team member not found"));

        member.setRole(newRole);
        return teamMemberRepository.save(member);
    }

    
    public BulkCreateTeamResponse bulkCreateTeamMembers(
            UUID storeId,
            BulkCreateTeamRequest request,
            UUID addedByUserId) {
        
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        
        UUID accountId = store.getAccount().getId();
        BulkCreateTeamResponse response = new BulkCreateTeamResponse();
        List<StoreTeamMember> createdManagers = new ArrayList<>();
        List<StoreTeamMember> createdSupports = new ArrayList<>();

        
        if (request.getManagers() != null) {
            for (TeamMemberRequest memberRequest : request.getManagers()) {
                try {
                    
                    Optional<User> existingUser = userRepository.findByEmail(memberRequest.getEmail());
                    User user;
                    
                    if (existingUser.isPresent()) {
                        user = existingUser.get();
                        
                        Optional<StoreTeamMember> existingMember = teamMemberRepository.findByStoreIdAndUserId(storeId, user.getId());
                        if (existingMember.isPresent()) {
                            continue; 
                        }
                    } else {
                        
                        
                        String password = memberRequest.getPassword() != null && !memberRequest.getPassword().isEmpty()
                                ? memberRequest.getPassword()
                                : null;
                        
                        user = userService.createUser(
                                memberRequest.getEmail(),
                                password,
                                memberRequest.getFirstName(),
                                memberRequest.getLastName(),
                                null, 
                                accountId,
                                UserRole.MANAGER
                        );
                        
                        
                        user.setStatus(UserStatus.ACTIVE);
                        userRepository.save(user);
                    }
                    
                    
                    StoreTeamMember member = new StoreTeamMember();
                    member.setStore(store);
                    member.setUser(user);
                    member.setRole(StoreTeamRole.MANAGER);
                    member.setInvitationStatus(TeamInvitationStatus.ACCEPTED);
                    if (addedByUserId != null) {
                        User addedByUser = userRepository.findById(addedByUserId)
                                .orElseThrow(() -> new RuntimeException("Added by user not found"));
                        member.setAddedBy(addedByUser);
                    }
                    createdManagers.add(teamMemberRepository.save(member));
                    
                } catch (Exception e) {
                    
                    System.err.println("Error creating manager " + memberRequest.getEmail() + ": " + e.getMessage());
                }
            }
        }

        
        if (request.getSupports() != null) {
            for (TeamMemberRequest memberRequest : request.getSupports()) {
                try {
                    
                    Optional<User> existingUser = userRepository.findByEmail(memberRequest.getEmail());
                    User user;
                    
                    if (existingUser.isPresent()) {
                        user = existingUser.get();
                        
                        Optional<StoreTeamMember> existingMember = teamMemberRepository.findByStoreIdAndUserId(storeId, user.getId());
                        if (existingMember.isPresent()) {
                            continue; 
                        }
                    } else {
                        
                        
                        String password = memberRequest.getPassword() != null && !memberRequest.getPassword().isEmpty()
                                ? memberRequest.getPassword()
                                : null;
                        
                        user = userService.createUser(
                                memberRequest.getEmail(),
                                password,
                                memberRequest.getFirstName(),
                                memberRequest.getLastName(),
                                null, 
                                accountId,
                                UserRole.SUPPORT
                        );
                        
                        
                        user.setStatus(UserStatus.ACTIVE);
                        userRepository.save(user);
                    }
                    
                    
                    StoreTeamMember member = new StoreTeamMember();
                    member.setStore(store);
                    member.setUser(user);
                    member.setRole(StoreTeamRole.SUPPORT);
                    member.setInvitationStatus(TeamInvitationStatus.ACCEPTED);
                    if (addedByUserId != null) {
                        User addedByUser = userRepository.findById(addedByUserId)
                                .orElseThrow(() -> new RuntimeException("Added by user not found"));
                        member.setAddedBy(addedByUser);
                    }
                    createdSupports.add(teamMemberRepository.save(member));
                    
                } catch (Exception e) {
                    
                    System.err.println("Error creating support " + memberRequest.getEmail() + ": " + e.getMessage());
                }
            }
        }

        response.setCreatedManagers(createdManagers);
        response.setCreatedSupports(createdSupports);
        response.setTotalCreated(createdManagers.size() + createdSupports.size());
        
        return response;
    }

    
    public CreateTeamMemberResponse createOrAddTeamMember(
            UUID storeId,
            String email,
            String password,
            String firstName,
            String lastName,
            UserRole userRole,
            UUID accountId,
            UUID addedByUserId) {
        
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;
        boolean userWasCreated = false;
        String generatedPassword = null;

        if (existingUser.isPresent()) {
            
            user = existingUser.get();

            Optional<StoreTeamMember> existingMember = teamMemberRepository.findByStoreIdAndUserId(storeId, user.getId());
            if (existingMember.isPresent()) {
                
                
                CreateTeamMemberResponse response = new CreateTeamMemberResponse();
                response.setMember(existingMember.get());
                response.setUserWasCreated(false);
                response.setGeneratedPassword(null);
                return response;
            }
        } else {
            
            userWasCreated = true;
            
            
            String finalPassword = password;
            boolean mustChangePassword = false;
            if (password == null || password.isEmpty()) {
                finalPassword = generateTemporaryPassword();
                mustChangePassword = true;
                generatedPassword = finalPassword;
            }

            user = userService.createUser(
                    email,
                    finalPassword,
                    firstName,
                    lastName,
                    null, 
                    accountId,
                    userRole
            );
            
            if (mustChangePassword) {
                user.setMustChangePassword(true);
                userRepository.save(user);
            }
        }

        
        StoreTeamMember member = new StoreTeamMember();
        member.setStore(store);
        member.setUser(user);
        member.setRole(userRole == UserRole.MANAGER ? StoreTeamRole.MANAGER : StoreTeamRole.SUPPORT);
        member.setInvitationStatus(TeamInvitationStatus.ACCEPTED);
        if (addedByUserId != null) {
            User addedByUser = userRepository.findById(addedByUserId)
                    .orElseThrow(() -> new RuntimeException("Added by user not found"));
            member.setAddedBy(addedByUser);
        }

        StoreTeamMember savedMember = teamMemberRepository.save(member);

        CreateTeamMemberResponse response = new CreateTeamMemberResponse();
        response.setMember(savedMember);
        response.setUserWasCreated(userWasCreated);
        response.setGeneratedPassword(generatedPassword);
        
        return response;
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

    
    public static class BulkCreateTeamRequest {
        private List<TeamMemberRequest> managers;
        private List<TeamMemberRequest> supports;

        public List<TeamMemberRequest> getManagers() { return managers; }
        public void setManagers(List<TeamMemberRequest> managers) { this.managers = managers; }
        public List<TeamMemberRequest> getSupports() { return supports; }
        public void setSupports(List<TeamMemberRequest> supports) { this.supports = supports; }
    }

    public static class TeamMemberRequest {
        private String email;
        private String firstName;
        private String lastName;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class BulkCreateTeamResponse {
        private List<StoreTeamMember> createdManagers;
        private List<StoreTeamMember> createdSupports;
        private int totalCreated;

        public BulkCreateTeamResponse() {
            this.createdManagers = new ArrayList<>();
            this.createdSupports = new ArrayList<>();
        }

        public List<StoreTeamMember> getCreatedManagers() { return createdManagers; }
        public void setCreatedManagers(List<StoreTeamMember> createdManagers) { this.createdManagers = createdManagers; }
        public List<StoreTeamMember> getCreatedSupports() { return createdSupports; }
        public void setCreatedSupports(List<StoreTeamMember> createdSupports) { this.createdSupports = createdSupports; }
        public int getTotalCreated() { return totalCreated; }
        public void setTotalCreated(int totalCreated) { this.totalCreated = totalCreated; }
    }

    public static class CreateTeamMemberResponse {
        private StoreTeamMember member;
        private boolean userWasCreated;
        private String generatedPassword;

        public StoreTeamMember getMember() { return member; }
        public void setMember(StoreTeamMember member) { this.member = member; }
        public boolean isUserWasCreated() { return userWasCreated; }
        public void setUserWasCreated(boolean userWasCreated) { this.userWasCreated = userWasCreated; }
        public String getGeneratedPassword() { return generatedPassword; }
        public void setGeneratedPassword(String generatedPassword) { this.generatedPassword = generatedPassword; }
    }
}


package ma.talabaty.talabaty.api.controllers;

import ma.talabaty.talabaty.core.security.AuthenticationHelper;
import ma.talabaty.talabaty.core.security.PermissionChecker;
import ma.talabaty.talabaty.domain.support.SupportPaymentRequest;
import ma.talabaty.talabaty.domain.support.SupportRevenueService;
import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final SupportRevenueService supportRevenueService;
    private final PermissionChecker permissionChecker;
    private final UserRepository userRepository;

    public SupportController(SupportRevenueService supportRevenueService,
                             PermissionChecker permissionChecker,
                             UserRepository userRepository) {
        this.supportRevenueService = supportRevenueService;
        this.permissionChecker = permissionChecker;
        this.userRepository = userRepository;
    }

    
    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance(
            @RequestParam String storeId,
            Authentication authentication) {
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        UUID storeUuid = UUID.fromString(storeId);
        BigDecimal totalEarned = supportRevenueService.getTotalEarned(userId, storeUuid);
        BigDecimal totalPaid = supportRevenueService.getTotalPaid(userId, storeUuid);
        BigDecimal balance = totalEarned.subtract(totalPaid);
        Map<String, Object> body = new HashMap<>();
        body.put("balance", balance);
        body.put("totalEarned", totalEarned);
        body.put("totalPaid", totalPaid);
        return ResponseEntity.ok(body);
    }

    
    @PostMapping("/request-payment")
    public ResponseEntity<Map<String, Object>> requestPayment(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        String storeIdStr = (String) request.get("storeId");
        if (storeIdStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "storeId is required"));
        }
        Number amountNum = (Number) request.get("amount");
        if (amountNum == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "amount is required"));
        }
        BigDecimal amount = BigDecimal.valueOf(amountNum.doubleValue());
        try {
            SupportPaymentRequest pr = supportRevenueService.requestPayment(userId, UUID.fromString(storeIdStr), amount);
            Map<String, Object> body = new HashMap<>();
            body.put("id", pr.getId().toString());
            body.put("amountRequested", pr.getAmountRequested());
            body.put("status", pr.getStatus().name());
            body.put("requestedAt", pr.getRequestedAt());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    
    @GetMapping("/payment-requests")
    public ResponseEntity<List<Map<String, Object>>> getMyPaymentRequests(
            @RequestParam String storeId,
            Authentication authentication) {
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        List<SupportPaymentRequest> list = supportRevenueService.getPaymentRequestsByUserAndStore(userId, UUID.fromString(storeId));
        return ResponseEntity.ok(list.stream().map(this::toPaymentRequestMap).collect(Collectors.toList()));
    }

    
    @GetMapping("/payment-requests/admin")
    public ResponseEntity<List<Map<String, Object>>> getAllPaymentRequests(Authentication authentication) {
        UUID accountId = AuthenticationHelper.getAccountIdFromAuth(authentication);
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!permissionChecker.canManagePaymentRequests(user.getRole())) {
            return ResponseEntity.status(403).build();
        }
        List<SupportPaymentRequest> list = supportRevenueService.getPaymentRequestsByAccount(accountId);
        return ResponseEntity.ok(list.stream().map(this::toPaymentRequestMapAdmin).collect(Collectors.toList()));
    }

    
    @PutMapping("/payment-requests/{id}/paid")
    public ResponseEntity<Map<String, Object>> markAsPaid(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body,
            Authentication authentication) {
        UUID adminId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(adminId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!permissionChecker.canManagePaymentRequests(user.getRole())) {
            return ResponseEntity.status(403).build();
        }
        String note = body != null && body.containsKey("note") ? (String) body.get("note") : null;
        SupportPaymentRequest pr = supportRevenueService.markAsPaid(UUID.fromString(id), adminId, note);
        return ResponseEntity.ok(toPaymentRequestMapAdmin(pr));
    }

    
    @PutMapping("/payment-requests/{id}/rejected")
    public ResponseEntity<Map<String, Object>> rejectRequest(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body,
            Authentication authentication) {
        UUID adminId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(adminId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!permissionChecker.canManagePaymentRequests(user.getRole())) {
            return ResponseEntity.status(403).build();
        }
        String note = body != null && body.containsKey("note") ? (String) body.get("note") : null;
        SupportPaymentRequest pr = supportRevenueService.rejectRequest(UUID.fromString(id), adminId, note);
        return ResponseEntity.ok(toPaymentRequestMapAdmin(pr));
    }

    private Map<String, Object> toPaymentRequestMap(SupportPaymentRequest r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId().toString());
        m.put("amountRequested", r.getAmountRequested());
        m.put("status", r.getStatus().name());
        m.put("requestedAt", r.getRequestedAt());
        m.put("processedAt", r.getProcessedAt());
        m.put("note", r.getNote());
        return m;
    }

    private Map<String, Object> toPaymentRequestMapAdmin(SupportPaymentRequest r) {
        Map<String, Object> m = toPaymentRequestMap(r);
        m.put("userId", r.getUser().getId().toString());
        m.put("userName", r.getUser().getFirstName() + " " + r.getUser().getLastName());
        m.put("storeId", r.getStore().getId().toString());
        m.put("storeName", r.getStore().getName());
        if (r.getProcessedBy() != null) {
            m.put("processedBy", r.getProcessedBy().getFirstName() + " " + r.getProcessedBy().getLastName());
        }
        return m;
    }
}

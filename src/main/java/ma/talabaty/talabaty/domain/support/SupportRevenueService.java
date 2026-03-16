package ma.talabaty.talabaty.domain.support;

import ma.talabaty.talabaty.domain.stores.model.Store;
import ma.talabaty.talabaty.domain.stores.repository.StoreRepository;
import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SupportRevenueService {

    private final SupportRevenueEntryRepository revenueEntryRepository;
    private final SupportPaymentRequestRepository paymentRequestRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    public SupportRevenueService(SupportRevenueEntryRepository revenueEntryRepository,
                                 SupportPaymentRequestRepository paymentRequestRepository,
                                 StoreRepository storeRepository,
                                 UserRepository userRepository) {
        this.revenueEntryRepository = revenueEntryRepository;
        this.paymentRequestRepository = paymentRequestRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
    }

    /**
     * Total earned by support user for a store (sum of all revenue entries).
     */
    public BigDecimal getTotalEarned(UUID userId, UUID storeId) {
        BigDecimal sum = revenueEntryRepository.sumAmountByUserAndStore(userId, storeId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    /**
     * Total already paid out to this user for this store (sum of paid payment requests).
     */
    public BigDecimal getTotalPaid(UUID userId, UUID storeId) {
        BigDecimal sum = paymentRequestRepository.sumAmountByUserAndStoreAndStatus(
                userId, storeId, SupportPaymentRequest.PaymentRequestStatus.PAID);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    /**
     * Current balance = total earned - total paid.
     */
    public BigDecimal getBalance(UUID userId, UUID storeId) {
        return getTotalEarned(userId, storeId).subtract(getTotalPaid(userId, storeId));
    }

    /**
     * Create a payment request from support user. Amount must not exceed balance.
     */
    public SupportPaymentRequest requestPayment(UUID userId, UUID storeId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        BigDecimal balance = getBalance(userId, storeId);
        if (amount.compareTo(balance) > 0) {
            throw new IllegalArgumentException("Requested amount cannot exceed balance: " + balance);
        }
        SupportPaymentRequest request = new SupportPaymentRequest();
        request.setUser(user);
        request.setStore(store);
        request.setAmountRequested(amount);
        request.setStatus(SupportPaymentRequest.PaymentRequestStatus.PENDING);
        return paymentRequestRepository.save(request);
    }

    public List<SupportPaymentRequest> getPaymentRequestsByUserAndStore(UUID userId, UUID storeId) {
        return paymentRequestRepository.findByUser_IdAndStore_IdOrderByRequestedAtDesc(userId, storeId);
    }

    /**
     * List all payment requests for the account (admin only).
     */
    public List<SupportPaymentRequest> getPaymentRequestsByAccount(UUID accountId) {
        return paymentRequestRepository.findByAccountIdOrderByRequestedAtDesc(accountId);
    }

    /**
     * Mark a payment request as PAID (admin).
     */
    public SupportPaymentRequest markAsPaid(UUID requestId, UUID processedByUserId, String note) {
        SupportPaymentRequest request = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Payment request not found"));
        request.setStatus(SupportPaymentRequest.PaymentRequestStatus.PAID);
        request.setProcessedAt(java.time.OffsetDateTime.now());
        User processedBy = userRepository.findById(processedByUserId).orElse(null);
        request.setProcessedBy(processedBy);
        request.setNote(note);
        return paymentRequestRepository.save(request);
    }

    /**
     * Reject a payment request (admin).
     */
    public SupportPaymentRequest rejectRequest(UUID requestId, UUID processedByUserId, String note) {
        SupportPaymentRequest request = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Payment request not found"));
        request.setStatus(SupportPaymentRequest.PaymentRequestStatus.REJECTED);
        request.setProcessedAt(java.time.OffsetDateTime.now());
        User processedBy = userRepository.findById(processedByUserId).orElse(null);
        request.setProcessedBy(processedBy);
        request.setNote(note);
        return paymentRequestRepository.save(request);
    }
}

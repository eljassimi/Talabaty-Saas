package ma.talabaty.talabaty.domain.orders.service;

import ma.talabaty.talabaty.domain.orders.model.Order;
import ma.talabaty.talabaty.domain.orders.model.OrderSource;
import ma.talabaty.talabaty.domain.orders.model.OrderStatus;
import ma.talabaty.talabaty.domain.orders.model.OrderStatusHistory;
import ma.talabaty.talabaty.domain.orders.repository.OrderRepository;
import ma.talabaty.talabaty.domain.orders.repository.OrderStatusHistoryRepository;
import ma.talabaty.talabaty.domain.stores.model.Store;
import ma.talabaty.talabaty.domain.stores.model.StoreSettings;
import ma.talabaty.talabaty.domain.stores.repository.StoreRepository;
import ma.talabaty.talabaty.domain.stores.service.StoreService;
import ma.talabaty.talabaty.domain.support.SupportRevenueEntry;
import ma.talabaty.talabaty.domain.support.SupportRevenueEntryRepository;
import ma.talabaty.talabaty.domain.teams.model.StoreTeamMember;
import ma.talabaty.talabaty.domain.teams.model.StoreTeamRole;
import ma.talabaty.talabaty.domain.teams.repository.StoreTeamMemberRepository;
import ma.talabaty.talabaty.domain.whatsapp.WhatsAppService;
import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final StoreService storeService;
    private final WhatsAppService whatsAppService;
    private final StoreTeamMemberRepository storeTeamMemberRepository;
    private final SupportRevenueEntryRepository supportRevenueEntryRepository;

    public OrderService(OrderRepository orderRepository, OrderStatusHistoryRepository statusHistoryRepository,
                       StoreRepository storeRepository, UserRepository userRepository,
                       StoreService storeService, WhatsAppService whatsAppService,
                       StoreTeamMemberRepository storeTeamMemberRepository,
                       SupportRevenueEntryRepository supportRevenueEntryRepository) {
        this.orderRepository = orderRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.storeService = storeService;
        this.whatsAppService = whatsAppService;
        this.storeTeamMemberRepository = storeTeamMemberRepository;
        this.supportRevenueEntryRepository = supportRevenueEntryRepository;
    }

    public Order createOrder(UUID storeId, String customerName, String customerPhone, 
                            String destinationAddress, BigDecimal totalAmount, String currency,
                            String externalOrderId, OrderSource source, String productName, String productId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        // Check for duplicate order by external_order_id if provided
        if (externalOrderId != null && !externalOrderId.trim().isEmpty()) {
            Optional<Order> existingOrder = orderRepository.findByStoreIdAndExternalOrderId(storeId, externalOrderId);
            if (existingOrder.isPresent()) {
                // Order already exists, return it instead of creating a duplicate
                return existingOrder.get();
            }
        }

        Order order = new Order();
        order.setStore(store);
        order.setCustomerName(customerName);
        order.setCustomerPhone(customerPhone);
        order.setDestinationAddress(destinationAddress);
        
        // DEBUG: Log the totalAmount being set
        System.out.println("========================================");
        System.out.println("DEBUG OrderService.createOrder:");
        System.out.println("  totalAmount parameter = " + totalAmount);
        System.out.println("  totalAmount != null? " + (totalAmount != null));
        if (totalAmount != null) {
            System.out.println("  totalAmount.compareTo(BigDecimal.ZERO) = " + totalAmount.compareTo(BigDecimal.ZERO));
            System.out.println("  totalAmount.doubleValue() = " + totalAmount.doubleValue());
            System.out.println("  totalAmount.toString() = " + totalAmount.toString());
        } else {
            System.out.println("  WARNING: totalAmount is NULL!");
        }
        System.out.println("========================================");
        
        // Set totalAmount - if null, use ZERO, but log a warning
        BigDecimal finalTotalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        if (totalAmount == null) {
            System.out.println("WARNING: totalAmount is null, using BigDecimal.ZERO");
        }
        order.setTotalAmount(finalTotalAmount);
        order.setCurrency(currency != null ? currency : "USD");
        order.setExternalOrderId(externalOrderId);
        order.setSource(source != null ? source : OrderSource.API);
        order.setProductName(productName);
        order.setProductId(productId);
        order.setStatus(OrderStatus.ENCOURS);

        Order savedOrder = orderRepository.save(order);
        
        // DEBUG: Log the saved order
        System.out.println("========================================");
        System.out.println("DEBUG OrderService.createOrder: After saving to database:");
        System.out.println("  Saved order ID = " + savedOrder.getId());
        System.out.println("  Saved order totalAmount = " + savedOrder.getTotalAmount());
        System.out.println("  Saved order totalAmount (string) = " + (savedOrder.getTotalAmount() != null ? savedOrder.getTotalAmount().toString() : "null"));
        System.out.println("  Saved order totalAmount (double) = " + (savedOrder.getTotalAmount() != null ? savedOrder.getTotalAmount().doubleValue() : "null"));
        System.out.println("  Saved order totalAmount.compareTo(BigDecimal.ZERO) = " + (savedOrder.getTotalAmount() != null ? savedOrder.getTotalAmount().compareTo(BigDecimal.ZERO) : "null"));
        System.out.println("========================================");
        
        addStatusHistory(savedOrder, OrderStatus.ENCOURS, null, null);
        return savedOrder;
    }

    /**
     * List orders for a store. Automatically distributes unassigned orders among support team members
     * (round-robin by creation date) so each support user gets a fair share.
     */
    public List<Order> findByStoreId(UUID storeId) {
        distributeUnassignedOrdersToSupport(storeId);
        return orderRepository.findByStoreId(storeId);
    }

    /**
     * Assign unassigned orders to support team members in round-robin order (by createdAt)
     * so the workload is divided evenly (e.g. 10 orders, 2 support users → 5 each).
     * Uses distinct users only (by user id) so each person gets a fair share.
     */
    public void distributeUnassignedOrdersToSupport(UUID storeId) {
        List<StoreTeamMember> supportMembers = storeTeamMemberRepository.findSupportMembersWithUserByStoreId(
                storeId, Arrays.asList(StoreTeamRole.SUPPORT, StoreTeamRole.EXTERNAL_SUPPORT));
        // Deduplicate by user id so we round-robin across people, not member rows
        Set<UUID> seenUserIds = new LinkedHashSet<>();
        List<User> users = new ArrayList<>();
        for (StoreTeamMember m : supportMembers) {
            User u = m.getUser();
            if (u != null && u.getId() != null && seenUserIds.add(u.getId())) {
                users.add(u);
            }
        }
        if (users.isEmpty()) {
            return;
        }
        List<Order> unassigned = orderRepository.findUnassignedByStoreIdOrderByCreatedAtAsc(storeId);
        for (int i = 0; i < unassigned.size(); i++) {
            User supportUser = users.get(i % users.size());
            Order order = unassigned.get(i);
            order.setAssignedTo(supportUser);
            orderRepository.save(order);
        }
    }

    public Optional<Order> findById(UUID id) {
        return orderRepository.findById(id);
    }

    public Order updateOrderStatus(UUID orderId, OrderStatus newStatus, String note, UUID changedByUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != newStatus) {
            order.setStatus(newStatus);
            
            // If a support user changes the status, assign the order to them
            User changedBy = null;
            if (changedByUserId != null) {
                changedBy = userRepository.findById(changedByUserId).orElse(null);
                if (changedBy != null && changedBy.getRole().name().equals("SUPPORT")) {
                    // Assign order to the support user who changed the status
                    order.setAssignedTo(changedBy);
                }
            }
            
            orderRepository.saveAndFlush(order); // Use saveAndFlush to ensure immediate database write
            addStatusHistory(order, newStatus, note, changedBy);

            // WhatsApp automation: send message when order is confirmed or delivered
            if (newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CONCLED) {
                try {
                    Store store = order.getStore();
                    if (store != null) {
                        StoreSettings settings = storeService.getOrCreateSettings(store);
                        if (settings.isWhatsappAutomationEnabled() && order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
                            String template = newStatus == OrderStatus.CONFIRMED ? settings.getWhatsappTemplateConfirmed() : settings.getWhatsappTemplateDelivered();
                            if (template != null && !template.isBlank()) {
                                String message = whatsAppService.fillTemplate(template, order);
                                if (!message.isBlank()) {
                                    UUID storeId = store.getId();
                                    whatsAppService.send(storeId, order.getCustomerPhone(), message);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[WhatsApp automation] Failed: " + e.getMessage());
                }
            }

            // Support revenue: credit the handler when order is confirmed (admin-defined price per order).
            // For testing, only CONFIRMED is credited; add CONCLED back when needed for "delivered" pay.
            if (newStatus == OrderStatus.CONFIRMED && order.getAssignedTo() != null) {
                try {
                    Store store = order.getStore();
                    if (store != null) {
                        StoreSettings settings = storeService.getOrCreateSettings(store);
                        BigDecimal amount = settings.getPricePerOrderConfirmedMad();
                        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                            SupportRevenueEntry entry = new SupportRevenueEntry();
                            entry.setUser(order.getAssignedTo());
                            entry.setStore(store);
                            entry.setOrder(order);
                            entry.setStatusCredited(OrderStatus.CONFIRMED);
                            entry.setAmount(amount);
                            supportRevenueEntryRepository.save(entry);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[Support revenue] Failed to credit: " + e.getMessage());
                }
            }
        }

        return order;
    }

    private void addStatusHistory(Order order, OrderStatus status, String note, User changedBy) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(status);
        history.setNote(note);
        history.setChangedBy(changedBy);
        statusHistoryRepository.save(history);
    }

    public List<OrderStatusHistory> getOrderStatusHistory(UUID orderId) {
        return statusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId);
    }

    public List<Order> findByStoreIdAndStatus(UUID storeId, OrderStatus status) {
        return orderRepository.findByStoreIdAndStatus(storeId, status);
    }

    public Order updateOrder(UUID orderId, String customerName, String customerPhone, 
                            String destinationAddress, String city, BigDecimal totalAmount, String currency,
                            String metadata, String productName, String productId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (customerName != null && !customerName.trim().isEmpty()) {
            order.setCustomerName(customerName);
        }
        if (customerPhone != null && !customerPhone.trim().isEmpty()) {
            order.setCustomerPhone(customerPhone);
        }
        if (destinationAddress != null && !destinationAddress.trim().isEmpty()) {
            order.setDestinationAddress(destinationAddress);
        }
        if (city != null) {
            order.setCity(city);
        }
        if (totalAmount != null) {
            order.setTotalAmount(totalAmount);
        }
        if (currency != null && !currency.trim().isEmpty()) {
            order.setCurrency(currency);
        }
        if (metadata != null) {
            order.setMetadata(metadata);
        }
        if (productName != null) {
            order.setProductName(productName);
        }
        if (productId != null) {
            order.setProductId(productId);
        }

        // Use saveAndFlush to ensure immediate database write
        return orderRepository.saveAndFlush(order);
    }
}


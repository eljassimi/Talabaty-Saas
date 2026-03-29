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
                            String externalOrderId, OrderSource source, String productName, String productId,
                            String city, String metadata) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        
        if (externalOrderId != null && !externalOrderId.trim().isEmpty()) {
            Optional<Order> existingOrder = orderRepository.findByStoreIdAndExternalOrderId(storeId, externalOrderId);
            if (existingOrder.isPresent()) {
                
                return existingOrder.get();
            }
        }

        Order order = new Order();
        order.setStore(store);
        order.setCustomerName(customerName);
        order.setCustomerPhone(customerPhone);
        order.setDestinationAddress(destinationAddress);

        BigDecimal finalTotalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        order.setTotalAmount(finalTotalAmount);
        order.setCurrency(currency != null ? currency : "USD");
        order.setExternalOrderId(externalOrderId);
        order.setSource(source != null ? source : OrderSource.API);
        order.setProductName(productName);
        order.setProductId(productId);
        order.setStatus(OrderStatus.ENCOURS);
        if (city != null && !city.isBlank()) {
            order.setCity(city.trim());
        }
        if (metadata != null && !metadata.isBlank()) {
            order.setMetadata(metadata);
        }

        Order savedOrder = orderRepository.save(order);

        addStatusHistory(savedOrder, OrderStatus.ENCOURS, null, null);
        return savedOrder;
    }

    
    public List<Order> findByStoreId(UUID storeId) {
        distributeUnassignedOrdersToSupport(storeId);
        return orderRepository.findByStoreId(storeId);
    }

    
    public void distributeUnassignedOrdersToSupport(UUID storeId) {
        List<StoreTeamMember> supportMembers = storeTeamMemberRepository.findSupportMembersWithUserByStoreId(
                storeId, Arrays.asList(StoreTeamRole.SUPPORT, StoreTeamRole.EXTERNAL_SUPPORT));
        
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
            
            
            User changedBy = null;
            if (changedByUserId != null) {
                changedBy = userRepository.findById(changedByUserId).orElse(null);
                if (changedBy != null && changedBy.getRole().name().equals("SUPPORT")) {
                    
                    order.setAssignedTo(changedBy);
                }
            }
            
            orderRepository.saveAndFlush(order); 
            addStatusHistory(order, newStatus, note, changedBy);

            
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

        
        return orderRepository.saveAndFlush(order);
    }
}


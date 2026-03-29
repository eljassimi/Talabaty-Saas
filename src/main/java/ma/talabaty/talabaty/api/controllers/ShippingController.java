package ma.talabaty.talabaty.api.controllers;

import ma.talabaty.talabaty.core.security.JwtUser;
import ma.talabaty.talabaty.domain.orders.model.Order;
import ma.talabaty.talabaty.domain.orders.repository.OrderRepository;
import ma.talabaty.talabaty.domain.shipping.model.ProviderType;
import ma.talabaty.talabaty.domain.shipping.model.ShippingProvider;
import ma.talabaty.talabaty.domain.shipping.service.OzonExpressService;
import ma.talabaty.talabaty.domain.shipping.service.ShippingProviderService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {

    private final ShippingProviderService providerService;
    private final OzonExpressService ozonExpressService;
    private final OrderRepository orderRepository;

    public ShippingController(ShippingProviderService providerService, 
                              OzonExpressService ozonExpressService,
                              OrderRepository orderRepository) {
        this.providerService = providerService;
        this.ozonExpressService = ozonExpressService;
        this.orderRepository = orderRepository;
    }

    private UUID getAccountIdFromAuth(Authentication authentication) {
        if (authentication.getPrincipal() instanceof JwtUser) {
            JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
            return UUID.fromString(jwtUser.getAccountId());
        }
        
        return UUID.fromString(authentication.getName());
    }

    
    @PostMapping("/providers")
    public ResponseEntity<ShippingProvider> createProvider(@RequestBody CreateProviderRequest request, Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        ShippingProvider provider = providerService.createProvider(
                accountId,
                ProviderType.OZON_EXPRESS,
                request.getCustomerId(),
                request.getApiKey(),
                request.getDisplayName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(provider);
    }

    @GetMapping("/providers")
    public ResponseEntity<List<ShippingProvider>> getProviders(Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        List<ShippingProvider> providers = providerService.getAccountProviders(accountId);
        return ResponseEntity.ok(providers);
    }

    @PutMapping("/providers/{id}")
    public ResponseEntity<ShippingProvider> updateProvider(
            @PathVariable String id,
            @RequestBody UpdateProviderRequest request) {
        ShippingProvider provider = providerService.updateProvider(
                UUID.fromString(id),
                request.getCustomerId(),
                request.getApiKey(),
                request.getDisplayName(),
                request.getActive()
        );
        return ResponseEntity.ok(provider);
    }

    @DeleteMapping("/providers/{id}")
    public ResponseEntity<Void> deleteProvider(@PathVariable String id) {
        providerService.deleteProvider(UUID.fromString(id));
        return ResponseEntity.noContent().build();
    }

    
    @PostMapping("/ozon-express/parcels")
    public ResponseEntity<Map<String, Object>> createParcel(
            @RequestBody CreateParcelRequest request,
            Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        
        ShippingProvider provider = providerService.getActiveProvider(accountId, ProviderType.OZON_EXPRESS)
                .orElseThrow(() -> new RuntimeException("Ozon Express provider not configured for this account"));

        OzonExpressService.CreateParcelRequest ozonRequest = new OzonExpressService.CreateParcelRequest();
        ozonRequest.setTrackingNumber(request.getTrackingNumber());
        ozonRequest.setReceiver(request.getReceiver());
        ozonRequest.setPhone(request.getPhone());
        ozonRequest.setCityId(request.getCityId());
        ozonRequest.setAddress(request.getAddress());
        ozonRequest.setNote(request.getNote());
        ozonRequest.setPrice(request.getPrice());
        ozonRequest.setNature(request.getNature());
        ozonRequest.setStock(request.getStock() != null ? request.getStock() : 1);
        ozonRequest.setOpen(request.getOpen() != null ? request.getOpen() : 1);
        ozonRequest.setFragile(request.getFragile() != null ? request.getFragile() : 0);
        ozonRequest.setReplace(request.getReplace() != null ? request.getReplace() : 0);
        ozonRequest.setProducts(request.getProducts());

        Map<String, Object> result = ozonExpressService.createParcel(
                provider.getCustomerId(),
                provider.getApiKey(),
                ozonRequest
        );

        
        if (request.getOrderId() != null && result.containsKey("TRACKING-NUMBER")) {
            Order order = orderRepository.findById(UUID.fromString(request.getOrderId()))
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            order.setOzonTrackingNumber((String) result.get("TRACKING-NUMBER"));
            orderRepository.save(order);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/ozon-express/parcels/info")
    public ResponseEntity<Map<String, Object>> getParcelInfo(
            @RequestBody ParcelInfoRequest request,
            Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        
        ShippingProvider provider = providerService.getActiveProvider(accountId, ProviderType.OZON_EXPRESS)
                .orElseThrow(() -> new RuntimeException("Ozon Express provider not configured for this account"));

        Map<String, Object> result = ozonExpressService.getParcelInfo(
                provider.getCustomerId(),
                provider.getApiKey(),
                request.getTrackingNumber()
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping("/ozon-express/parcels/track")
    public ResponseEntity<Map<String, Object>> trackParcel(
            @RequestBody TrackParcelRequest request,
            Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        
        ShippingProvider provider = providerService.getActiveProvider(accountId, ProviderType.OZON_EXPRESS)
                .orElseThrow(() -> new RuntimeException("Ozon Express provider not configured for this account"));

        
        if ((request.getTrackingNumber() == null || request.getTrackingNumber().trim().isEmpty()) &&
            (request.getTrackingNumbers() == null || request.getTrackingNumbers().isEmpty())) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Tracking number is required");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            Map<String, Object> result;
            if (request.getTrackingNumbers() != null && !request.getTrackingNumbers().isEmpty()) {
                
                result = ozonExpressService.trackMultipleParcels(
                        provider.getCustomerId(),
                        provider.getApiKey(),
                        request.getTrackingNumbers()
                );
            } else {
                
                result = ozonExpressService.trackParcel(
                        provider.getCustomerId(),
                        provider.getApiKey(),
                        request.getTrackingNumber()
                );
            }

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("status", "error");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/ozon-express/delivery-notes")
    public ResponseEntity<Map<String, Object>> createDeliveryNote(Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        
        ShippingProvider provider = providerService.getActiveProvider(accountId, ProviderType.OZON_EXPRESS)
                .orElseThrow(() -> new RuntimeException("Ozon Express provider not configured for this account"));

        Map<String, Object> result = ozonExpressService.createDeliveryNote(
                provider.getCustomerId(),
                provider.getApiKey()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/ozon-express/delivery-notes/{ref}/parcels")
    public ResponseEntity<Map<String, Object>> addParcelsToDeliveryNote(
            @PathVariable String ref,
            @RequestBody AddParcelsRequest request,
            Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        
        ShippingProvider provider = providerService.getActiveProvider(accountId, ProviderType.OZON_EXPRESS)
                .orElseThrow(() -> new RuntimeException("Ozon Express provider not configured for this account"));

        Map<String, Object> result = ozonExpressService.addParcelsToDeliveryNote(
                provider.getCustomerId(),
                provider.getApiKey(),
                ref,
                request.getTrackingNumbers()
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping("/ozon-express/delivery-notes/{ref}/save")
    public ResponseEntity<Map<String, Object>> saveDeliveryNote(
            @PathVariable String ref,
            Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        
        ShippingProvider provider = providerService.getActiveProvider(accountId, ProviderType.OZON_EXPRESS)
                .orElseThrow(() -> new RuntimeException("Ozon Express provider not configured for this account"));

        Map<String, Object> result = ozonExpressService.saveDeliveryNote(
                provider.getCustomerId(),
                provider.getApiKey(),
                ref
        );

        return ResponseEntity.ok(result);
    }

    
    @PostMapping("/ozon-express/delivery-notes/create-full")
    public ResponseEntity<Map<String, Object>> createFullDeliveryNote(
            @RequestBody CreateFullDeliveryNoteRequest request,
            Authentication authentication) {
        try {
            System.err.println("[Bon de Livraison] create-full called, orderIds=" + (request != null && request.getOrderIds() != null ? request.getOrderIds() : "null"));
            UUID accountId = getAccountIdFromAuth(authentication);

            ShippingProvider provider = providerService.getActiveProvider(accountId, ProviderType.OZON_EXPRESS)
                    .orElseThrow(() -> new RuntimeException("Ozon Express provider not configured for this account"));

            List<String> rawIds = request.getOrderIds();
            if (rawIds == null || rawIds.isEmpty()) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "At least one order is required");
                return ResponseEntity.badRequest().body(err);
            }
            List<UUID> orderIds = new ArrayList<>();
            for (String id : rawIds) {
                try {
                    orderIds.add(UUID.fromString(id));
                } catch (IllegalArgumentException e) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("error", "Invalid order ID format: " + id);
                    return ResponseEntity.badRequest().body(err);
                }
            }

            List<Order> orders = orderRepository.findAllByIdWithStoreAndAccount(orderIds);
            List<Order> allowed = orders.stream()
                    .filter(o -> o.getStore() != null && o.getStore().getAccount() != null
                            && accountId.equals(o.getStore().getAccount().getId()))
                    .collect(Collectors.toList());
            if (allowed.size() != orders.size()) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Some orders were not found or do not belong to your account");
                return ResponseEntity.badRequest().body(err);
            }

            List<String> trackingNumbers = allowed.stream()
                    .map(Order::getOzonTrackingNumber)
                    .filter(t -> t != null && !t.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            if (trackingNumbers.isEmpty()) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Selected orders have no Ozon Express tracking numbers. Send orders to shipping first.");
                return ResponseEntity.badRequest().body(err);
            }

            String existingRef = null;
            List<String> refs = allowed.stream()
                    .map(Order::getDeliveryNoteRef)
                    .filter(r -> r != null && !r.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());
            long withRef = allowed.stream()
                    .filter(o -> o.getDeliveryNoteRef() != null && !o.getDeliveryNoteRef().trim().isEmpty())
                    .count();
            if (withRef == allowed.size() && refs.size() == 1) {
                existingRef = refs.get(0);
            }
            if (existingRef != null) {
                String base = "https://client.ozoneexpress.ma";
                String dnRef = "?dn-ref=" + existingRef;
                Map<String, Object> response = new HashMap<>();
                response.put("ref", existingRef);
                response.put("pdfUrl", base + "/pdf-delivery-note" + dnRef);
                response.put("pdfTicketsUrl", base + "/pdf-delivery-note-tickets" + dnRef);
                response.put("pdfTickets4x4Url", base + "/pdf-delivery-note-tickets-4-4" + dnRef);
                response.put("trackingCount", trackingNumbers.size());
                response.put("existing", true);
                return ResponseEntity.ok(response);
            }

            Map<String, Object> createResult = ozonExpressService.createDeliveryNote(
                    provider.getCustomerId(),
                    provider.getApiKey());
            String ref = extractDeliveryNoteRef(createResult);
            if (ref == null || ref.trim().isEmpty()) {
                String ozonMessage = extractOzonErrorMessage(createResult);
                Map<String, Object> err = new HashMap<>();
                if (ozonMessage != null && !ozonMessage.isBlank()) {
                    err.put("error", ozonMessage);
                } else {
                    err.put("error", "Ozon Express did not return a delivery note reference. Response: " + createResult);
                }
                return ResponseEntity.badRequest().body(err);
            }
            ref = ref.trim();

            Map<String, Object> addParcelsResult = ozonExpressService.addParcelsToDeliveryNote(
                    provider.getCustomerId(),
                    provider.getApiKey(),
                    ref,
                    trackingNumbers);
            if (!isOzonSuccess(addParcelsResult)) {
                String msg = getOzonErrorMessageFromMap(addParcelsResult);
                Map<String, Object> err = new HashMap<>();
                err.put("error", msg != null ? msg : "Les colis n'ont pas été enregistrés sur Ozon. Vérifiez les numéros de suivi.");
                return ResponseEntity.badRequest().body(err);
            }

            Map<String, Object> saveResult = ozonExpressService.saveDeliveryNote(
                    provider.getCustomerId(),
                    provider.getApiKey(),
                    ref);
            if (!isOzonSuccess(saveResult)) {
                String msg = getOzonErrorMessageFromMap(saveResult);
                if (msg != null && msg.toLowerCase().contains("ne pouvez pas enregistrer")) {
                    msg = "Ozon refuse l'enregistrement du BL. Vérifiez que les numéros de suivi sont valides et que les colis sont bien envoyés chez Ozon. Si le BL existe déjà sur Ozon, sélectionnez uniquement des commandes sans BL.";
                } else if (msg == null) {
                    msg = "Le Bon de Livraison n'a pas été sauvegardé sur Ozon. Les PDF ne seront pas générés.";
                }
                Map<String, Object> err = new HashMap<>();
                err.put("error", msg);
                return ResponseEntity.badRequest().body(err);
            }

            
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            for (Order o : allowed) {
                o.setDeliveryNoteRef(ref);
            }
            orderRepository.saveAll(allowed);

            String base = "https://client.ozoneexpress.ma";
            String dnRef = "?dn-ref=" + ref;
            Map<String, Object> response = new HashMap<>();
            response.put("ref", ref);
            response.put("pdfUrl", base + "/pdf-delivery-note" + dnRef);
            response.put("pdfTicketsUrl", base + "/pdf-delivery-note-tickets" + dnRef);
            response.put("pdfTickets4x4Url", base + "/pdf-delivery-note-tickets-4-4" + dnRef);
            response.put("trackingCount", trackingNumbers.size());
            response.put("existing", false);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            System.err.println("[Bon de Livraison] RuntimeException: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage() != null ? e.getMessage() : "Failed to create Bon de Livraison");
            return ResponseEntity.badRequest().body(err);
        } catch (Exception e) {
            System.err.println("[Bon de Livraison] Exception: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> err = new HashMap<>();
            String msg = e.getMessage();
            if (msg != null && !msg.isBlank()) {
                err.put("error", msg);
            } else {
                Throwable c = e.getCause();
                err.put("error", c != null && c.getMessage() != null ? c.getMessage() : "Bon de Livraison failed (" + e.getClass().getSimpleName() + "). Check server logs.");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        } catch (Throwable t) {
            System.err.println("[Bon de Livraison] Throwable: " + t.getClass().getName() + " - " + t.getMessage());
            t.printStackTrace();
            Map<String, Object> err = new HashMap<>();
            String msg = t.getMessage();
            err.put("error", msg != null && !msg.isBlank() ? msg : "Bon de Livraison failed (" + t.getClass().getSimpleName() + "). Check server logs.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    
    @SuppressWarnings("unchecked")
    private String extractOzonErrorMessage(Map<String, Object> createResult) {
        if (createResult == null) return null;
        Object addBl = createResult.get("ADD-BL");
        if (addBl instanceof Map) {
            Map<String, Object> addBlMap = (Map<String, Object>) addBl;
            Object msg = addBlMap.get("MESSAGE");
            if (msg != null) {
                String s = String.valueOf(msg).trim();
                if (s.toLowerCase().contains("ajouter") && s.toLowerCase().contains("colis")) {
                    return "Ces colis sont peut-être déjà dans un Bon de Livraison sur Ozon. Sélectionnez des commandes sans BL existant, ou consultez le BL déjà créé.";
                }
                return s;
            }
        }
        return null;
    }

    
    @SuppressWarnings("unchecked")
    private String extractDeliveryNoteRef(Map<String, Object> createResult) {
        if (createResult == null) return null;
        if (createResult.containsKey("ref")) return String.valueOf(createResult.get("ref"));
        if (createResult.containsKey("Ref")) return String.valueOf(createResult.get("Ref"));
        if (createResult.containsKey("REF")) return String.valueOf(createResult.get("REF"));
        Object addBl = createResult.get("ADD-BL");
        if (addBl instanceof Map) {
            Object newBl = ((Map<String, Object>) addBl).get("NEW-BL");
            if (newBl instanceof Map) {
                Object refObj = ((Map<String, Object>) newBl).get("REF");
                if (refObj != null) return String.valueOf(refObj).trim();
            }
        }
        return null;
    }

    
    @SuppressWarnings("unchecked")
    private boolean isOzonSuccess(Map<String, Object> map) {
        if (map == null) return true;
        Object result = map.get("RESULT");
        if (result != null && "ERROR".equalsIgnoreCase(String.valueOf(result))) return false;
        for (Object value : map.values()) {
            if (value instanceof Map && !isOzonSuccess((Map<String, Object>) value)) return false;
        }
        return true;
    }

    
    @SuppressWarnings("unchecked")
    private String getOzonErrorMessageFromMap(Map<String, Object> map) {
        if (map == null) return null;
        Object result = map.get("RESULT");
        if (result != null && "ERROR".equalsIgnoreCase(String.valueOf(result))) {
            Object msg = map.get("MESSAGE");
            if (msg != null) return String.valueOf(msg).trim();
            return "Ozon a retourné une erreur.";
        }
        for (Object value : map.values()) {
            if (value instanceof Map) {
                String nested = getOzonErrorMessageFromMap((Map<String, Object>) value);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    
    @GetMapping(value = "/ozon-express/delivery-notes/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getDeliveryNotePdf(
            @RequestParam String ref,
            @RequestParam(defaultValue = "standard") String type,
            Authentication authentication) {
        UUID accountId = getAccountIdFromAuth(authentication);
        ShippingProvider provider = providerService.getActiveProvider(accountId, ProviderType.OZON_EXPRESS)
                .orElseThrow(() -> new RuntimeException("Ozon Express non configuré"));
        byte[] pdf = ozonExpressService.fetchDeliveryNotePdf(ref, type, provider.getCustomerId(), provider.getApiKey());
        String filename = "bon-livraison-" + ref.replaceAll("[^a-zA-Z0-9-]", "_") + ".pdf";
        if ("tickets".equals(type)) filename = "etiquettes-a4-" + ref.replaceAll("[^a-zA-Z0-9-]", "_") + ".pdf";
        if ("tickets-4-4".equals(type)) filename = "etiquettes-10x10-" + ref.replaceAll("[^a-zA-Z0-9-]", "_") + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping("/ozon-express/cities")
    public ResponseEntity<List<Map<String, Object>>> getCities() {
        List<Map<String, Object>> cities = ozonExpressService.getCities();
        return ResponseEntity.ok(cities);
    }

    
    public static class CreateProviderRequest {
        private String customerId;
        private String apiKey;
        private String displayName;

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }

    public static class UpdateProviderRequest {
        private String customerId;
        private String apiKey;
        private String displayName;
        private Boolean active;

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    public static class CreateParcelRequest {
        private String orderId; 
        private String trackingNumber;
        private String receiver;
        private String phone;
        private String cityId;
        private String address;
        private String note;
        private Double price;
        private String nature;
        private Integer stock;
        private Integer open;
        private Integer fragile;
        private Integer replace;
        private String products;

        
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getTrackingNumber() { return trackingNumber; }
        public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
        public String getReceiver() { return receiver; }
        public void setReceiver(String receiver) { this.receiver = receiver; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getCityId() { return cityId; }
        public void setCityId(String cityId) { this.cityId = cityId; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public String getNature() { return nature; }
        public void setNature(String nature) { this.nature = nature; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
        public Integer getOpen() { return open; }
        public void setOpen(Integer open) { this.open = open; }
        public Integer getFragile() { return fragile; }
        public void setFragile(Integer fragile) { this.fragile = fragile; }
        public Integer getReplace() { return replace; }
        public void setReplace(Integer replace) { this.replace = replace; }
        public String getProducts() { return products; }
        public void setProducts(String products) { this.products = products; }
    }

    public static class ParcelInfoRequest {
        private String trackingNumber;

        public String getTrackingNumber() { return trackingNumber; }
        public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    }

    public static class TrackParcelRequest {
        private String trackingNumber;
        private List<String> trackingNumbers; 

        public String getTrackingNumber() { return trackingNumber; }
        public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
        public List<String> getTrackingNumbers() { return trackingNumbers; }
        public void setTrackingNumbers(List<String> trackingNumbers) { this.trackingNumbers = trackingNumbers; }
    }

    public static class AddParcelsRequest {
        private List<String> trackingNumbers;

        public List<String> getTrackingNumbers() { return trackingNumbers; }
        public void setTrackingNumbers(List<String> trackingNumbers) { this.trackingNumbers = trackingNumbers; }
    }

    public static class CreateFullDeliveryNoteRequest {
        private List<String> orderIds;

        public List<String> getOrderIds() { return orderIds; }
        public void setOrderIds(List<String> orderIds) { this.orderIds = orderIds; }
    }
}


package ma.talabaty.talabaty.api.controllers;

import ma.talabaty.talabaty.core.security.AuthenticationHelper;
import ma.talabaty.talabaty.core.security.PermissionChecker;
import ma.talabaty.talabaty.domain.users.model.User;
import ma.talabaty.talabaty.domain.users.repository.UserRepository;
import ma.talabaty.talabaty.domain.whatsapp.WhatsAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;
    private final PermissionChecker permissionChecker;
    private final UserRepository userRepository;

    public WhatsAppController(WhatsAppService whatsAppService,
                              PermissionChecker permissionChecker,
                              UserRepository userRepository) {
        this.whatsAppService = whatsAppService;
        this.permissionChecker = permissionChecker;
        this.userRepository = userRepository;
    }

    
    @GetMapping("/link-status")
    public ResponseEntity<Map<String, Object>> getLinkStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = AuthenticationHelper.getUserIdFromAuth(authentication);
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && !permissionChecker.canAccessIntegrations(user.getRole())) {
            return ResponseEntity.status(403).build();
        }
        Map<String, Object> body = new HashMap<>();
        body.put("configured", whatsAppService.isConfigured());
        if (whatsAppService.isTwilioConfigured()) {
            body.put("provider", "twilio");
            body.put("ready", true);
            return ResponseEntity.ok(body);
        }
        if (whatsAppService.isLocalBridgeConfigured()) {
            body.put("provider", "bridge");
            
            Map<String, Object> bridge = whatsAppService.getBridgeLinkStatus();
            body.put("ready", bridge.getOrDefault("ready", false));
            if (bridge.containsKey("qr")) {
                body.put("qr", bridge.get("qr"));
            }
            body.put("initializing", bridge.getOrDefault("initializing", false));
            if (bridge.containsKey("error")) {
                body.put("error", bridge.get("error"));
            }
            if (bridge.containsKey("bridgeError")) {
                body.put("bridgeError", bridge.get("bridgeError"));
            }
            if (bridge.containsKey("reason")) {
                body.put("reason", bridge.get("reason"));
            }
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.ok(body);
    }
}

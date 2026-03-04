package ma.talabaty.talabaty.api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/auth-info")
    public ResponseEntity<Map<String, Object>> getAuthInfo(Authentication authentication) {
        Map<String, Object> info = new HashMap<>();
        
        if (authentication == null) {
            info.put("authenticated", false);
            info.put("message", "No authentication found");
            return ResponseEntity.ok(info);
        }
        
        info.put("authenticated", true);
        info.put("principalType", authentication.getPrincipal() != null 
            ? authentication.getPrincipal().getClass().getName() 
            : "null");
        info.put("principal", authentication.getPrincipal() != null 
            ? authentication.getPrincipal().toString() 
            : "null");
        info.put("name", authentication.getName());
        info.put("authorities", authentication.getAuthorities().toString());
        
        if (authentication.getPrincipal() instanceof ma.talabaty.talabaty.core.security.JwtUser) {
            ma.talabaty.talabaty.core.security.JwtUser jwtUser = 
                (ma.talabaty.talabaty.core.security.JwtUser) authentication.getPrincipal();
            Map<String, String> jwtUserInfo = new HashMap<>();
            jwtUserInfo.put("userId", jwtUser.getUserId());
            jwtUserInfo.put("accountId", jwtUser.getAccountId());
            jwtUserInfo.put("email", jwtUser.getEmail());
            info.put("jwtUser", jwtUserInfo);
        }
        
        return ResponseEntity.ok(info);
    }
}


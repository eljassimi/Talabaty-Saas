package ma.talabaty.talabaty.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.talabaty.talabaty.domain.credentials.service.ApiCredentialService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiCredentialService credentialService;

    public ApiKeyAuthenticationFilter(ApiCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Skip authentication for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Only process API key authentication if JWT token is not present
        // This prevents API key from overriding JWT authentication
        String authHeader = request.getHeader("Authorization");
        boolean hasJwtToken = authHeader != null && authHeader.startsWith("Bearer ");
        
        if (!hasJwtToken) {
            String publicKey = request.getHeader("X-API-Key");
            String secretKey = request.getHeader("X-API-Secret");

            if (publicKey != null && secretKey != null) {
                if (credentialService.validateCredentials(publicKey, secretKey)) {
                    var credential = credentialService.findByPublicKey(publicKey).orElse(null);
                    if (credential != null) {
                        credentialService.updateLastUsed(credential.getId());
                        
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        credential.getAccount().getId().toString(),
                                        null,
                                        new ArrayList<>()
                                );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}


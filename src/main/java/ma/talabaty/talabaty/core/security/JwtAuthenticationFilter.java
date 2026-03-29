package ma.talabaty.talabaty.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            
            if (token == null || token.trim().isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }
            
            
            if (!tokenProvider.validateToken(token)) {
                String validationError = tokenProvider.getTokenValidationError(token);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                String errorMessage = validationError != null 
                    ? validationError + ". Please login again to get a new token."
                    : "Invalid or expired token. Please login again.";
                response.getWriter().write("{\"error\": \"" + errorMessage + "\", \"code\": \"INVALID_TOKEN\"}");
                return;
            }
            
            try {
                String userId = tokenProvider.getUserIdFromToken(token);
                String email = tokenProvider.getEmailFromToken(token);
                String accountId = tokenProvider.getAccountIdFromToken(token);

                
                if (accountId == null || accountId.trim().isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Invalid token: accountId is missing from token. Please login again.\", \"code\": \"INVALID_TOKEN\"}");
                    return;
                }
                
                
                if (userId == null || userId.trim().isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Invalid token: userId is missing from token. Please login again.\", \"code\": \"INVALID_TOKEN\"}");
                    return;
                }

                
                JwtUser jwtUser = new JwtUser(userId, accountId, email);
                
                
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(jwtUser, null, new ArrayList<>());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                String errorMsg = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "Unknown error";
                response.getWriter().write("{\"error\": \"Error processing token: " + errorMsg + ". Please login again.\", \"code\": \"TOKEN_PROCESSING_ERROR\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}


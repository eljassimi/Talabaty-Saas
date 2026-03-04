package ma.talabaty.talabaty.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:your-secret-key-change-in-production-min-256-bits}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration:3600}")
    private Long accessTokenExpiration; // seconds

    @Value("${jwt.refresh-token-expiration:86400}")
    private Long refreshTokenExpiration; // seconds

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String userId, String email, String accountId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("accountId", accountId);
        return generateToken(claims, userId, accessTokenExpiration);
    }

    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        return generateToken(claims, userId, refreshTokenExpiration);
    }

    private String generateToken(Map<String, Object> claims, String subject, Long expiration) {
        Instant now = Instant.now();
        Instant expirationTime = now.plus(expiration, ChronoUnit.SECONDS);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", String.class);
    }

    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("email", String.class);
    }

    public String getAccountIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        // Try to get accountId as String first (most reliable)
        try {
            String accountId = claims.get("accountId", String.class);
            if (accountId != null && !accountId.trim().isEmpty()) {
                return accountId;
            }
        } catch (Exception e) {
            // Fall through to try as Object
        }
        
        // Fallback: try as Object
        Object accountIdObj = claims.get("accountId");
        if (accountIdObj != null) {
            return accountIdObj.toString();
        }
        
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            // Also check if token is expired
            if (claims.getExpiration() != null && claims.getExpiration().before(new Date())) {
                return false;
            }
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return false;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getTokenValidationError(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims.getExpiration() != null && claims.getExpiration().before(new Date())) {
                return "Token is expired";
            }
            return null; // Token is valid
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return "Token is expired: " + e.getMessage();
        } catch (io.jsonwebtoken.security.SignatureException e) {
            return "Invalid token signature. The JWT secret key may not match: " + e.getMessage();
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            return "Malformed token: " + e.getMessage();
        } catch (Exception e) {
            return "Token validation error: " + e.getMessage();
        }
    }
}


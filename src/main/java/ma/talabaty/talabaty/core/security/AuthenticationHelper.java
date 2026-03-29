package ma.talabaty.talabaty.core.security;

import ma.talabaty.talabaty.core.exceptions.AuthenticationException;
import ma.talabaty.talabaty.core.exceptions.InvalidAccountIdException;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public class AuthenticationHelper {

    public static UUID getAccountIdFromAuth(Authentication authentication) {
        if (authentication == null) {
            throw new AuthenticationException("Authentication is required. Please provide a valid JWT token in the Authorization header.");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal == null) {
            throw new AuthenticationException("Authentication principal is null. The token may be invalid or expired.");
        }
        
        
        if (principal instanceof JwtUser) {
            JwtUser jwtUser = (JwtUser) principal;
            String accountId = jwtUser.getAccountId();
            if (accountId == null || accountId.trim().isEmpty()) {
                throw new InvalidAccountIdException("Account ID is missing from authentication token. Please login again to get a new token.");
            }
            try {
                return UUID.fromString(accountId);
            } catch (IllegalArgumentException e) {
                throw new InvalidAccountIdException("Invalid account ID format in token: '" + accountId + "'. Please login again.", e);
            }
        }
        
        
        String accountIdStr = null;
        if (principal instanceof String) {
            accountIdStr = (String) principal;
        } else if (principal instanceof UUID) {
            accountIdStr = principal.toString();
        } else {
            
            String principalStr = principal.toString();
            if (principalStr != null && !principalStr.trim().isEmpty() && !principalStr.equals("null")) {
                accountIdStr = principalStr;
            } else {
                
                accountIdStr = authentication.getName();
            }
        }
        
        if (accountIdStr == null || accountIdStr.trim().isEmpty()) {
            String principalInfo = "Principal type: " + principal.getClass().getName();
            if (principal instanceof String) {
                principalInfo += ", Principal value: '" + principal + "'";
            } else {
                principalInfo += ", Principal toString: '" + principal.toString() + "'";
            }
            principalInfo += ", Authentication name: '" + authentication.getName() + "'";
            
            throw new InvalidAccountIdException(
                "Unable to extract account ID from authentication. " +
                principalInfo + ". " +
                "This usually means the JWT authentication filter did not process your token correctly. " +
                "Possible causes: 1) Token is invalid or expired, 2) JWT secret key mismatch, 3) Token format is incorrect. " +
                "Please ensure you are using a valid JWT token with 'Bearer ' prefix in the Authorization header. " +
                "Try logging in again to get a new token."
            );
        }
        
        try {
            return UUID.fromString(accountIdStr);
        } catch (IllegalArgumentException e) {
            
            if (accountIdStr == null || accountIdStr.trim().isEmpty()) {
                throw new InvalidAccountIdException(
                    "Unable to extract account ID from authentication. " +
                    "Principal type: " + principal.getClass().getName() + ". " +
                    "Principal toString: '" + principal.toString() + "'. " +
                    "Authentication name: '" + authentication.getName() + "'. " +
                    "The JWT token may be invalid or the authentication filter may not have processed it correctly. " +
                    "Please ensure you are using a valid JWT token with 'Bearer ' prefix in the Authorization header. " +
                    "Try logging in again to get a new token."
                );
            }
            throw new InvalidAccountIdException(
                "Invalid account ID format: '" + accountIdStr + "'. " +
                "Principal type: " + principal.getClass().getName() + ". " +
                "Please login again to get a new token.",
                e
            );
        }
    }

    public static UUID getUserIdFromAuth(Authentication authentication) {
        if (authentication == null) {
            throw new AuthenticationException("Authentication is required. Please provide a valid JWT token in the Authorization header.");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal == null) {
            throw new AuthenticationException("Authentication principal is null. The token may be invalid or expired.");
        }
        
        
        if (principal instanceof JwtUser) {
            JwtUser jwtUser = (JwtUser) principal;
            String userId = jwtUser.getUserId();
            if (userId == null || userId.trim().isEmpty()) {
                throw new AuthenticationException("User ID is missing from authentication token. Please login again.");
            }
            try {
                return UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                throw new AuthenticationException("Invalid user ID format in token: '" + userId + "'. Please login again.", e);
            }
        }
        
        
        String userIdStr = authentication.getName();
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            throw new AuthenticationException(
                "Unable to extract user ID from authentication. " +
                "Principal type: " + principal.getClass().getName() + ". " +
                "Please ensure you are using a valid JWT token."
            );
        }
        
        try {
            return UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException("Invalid user ID format: '" + userIdStr + "'. Please login again.", e);
        }
    }
}


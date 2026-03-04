package ma.talabaty.talabaty.core.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidAccountIdException.class)
    public ResponseEntity<Map<String, String>> handleInvalidAccountIdException(InvalidAccountIdException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        error.put("code", "INVALID_ACCOUNT_ID");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(ma.talabaty.talabaty.core.exceptions.AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(ma.talabaty.talabaty.core.exceptions.AuthenticationException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        error.put("code", "AUTHENTICATION_ERROR");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        String message = ex.getMessage();
        if (message != null && message.contains("UUID")) {
            // Check if this is related to authentication (empty string case)
            if (message.contains("Invalid UUID string: ") && (message.trim().endsWith(":") || message.trim().endsWith(": "))) {
                error.put("error", "Unable to extract account ID from authentication token. The JWT token may be invalid, expired, or not properly processed. Please ensure you are using a valid JWT token with 'Bearer ' prefix in the Authorization header. Try logging in again to get a new token.");
                error.put("code", "AUTHENTICATION_ERROR");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            error.put("error", "Invalid UUID format: " + message);
            error.put("code", "INVALID_UUID");
        } else {
            error.put("error", ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        String message = ex.getMessage() != null ? ex.getMessage() : "An error occurred";
        
        // Log the exception for debugging
        System.err.println("DEBUG GlobalExceptionHandler: RuntimeException caught: " + message);
        System.err.println("DEBUG GlobalExceptionHandler: Exception class: " + ex.getClass().getName());
        ex.printStackTrace();
        
        // Check if this is a store access error
        if (message != null && (message.contains("does not belong to your account") || message.contains("Store not found"))) {
            error.put("error", message);
            error.put("code", "STORE_ACCESS_DENIED");
            error.put("message", message); // Also include in 'message' field for frontend compatibility
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
        
        error.put("error", message);
        error.put("message", message); // Also include in 'message' field for frontend compatibility
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}


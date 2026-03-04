package ma.talabaty.talabaty.core.exceptions;

public class InvalidAccountIdException extends AuthenticationException {
    
    public InvalidAccountIdException(String message) {
        super(message);
    }
    
    public InvalidAccountIdException(String message, Throwable cause) {
        super(message, cause);
    }
}


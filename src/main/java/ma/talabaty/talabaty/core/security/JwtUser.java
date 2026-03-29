package ma.talabaty.talabaty.core.security;

import java.io.Serializable;

public class JwtUser implements Serializable {
    private final String userId;
    private final String accountId;
    private final String email;

    public JwtUser(String userId, String accountId, String email) {
        this.userId = userId;
        this.accountId = accountId;
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return accountId; 
    }
}


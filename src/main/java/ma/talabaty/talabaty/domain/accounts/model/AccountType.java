package ma.talabaty.talabaty.domain.accounts.model;

public enum AccountType {
    INDIVIDUAL,
    COMPANY;

    public enum AccountStatus {
        ACTIVE,
        SUSPENDED,
        DELETED
    }
}



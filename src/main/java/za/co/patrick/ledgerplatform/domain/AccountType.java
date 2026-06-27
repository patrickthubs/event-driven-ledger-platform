package za.co.patrick.ledgerplatform.domain;

public enum AccountType {
    ASSET(EntryDirection.DEBIT),
    LIABILITY(EntryDirection.CREDIT),
    EQUITY(EntryDirection.CREDIT),
    REVENUE(EntryDirection.CREDIT),
    EXPENSE(EntryDirection.DEBIT);

    private final EntryDirection normalBalanceSide;

    AccountType(EntryDirection normalBalanceSide) {
        this.normalBalanceSide = normalBalanceSide;
    }

    public EntryDirection normalBalanceSide() {
        return normalBalanceSide;
    }
}

package za.co.patrick.ledgerplatform.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import za.co.patrick.ledgerplatform.domain.AccountStatus;
import za.co.patrick.ledgerplatform.domain.AccountType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_account")
public class LedgerAccountEntity {

    @Id
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true, length = 50)
    private String accountNumber;

    @Column(name = "account_name", nullable = false, length = 120)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "allow_negative_balance", nullable = false)
    private boolean allowNegativeBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected LedgerAccountEntity() {
    }

    public LedgerAccountEntity(
            UUID id,
            String accountNumber,
            String accountName,
            AccountType accountType,
            String currency,
            boolean allowNegativeBalance,
            AccountStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.accountType = accountType;
        this.currency = currency;
        this.allowNegativeBalance = allowNegativeBalance;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isAllowNegativeBalance() {
        return allowNegativeBalance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}

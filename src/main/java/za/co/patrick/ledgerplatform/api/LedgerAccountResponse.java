package za.co.patrick.ledgerplatform.api;

import za.co.patrick.ledgerplatform.domain.AccountStatus;
import za.co.patrick.ledgerplatform.domain.AccountType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LedgerAccountResponse(
        UUID accountId,
        String accountNumber,
        String accountName,
        AccountType accountType,
        String currency,
        boolean allowNegativeBalance,
        AccountStatus status,
        BigDecimal currentBalance,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

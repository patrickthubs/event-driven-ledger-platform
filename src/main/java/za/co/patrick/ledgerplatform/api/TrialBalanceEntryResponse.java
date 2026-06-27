package za.co.patrick.ledgerplatform.api;

import za.co.patrick.ledgerplatform.domain.AccountType;
import za.co.patrick.ledgerplatform.domain.EntryDirection;

import java.math.BigDecimal;
import java.util.UUID;

public record TrialBalanceEntryResponse(
        UUID accountId,
        String accountNumber,
        String accountName,
        AccountType accountType,
        EntryDirection balanceSide,
        BigDecimal debitBalance,
        BigDecimal creditBalance
) {
}

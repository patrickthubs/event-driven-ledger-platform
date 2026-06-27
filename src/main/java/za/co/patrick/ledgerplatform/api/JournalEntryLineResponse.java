package za.co.patrick.ledgerplatform.api;

import za.co.patrick.ledgerplatform.domain.EntryDirection;

import java.math.BigDecimal;
import java.util.UUID;

public record JournalEntryLineResponse(
        UUID lineId,
        UUID accountId,
        String accountNumber,
        String accountName,
        EntryDirection direction,
        BigDecimal amount,
        String narrative
) {
}

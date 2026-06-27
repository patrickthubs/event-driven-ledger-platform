package za.co.patrick.ledgerplatform.api;

import za.co.patrick.ledgerplatform.domain.EntryDirection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountStatementEntryResponse(
        UUID journalEntryId,
        UUID journalEntryLineId,
        OffsetDateTime effectiveAt,
        String externalReference,
        String description,
        EntryDirection direction,
        BigDecimal amount,
        String narrative,
        BigDecimal runningBalance
) {
}

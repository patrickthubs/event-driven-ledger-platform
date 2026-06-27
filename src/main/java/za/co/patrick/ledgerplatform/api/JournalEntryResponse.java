package za.co.patrick.ledgerplatform.api;

import za.co.patrick.ledgerplatform.domain.JournalEntryStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record JournalEntryResponse(
        UUID journalEntryId,
        String idempotencyKey,
        String externalReference,
        String description,
        String currency,
        OffsetDateTime effectiveAt,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        JournalEntryStatus status,
        OffsetDateTime createdAt,
        UUID reversalOfJournalEntryId,
        String reversalReason,
        List<JournalEntryLineResponse> lines
) {
}

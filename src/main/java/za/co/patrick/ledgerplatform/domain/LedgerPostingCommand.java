package za.co.patrick.ledgerplatform.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LedgerPostingCommand(
        String idempotencyKey,
        String externalReference,
        String description,
        String currency,
        OffsetDateTime effectiveAt,
        List<LedgerPostingLine> lines
) {
    public record LedgerPostingLine(
            UUID accountId,
            EntryDirection direction,
            BigDecimal amount,
            String narrative
    ) {
    }
}

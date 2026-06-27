package za.co.patrick.ledgerplatform.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AccountStatementResponse(
        UUID accountId,
        String accountNumber,
        String accountName,
        String currency,
        OffsetDateTime from,
        OffsetDateTime to,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        List<AccountStatementEntryResponse> entries
) {
}

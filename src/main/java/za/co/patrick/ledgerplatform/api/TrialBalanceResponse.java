package za.co.patrick.ledgerplatform.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record TrialBalanceResponse(
        String currency,
        OffsetDateTime generatedAt,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        List<TrialBalanceEntryResponse> entries
) {
}

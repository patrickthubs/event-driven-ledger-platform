package za.co.patrick.ledgerplatform.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateReconciliationRequest(
        @NotNull UUID accountId,
        @NotNull OffsetDateTime from,
        @NotNull OffsetDateTime to,
        @NotNull BigDecimal externalBalance,
        BigDecimal toleranceAmount,
        @Size(max = 120) String externalReference,
        @Size(max = 300) String notes
) {
}

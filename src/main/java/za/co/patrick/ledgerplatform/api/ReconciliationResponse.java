package za.co.patrick.ledgerplatform.api;

import za.co.patrick.ledgerplatform.domain.ReconciliationStatus;
import za.co.patrick.ledgerplatform.domain.ReconciliationResolutionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReconciliationResponse(
        UUID reconciliationId,
        UUID accountId,
        String accountNumber,
        String accountName,
        String currency,
        OffsetDateTime from,
        OffsetDateTime to,
        BigDecimal ledgerBalance,
        BigDecimal externalBalance,
        BigDecimal toleranceAmount,
        BigDecimal differenceAmount,
        ReconciliationStatus status,
        String externalReference,
        String notes,
        String assignedTo,
        String reviewNotes,
        ReconciliationResolutionType resolutionType,
        String resolutionNotes,
        String resolvedBy,
        OffsetDateTime resolvedAt,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt
) {
}

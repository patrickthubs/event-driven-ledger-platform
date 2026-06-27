package za.co.patrick.ledgerplatform.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import za.co.patrick.ledgerplatform.domain.EntryDirection;

import java.math.BigDecimal;
import java.util.UUID;

public record JournalEntryLineRequest(
        @NotNull(message = "Account id is required.")
        UUID accountId,
        @NotNull(message = "Direction is required.")
        EntryDirection direction,
        @NotNull(message = "Amount is required.")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero.")
        BigDecimal amount,
        String narrative
) {
}

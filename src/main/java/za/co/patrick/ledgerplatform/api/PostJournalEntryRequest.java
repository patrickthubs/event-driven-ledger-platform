package za.co.patrick.ledgerplatform.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;
import java.util.List;

public record PostJournalEntryRequest(
        @NotBlank(message = "Idempotency key is required.")
        String idempotencyKey,
        String externalReference,
        @NotBlank(message = "Description is required.")
        String description,
        @NotBlank(message = "Currency is required.")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code.")
        String currency,
        @NotNull(message = "Effective timestamp is required.")
        OffsetDateTime effectiveAt,
        @NotEmpty(message = "At least one journal line is required.")
        List<@Valid JournalEntryLineRequest> lines
) {
}

package za.co.patrick.ledgerplatform.api;

import jakarta.validation.constraints.NotBlank;

public record ReverseJournalEntryRequest(
        @NotBlank(message = "Idempotency key is required.")
        String idempotencyKey,
        @NotBlank(message = "Reason is required.")
        String reason
) {
}

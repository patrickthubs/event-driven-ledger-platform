package za.co.patrick.ledgerplatform.application;

import za.co.patrick.ledgerplatform.api.JournalEntryResponse;

public record JournalPostingResult(
        boolean created,
        JournalEntryResponse response
) {
}

package za.co.patrick.ledgerplatform.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LedgerPostingValidatorTest {

    private final LedgerPostingValidator validator = new LedgerPostingValidator();

    @Test
    void shouldAcceptBalancedJournalEntry() {
        LedgerPostingCommand command = new LedgerPostingCommand(
                "txn-001",
                "EXT-001",
                "Initial funding",
                "USD",
                OffsetDateTime.parse("2026-06-26T10:15:30Z"),
                List.of(
                        new LedgerPostingCommand.LedgerPostingLine(UUID.randomUUID(), EntryDirection.DEBIT, new BigDecimal("100.00"), "Cash"),
                        new LedgerPostingCommand.LedgerPostingLine(UUID.randomUUID(), EntryDirection.CREDIT, new BigDecimal("100.00"), "Equity")
                )
        );

        assertDoesNotThrow(() -> validator.validate(command));
    }

    @Test
    void shouldRejectUnbalancedJournalEntry() {
        LedgerPostingCommand command = new LedgerPostingCommand(
                "txn-002",
                "EXT-002",
                "Broken posting",
                "USD",
                OffsetDateTime.parse("2026-06-26T10:15:30Z"),
                List.of(
                        new LedgerPostingCommand.LedgerPostingLine(UUID.randomUUID(), EntryDirection.DEBIT, new BigDecimal("100.00"), "Cash"),
                        new LedgerPostingCommand.LedgerPostingLine(UUID.randomUUID(), EntryDirection.CREDIT, new BigDecimal("90.00"), "Equity")
                )
        );

        assertThrows(IllegalArgumentException.class, () -> validator.validate(command));
    }
}

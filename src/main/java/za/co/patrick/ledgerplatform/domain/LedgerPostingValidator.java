package za.co.patrick.ledgerplatform.domain;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class LedgerPostingValidator {

    public void validate(LedgerPostingCommand command) {
        if (command.lines() == null || command.lines().size() < 2) {
            throw new IllegalArgumentException("A journal entry must contain at least two lines.");
        }
        if (command.currency() == null || command.currency().isBlank()) {
            throw new IllegalArgumentException("Currency is required.");
        }
        BigDecimal totalDebits = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCredits = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (LedgerPostingCommand.LedgerPostingLine line : command.lines()) {
            if (line.accountId() == null) {
                throw new IllegalArgumentException("Each journal line requires an account id.");
            }
            if (line.direction() == null) {
                throw new IllegalArgumentException("Each journal line requires a direction.");
            }
            if (line.amount() == null || line.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Each journal line amount must be greater than zero.");
            }
            BigDecimal normalizedAmount = line.amount().setScale(2, RoundingMode.HALF_UP);
            if (line.direction() == EntryDirection.DEBIT) {
                totalDebits = totalDebits.add(normalizedAmount);
            } else {
                totalCredits = totalCredits.add(normalizedAmount);
            }
        }
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalArgumentException("Journal entry debits must equal credits.");
        }
    }
}

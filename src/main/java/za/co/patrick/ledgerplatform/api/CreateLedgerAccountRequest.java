package za.co.patrick.ledgerplatform.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import za.co.patrick.ledgerplatform.domain.AccountType;

public record CreateLedgerAccountRequest(
        @NotBlank(message = "Account number is required.")
        String accountNumber,
        @NotBlank(message = "Account name is required.")
        String accountName,
        @NotNull(message = "Account type is required.")
        AccountType accountType,
        @NotBlank(message = "Currency is required.")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code.")
        String currency,
        boolean allowNegativeBalance
) {
}

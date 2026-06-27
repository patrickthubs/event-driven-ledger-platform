package za.co.patrick.ledgerplatform.infrastructure;

import java.math.BigDecimal;

public interface AccountPostingTotalsView {
    BigDecimal getTotalDebits();

    BigDecimal getTotalCredits();
}

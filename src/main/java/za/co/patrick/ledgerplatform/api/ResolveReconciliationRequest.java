package za.co.patrick.ledgerplatform.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import za.co.patrick.ledgerplatform.domain.ReconciliationResolutionType;

public record ResolveReconciliationRequest(
        @NotNull ReconciliationResolutionType resolutionType,
        @NotBlank @Size(max = 120) String resolvedBy,
        @NotBlank @Size(max = 300) String resolutionNotes
) {
}

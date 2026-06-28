package za.co.patrick.ledgerplatform.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignReconciliationRequest(
        @NotBlank @Size(max = 120) String assignedTo,
        @Size(max = 300) String reviewNotes
) {
}

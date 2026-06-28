package za.co.patrick.ledgerplatform.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import za.co.patrick.ledgerplatform.application.ReconciliationService;
import za.co.patrick.ledgerplatform.domain.ReconciliationStatus;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reconciliations")
class ReconciliationController {

    private final ReconciliationService reconciliationService;

    ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping
    ResponseEntity<ReconciliationResponse> createReconciliation(@Valid @RequestBody CreateReconciliationRequest request) {
        ReconciliationResponse response = reconciliationService.createReconciliation(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{reconciliationId}")
                .buildAndExpand(response.reconciliationId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    List<ReconciliationResponse> listReconciliations(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) ReconciliationStatus status
    ) {
        return reconciliationService.listReconciliations(accountId, status);
    }

    @GetMapping("/{reconciliationId}")
    ReconciliationResponse getReconciliation(@PathVariable UUID reconciliationId) {
        return reconciliationService.getReconciliation(reconciliationId);
    }

    @PostMapping("/{reconciliationId}/assign")
    ReconciliationResponse assignForReview(
            @PathVariable UUID reconciliationId,
            @Valid @RequestBody AssignReconciliationRequest request
    ) {
        return reconciliationService.assignForReview(reconciliationId, request);
    }

    @PostMapping("/{reconciliationId}/resolve")
    ReconciliationResponse resolve(
            @PathVariable UUID reconciliationId,
            @Valid @RequestBody ResolveReconciliationRequest request
    ) {
        return reconciliationService.resolve(reconciliationId, request);
    }
}

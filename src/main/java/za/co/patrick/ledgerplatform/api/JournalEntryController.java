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
import za.co.patrick.ledgerplatform.application.JournalPostingResult;
import za.co.patrick.ledgerplatform.application.LedgerService;
import za.co.patrick.ledgerplatform.application.OutboxProcessingService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
class JournalEntryController {

    private final LedgerService ledgerService;
    private final OutboxProcessingService outboxProcessingService;

    JournalEntryController(LedgerService ledgerService, OutboxProcessingService outboxProcessingService) {
        this.ledgerService = ledgerService;
        this.outboxProcessingService = outboxProcessingService;
    }

    @PostMapping("/journal-entries")
    ResponseEntity<JournalEntryResponse> postJournalEntry(@Valid @RequestBody PostJournalEntryRequest request) {
        JournalPostingResult result = ledgerService.postJournalEntry(request);
        if (!result.created()) {
            return ResponseEntity.ok(result.response());
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{journalEntryId}")
                .buildAndExpand(result.response().journalEntryId())
                .toUri();
        return ResponseEntity.created(location).body(result.response());
    }

    @GetMapping("/journal-entries/{journalEntryId}")
    JournalEntryResponse getJournalEntry(@PathVariable UUID journalEntryId) {
        return ledgerService.getJournalEntry(journalEntryId);
    }

    @PostMapping("/journal-entries/{journalEntryId}/reversals")
    ResponseEntity<JournalEntryResponse> reverseJournalEntry(
            @PathVariable UUID journalEntryId,
            @Valid @RequestBody ReverseJournalEntryRequest request
    ) {
        JournalPostingResult result = ledgerService.reverseJournalEntry(journalEntryId, request);
        if (!result.created()) {
            return ResponseEntity.ok(result.response());
        }
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/journal-entries/{journalEntryId}")
                .buildAndExpand(result.response().journalEntryId())
                .toUri();
        return ResponseEntity.created(location).body(result.response());
    }

    @GetMapping("/journal-entries")
    List<JournalEntryResponse> listJournalEntries(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) String externalReference
    ) {
        return ledgerService.listJournalEntries(accountId, externalReference);
    }

    @GetMapping("/outbox-events")
    List<OutboxEventResponse> listOutboxEvents(@RequestParam(required = false) Boolean published) {
        return ledgerService.listOutboxEvents(published);
    }

    @PostMapping("/outbox-events/{eventId}/publish")
    OutboxEventResponse markOutboxEventPublished(@PathVariable UUID eventId) {
        return outboxProcessingService.publishEvent(eventId);
    }

    @PostMapping("/outbox-events/publish-batch")
    OutboxPublishBatchResponse publishOutboxBatch(@RequestParam(defaultValue = "10") int batchSize) {
        return outboxProcessingService.publishPendingBatch(batchSize);
    }
}

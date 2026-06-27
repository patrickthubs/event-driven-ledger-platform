package za.co.patrick.ledgerplatform.api;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import za.co.patrick.ledgerplatform.application.LedgerService;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
class LedgerAccountController {

    private final LedgerService ledgerService;

    LedgerAccountController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping
    ResponseEntity<LedgerAccountResponse> createAccount(@Valid @RequestBody CreateLedgerAccountRequest request) {
        LedgerAccountResponse response = ledgerService.createAccount(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{accountId}")
                .buildAndExpand(response.accountId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    List<LedgerAccountResponse> listAccounts() {
        return ledgerService.listAccounts();
    }

    @GetMapping("/{accountId}")
    LedgerAccountResponse getAccount(@PathVariable UUID accountId) {
        return ledgerService.getAccount(accountId);
    }

    @GetMapping("/number/{accountNumber}")
    LedgerAccountResponse getAccountByNumber(@PathVariable String accountNumber) {
        return ledgerService.getAccountByNumber(accountNumber);
    }

    @GetMapping("/{accountId}/journal-entries")
    List<JournalEntryResponse> listAccountJournalEntries(@PathVariable UUID accountId) {
        return ledgerService.listJournalEntriesForAccount(accountId);
    }

    @GetMapping("/{accountId}/statement")
    AccountStatementResponse getAccountStatement(
            @PathVariable UUID accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return ledgerService.getAccountStatement(accountId, from, to);
    }
}

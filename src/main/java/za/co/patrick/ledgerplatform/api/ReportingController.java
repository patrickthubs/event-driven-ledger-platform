package za.co.patrick.ledgerplatform.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.patrick.ledgerplatform.application.LedgerService;

@RestController
@RequestMapping("/api/v1/reports")
class ReportingController {

    private final LedgerService ledgerService;

    ReportingController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/trial-balance")
    TrialBalanceResponse getTrialBalance(@RequestParam(required = false) String currency) {
        return ledgerService.getTrialBalance(currency);
    }
}

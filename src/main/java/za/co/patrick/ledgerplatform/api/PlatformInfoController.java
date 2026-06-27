package za.co.patrick.ledgerplatform.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform-info")
class PlatformInfoController {

    @GetMapping
    PlatformInfoResponse getPlatformInfo() {
        return new PlatformInfoResponse(
                "Event-Driven Ledger Platform",
                "MVP_IN_PROGRESS",
                List.of(
                        "account-management",
                        "double-entry-posting",
                        "ledger-posting",
                        "journal-reversals",
                        "event-publication",
                        "outbox-publish-ack",
                        "trial-balance-reporting",
                        "account-statements",
                        "reconciliation",
                        "audit-trail"
                )
        );
    }
}

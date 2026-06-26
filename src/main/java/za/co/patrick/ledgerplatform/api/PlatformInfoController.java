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
                "BOOTSTRAPPED",
                List.of(
                        "ledger-posting",
                        "event-publication",
                        "reconciliation",
                        "audit-trail"
                )
        );
    }
}


package za.co.patrick.ledgerplatform.api;

import java.util.List;

public record PlatformInfoResponse(
        String name,
        String status,
        List<String> capabilities
) {
}


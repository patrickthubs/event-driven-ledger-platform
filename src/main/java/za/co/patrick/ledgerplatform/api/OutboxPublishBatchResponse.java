package za.co.patrick.ledgerplatform.api;

import java.util.List;
import java.util.UUID;

public record OutboxPublishBatchResponse(
        int requestedCount,
        int publishedCount,
        int failedCount,
        List<UUID> publishedEventIds
) {
}

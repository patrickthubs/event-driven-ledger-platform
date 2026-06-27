package za.co.patrick.ledgerplatform.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OutboxEventResponse(
        UUID eventId,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String payload,
        OffsetDateTime createdAt,
        OffsetDateTime publishedAt
) {
}

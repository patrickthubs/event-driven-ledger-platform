package za.co.patrick.ledgerplatform.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OutboxEventResponse(
        UUID eventId,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String destinationTopic,
        String messageKey,
        String payload,
        OffsetDateTime createdAt,
        OffsetDateTime publishedAt,
        OffsetDateTime lastAttemptedAt,
        int publishAttemptCount,
        String lastPublishError,
        Integer publishedPartition,
        Long publishedOffset
) {
}

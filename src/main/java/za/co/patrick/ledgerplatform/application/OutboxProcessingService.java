package za.co.patrick.ledgerplatform.application;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.patrick.ledgerplatform.api.OutboxEventResponse;
import za.co.patrick.ledgerplatform.api.OutboxPublishBatchResponse;
import za.co.patrick.ledgerplatform.config.OutboxProcessorProperties;
import za.co.patrick.ledgerplatform.infrastructure.OutboxEventEntity;
import za.co.patrick.ledgerplatform.infrastructure.OutboxEventEntityRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxProcessingService {

    private final OutboxEventEntityRepository outboxEventRepository;
    private final OutboxPublisher outboxPublisher;
    private final OutboxProcessorProperties properties;

    public OutboxProcessingService(
            OutboxEventEntityRepository outboxEventRepository,
            OutboxPublisher outboxPublisher,
            OutboxProcessorProperties properties
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxPublisher = outboxPublisher;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString = "${app.outbox.processor.fixed-delay-ms:30000}",
            initialDelayString = "${app.outbox.processor.initial-delay-ms:5000}"
    )
    @Transactional
    public void publishScheduledBatch() {
        if (!properties.enabled()) {
            return;
        }
        publishPendingBatch(properties.batchSize());
    }

    @Transactional
    public OutboxPublishBatchResponse publishPendingBatch(int requestedCount) {
        int batchSize = requestedCount <= 0 ? properties.batchSize() : requestedCount;
        List<OutboxEventEntity> events = outboxEventRepository
                .findUnpublishedForUpdate(PageRequest.of(0, batchSize))
                .getContent();

        int publishedCount = 0;
        int failedCount = 0;
        List<UUID> publishedEventIds = new ArrayList<>();
        OffsetDateTime attemptedAt = OffsetDateTime.now();

        for (OutboxEventEntity event : events) {
            try {
                BrokerPublishResult publishResult = outboxPublisher.publish(event);
                event.recordPublishSuccess(attemptedAt, publishResult.partition(), publishResult.offset());
                publishedCount++;
                publishedEventIds.add(event.getId());
            } catch (RuntimeException exception) {
                event.recordPublishFailure(attemptedAt, exception.getMessage());
                failedCount++;
            }
        }
        outboxEventRepository.saveAll(events);

        return new OutboxPublishBatchResponse(batchSize, publishedCount, failedCount, publishedEventIds);
    }

    @Transactional
    public OutboxEventResponse publishEvent(UUID eventId) {
        OutboxEventEntity event = outboxEventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Outbox event %s was not found.".formatted(eventId)));
        if (event.getPublishedAt() != null) {
            return toResponse(event);
        }

        OffsetDateTime attemptedAt = OffsetDateTime.now();
        try {
            BrokerPublishResult publishResult = outboxPublisher.publish(event);
            event.recordPublishSuccess(attemptedAt, publishResult.partition(), publishResult.offset());
            return toResponse(outboxEventRepository.save(event));
        } catch (RuntimeException exception) {
            event.recordPublishFailure(attemptedAt, exception.getMessage());
            outboxEventRepository.save(event);
            throw exception;
        }
    }

    private OutboxEventResponse toResponse(OutboxEventEntity entity) {
        return new OutboxEventResponse(
                entity.getId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getDestinationTopic(),
                entity.getMessageKey(),
                entity.getPayload(),
                entity.getCreatedAt(),
                entity.getPublishedAt(),
                entity.getLastAttemptedAt(),
                entity.getPublishAttemptCount(),
                entity.getLastPublishError(),
                entity.getPublishedPartition(),
                entity.getPublishedOffset()
        );
    }
}

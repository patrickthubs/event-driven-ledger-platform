package za.co.patrick.ledgerplatform.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEventEntity {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "destination_topic", nullable = false, length = 120)
    private String destinationTopic;

    @Column(name = "message_key", nullable = false, length = 120)
    private String messageKey;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "last_attempted_at")
    private OffsetDateTime lastAttemptedAt;

    @Column(name = "publish_attempt_count", nullable = false)
    private int publishAttemptCount;

    @Column(name = "last_publish_error", length = 500)
    private String lastPublishError;

    @Column(name = "published_partition")
    private Integer publishedPartition;

    @Column(name = "published_offset")
    private Long publishedOffset;

    protected OutboxEventEntity() {
    }

    public OutboxEventEntity(
            UUID id,
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
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.destinationTopic = destinationTopic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
        this.lastAttemptedAt = lastAttemptedAt;
        this.publishAttemptCount = publishAttemptCount;
        this.lastPublishError = lastPublishError;
        this.publishedPartition = publishedPartition;
        this.publishedOffset = publishedOffset;
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDestinationTopic() {
        return destinationTopic;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getPayload() {
        return payload;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public OffsetDateTime getLastAttemptedAt() {
        return lastAttemptedAt;
    }

    public int getPublishAttemptCount() {
        return publishAttemptCount;
    }

    public String getLastPublishError() {
        return lastPublishError;
    }

    public Integer getPublishedPartition() {
        return publishedPartition;
    }

    public Long getPublishedOffset() {
        return publishedOffset;
    }

    public void markPublished(OffsetDateTime publishedAt) {
        markPublished(publishedAt, null, null);
    }

    public void markPublished(OffsetDateTime publishedAt, Integer partition, Long offset) {
        this.publishedAt = publishedAt;
        this.lastAttemptedAt = publishedAt;
        this.publishAttemptCount++;
        this.lastPublishError = null;
        this.publishedPartition = partition;
        this.publishedOffset = offset;
    }

    public void recordPublishSuccess(OffsetDateTime attemptedAt, Integer partition, Long offset) {
        markPublished(attemptedAt, partition, offset);
    }

    public void recordPublishFailure(OffsetDateTime attemptedAt, String error) {
        this.lastAttemptedAt = attemptedAt;
        this.publishAttemptCount++;
        this.lastPublishError = error == null || error.isBlank() ? "Unknown publish failure." : error;
    }
}

package za.co.patrick.ledgerplatform.infrastructure;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import za.co.patrick.ledgerplatform.application.BrokerPublishResult;
import za.co.patrick.ledgerplatform.application.OutboxPublisher;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "app.outbox.publisher", name = "type", havingValue = "kafka", matchIfMissing = true)
public class KafkaOutboxPublisher implements OutboxPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaOutboxPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public BrokerPublishResult publish(OutboxEventEntity event) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    event.getDestinationTopic(),
                    event.getMessageKey(),
                    event.getPayload()
            );
            record.headers().add(new RecordHeader("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8)));
            record.headers().add(new RecordHeader("aggregateType", event.getAggregateType().getBytes(StandardCharsets.UTF_8)));
            record.headers().add(new RecordHeader("aggregateId", event.getAggregateId().toString().getBytes(StandardCharsets.UTF_8)));

            SendResult<String, String> sendResult = kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
            return new BrokerPublishResult(
                    sendResult.getRecordMetadata().topic(),
                    sendResult.getRecordMetadata().partition(),
                    sendResult.getRecordMetadata().offset()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to publish outbox event %s to Kafka.".formatted(event.getId()), exception);
        }
    }
}

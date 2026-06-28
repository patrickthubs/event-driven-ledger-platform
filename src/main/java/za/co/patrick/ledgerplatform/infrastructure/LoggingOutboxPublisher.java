package za.co.patrick.ledgerplatform.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import za.co.patrick.ledgerplatform.application.BrokerPublishResult;
import za.co.patrick.ledgerplatform.application.OutboxPublisher;

@Component
@ConditionalOnProperty(prefix = "app.outbox.publisher", name = "type", havingValue = "logging")
public class LoggingOutboxPublisher implements OutboxPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingOutboxPublisher.class);

    @Override
    public BrokerPublishResult publish(OutboxEventEntity event) {
        LOGGER.info(
                "Publishing outbox event id={}, aggregateType={}, eventType={}, topic={}, messageKey={}",
                event.getId(),
                event.getAggregateType(),
                event.getEventType(),
                event.getDestinationTopic(),
                event.getMessageKey()
        );
        return new BrokerPublishResult(event.getDestinationTopic(), null, null);
    }
}

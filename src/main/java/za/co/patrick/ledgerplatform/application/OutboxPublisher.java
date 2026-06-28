package za.co.patrick.ledgerplatform.application;

import za.co.patrick.ledgerplatform.infrastructure.OutboxEventEntity;

public interface OutboxPublisher {

    BrokerPublishResult publish(OutboxEventEntity event);
}

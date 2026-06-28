package za.co.patrick.ledgerplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.outbox.kafka")
public record OutboxKafkaProperties(
        String topicName,
        int partitions,
        short replicationFactor
) {
}

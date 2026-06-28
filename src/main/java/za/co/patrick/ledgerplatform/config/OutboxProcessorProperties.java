package za.co.patrick.ledgerplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.outbox.processor")
public record OutboxProcessorProperties(
        boolean enabled,
        int batchSize,
        long fixedDelayMs,
        long initialDelayMs
) {
}

package za.co.patrick.ledgerplatform.application;

public record BrokerPublishResult(
        String topic,
        Integer partition,
        Long offset
) {
}

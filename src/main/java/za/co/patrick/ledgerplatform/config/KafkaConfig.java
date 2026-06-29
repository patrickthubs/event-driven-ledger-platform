package za.co.patrick.ledgerplatform.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Configuration
@ConditionalOnProperty(prefix = "app.outbox.publisher", name = "type", havingValue = "kafka", matchIfMissing = true)
class KafkaConfig {

    @Bean
    KafkaAdmin kafkaAdmin(Environment environment) {
        return new KafkaAdmin(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                environment.getRequiredProperty("spring.kafka.bootstrap-servers")
        ));
    }

    @Bean
    ProducerFactory<String, String> kafkaProducerFactory(Environment environment) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getRequiredProperty("spring.kafka.bootstrap-servers"));
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.RETRIES_CONFIG, 5);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 60000);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> kafkaProducerFactory) {
        return new KafkaTemplate<>(kafkaProducerFactory);
    }

    @Bean
    NewTopic ledgerJournalEntriesTopic(OutboxKafkaProperties properties) {
        return new NewTopic(properties.topicName(), properties.partitions(), properties.replicationFactor());
    }

    @Bean
    ApplicationRunner kafkaTopicProvisioner(KafkaAdmin kafkaAdmin, OutboxKafkaProperties properties) {
        return arguments -> ensureTopicExists(kafkaAdmin, properties);
    }

    private void ensureTopicExists(KafkaAdmin kafkaAdmin, OutboxKafkaProperties properties)
            throws InterruptedException, ExecutionException, TimeoutException {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            try {
                adminClient.createTopics(java.util.List.of(
                        new NewTopic(properties.topicName(), properties.partitions(), properties.replicationFactor())
                )).all().get(30, TimeUnit.SECONDS);
            } catch (ExecutionException exception) {
                if (!(exception.getCause() instanceof TopicExistsException)) {
                    throw exception;
                }
            }

            adminClient.describeTopics(java.util.List.of(properties.topicName()))
                    .allTopicNames()
                    .get(30, TimeUnit.SECONDS);
        }
    }
}

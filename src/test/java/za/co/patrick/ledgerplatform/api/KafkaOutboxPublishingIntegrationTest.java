package za.co.patrick.ledgerplatform.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class KafkaOutboxPublishingIntegrationTest {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.outbox.publisher.type", () -> "kafka");
        registry.add("app.outbox.processor.enabled", () -> false);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldPublishOutboxEventToKafka() throws Exception {
        String assetLocation = createAccount("""
                {
                  "accountNumber": "9100",
                  "accountName": "Kafka Cash",
                  "accountType": "ASSET",
                  "currency": "USD",
                  "allowNegativeBalance": false
                }
                """);
        String equityLocation = createAccount("""
                {
                  "accountNumber": "9101",
                  "accountName": "Kafka Equity",
                  "accountType": "EQUITY",
                  "currency": "USD",
                  "allowNegativeBalance": false
                }
                """);

        String assetId = assetLocation.substring(assetLocation.lastIndexOf('/') + 1);
        String equityId = equityLocation.substring(equityLocation.lastIndexOf('/') + 1);

        mockMvc.perform(post("/api/v1/journal-entries")
                        .with(httpBasic("operator", "operator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "kafka-funding-001",
                                  "externalReference": "KAFKA-EXT-001",
                                  "description": "Kafka funding",
                                  "currency": "USD",
                                  "effectiveAt": "2026-07-11T10:00:00Z",
                                  "lines": [
                                    {
                                      "accountId": "%s",
                                      "direction": "DEBIT",
                                      "amount": 750.00
                                    },
                                    {
                                      "accountId": "%s",
                                      "direction": "CREDIT",
                                      "amount": 750.00
                                    }
                                  ]
                                }
                                """.formatted(assetId, equityId)))
                .andExpect(status().isCreated());

        String responseBody = mockMvc.perform(get("/api/v1/outbox-events?published=false").with(httpBasic("publisher", "publisher")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode outboxEvents = objectMapper.readTree(responseBody);
        UUID eventId = UUID.fromString(outboxEvents.get(0).get("eventId").asText());

        mockMvc.perform(post("/api/v1/outbox-events/%s/publish".formatted(eventId)).with(httpBasic("publisher", "publisher")))
                .andExpect(status().isOk());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "ledger-kafka-test",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()
        ))) {
            consumer.subscribe(java.util.List.of("ledger.journal.entries"));

            ConsumerRecord<String, String> publishedRecord = null;
            for (int attempt = 0; attempt < 10 && publishedRecord == null; attempt++) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    if (record.value().contains("kafka-funding-001")) {
                        publishedRecord = record;
                        break;
                    }
                }
            }

            assertThat(publishedRecord).isNotNull();
            assertThat(publishedRecord.key()).isNotBlank();
            assertThat(publishedRecord.value()).contains("kafka-funding-001");
            assertThat(new String(publishedRecord.headers().lastHeader("eventType").value())).isEqualTo("JOURNAL_ENTRY_POSTED");
        }
    }

    private String createAccount(String requestBody) throws Exception {
        return mockMvc.perform(post("/api/v1/accounts")
                        .with(httpBasic("operator", "operator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
    }
}

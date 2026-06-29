package za.co.patrick.ledgerplatform.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import za.co.patrick.ledgerplatform.api.OutboxEventResponse;
import za.co.patrick.ledgerplatform.api.OutboxPublishBatchResponse;
import za.co.patrick.ledgerplatform.infrastructure.OutboxEventEntity;
import za.co.patrick.ledgerplatform.infrastructure.OutboxEventEntityRepository;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.outbox.publisher.type=logging",
        "app.outbox.processor.enabled=false"
})
@AutoConfigureMockMvc
@Import(OutboxProcessingServiceConcurrencyIntegrationTest.TestConfig.class)
class OutboxProcessingServiceConcurrencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxProcessingService outboxProcessingService;

    @Autowired
    private BlockingOutboxPublisher blockingOutboxPublisher;

    @Autowired
    private OutboxEventEntityRepository outboxEventRepository;

    @Test
    void shouldNotPublishSameOutboxEventTwiceWhenBatchAndManualPublishRace() throws Exception {
        blockingOutboxPublisher.reset();

        String uniqueSuffix = Long.toString(System.nanoTime());
        String cashLocation = createAccount("""
                {
                  "accountNumber": "18%s",
                  "accountName": "Concurrent Cash",
                  "accountType": "ASSET",
                  "currency": "USD",
                  "allowNegativeBalance": false
                }
                """.formatted(uniqueSuffix.substring(uniqueSuffix.length() - 6)));
        String equityLocation = createAccount("""
                {
                  "accountNumber": "38%s",
                  "accountName": "Concurrent Equity",
                  "accountType": "EQUITY",
                  "currency": "USD",
                  "allowNegativeBalance": false
                }
                """.formatted(uniqueSuffix.substring(uniqueSuffix.length() - 6)));

        String cashId = cashLocation.substring(cashLocation.lastIndexOf('/') + 1);
        String equityId = equityLocation.substring(equityLocation.lastIndexOf('/') + 1);
        String idempotencyKey = "concurrent-publish-%s".formatted(uniqueSuffix);

        mockMvc.perform(post("/api/v1/journal-entries")
                        .with(httpBasic("operator", "operator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "%s",
                                  "externalReference": "CONCURRENT-%s",
                                  "description": "Concurrent outbox publish race",
                                  "currency": "USD",
                                  "effectiveAt": "2026-07-16T10:00:00Z",
                                  "lines": [
                                    {
                                      "accountId": "%s",
                                      "direction": "DEBIT",
                                      "amount": 250.00
                                    },
                                    {
                                      "accountId": "%s",
                                      "direction": "CREDIT",
                                      "amount": 250.00
                                    }
                                  ]
                                }
                                """.formatted(idempotencyKey, uniqueSuffix, cashId, equityId)))
                .andExpect(status().isCreated());

        UUID eventId = firstUnpublishedEventId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<OutboxPublishBatchResponse> batchPublish = executor.submit(() -> outboxProcessingService.publishPendingBatch(1));
            blockingOutboxPublisher.awaitFirstPublish();

            Future<OutboxEventResponse> manualPublish = executor.submit(() -> outboxProcessingService.publishEvent(eventId));

            blockingOutboxPublisher.releaseFirstPublish();

            OutboxPublishBatchResponse batchResponse = batchPublish.get(10, TimeUnit.SECONDS);
            OutboxEventResponse manualResponse = manualPublish.get(10, TimeUnit.SECONDS);
            OutboxEventEntity persistedEvent = outboxEventRepository.findById(eventId).orElseThrow();

            assertThat(batchResponse.publishedCount()).isEqualTo(1);
            assertThat(batchResponse.failedCount()).isZero();
            assertThat(manualResponse.eventId()).isEqualTo(eventId);
            assertThat(manualResponse.publishedAt()).isNotNull();
            assertThat(blockingOutboxPublisher.publishInvocationCount()).isEqualTo(1);
            assertThat(persistedEvent.getPublishAttemptCount()).isEqualTo(1);
            assertThat(persistedEvent.getPublishedAt()).isNotNull();
        } finally {
            executor.shutdownNow();
        }
    }

    private UUID firstUnpublishedEventId() throws Exception {
        String responseBody = mockMvc.perform(get("/api/v1/outbox-events?published=false")
                        .with(httpBasic("publisher", "publisher")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode events = objectMapper.readTree(responseBody);
        return UUID.fromString(events.get(0).get("eventId").asText());
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

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        BlockingOutboxPublisher blockingOutboxPublisher() {
            return new BlockingOutboxPublisher();
        }
    }

    static final class BlockingOutboxPublisher implements OutboxPublisher {

        private final AtomicInteger publishInvocationCount = new AtomicInteger();
        private volatile CountDownLatch firstPublishStarted = new CountDownLatch(1);
        private volatile CountDownLatch releaseFirstPublish = new CountDownLatch(1);

        @Override
        public BrokerPublishResult publish(OutboxEventEntity event) {
            int invocation = publishInvocationCount.incrementAndGet();
            if (invocation == 1) {
                firstPublishStarted.countDown();
                try {
                    if (!releaseFirstPublish.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to release first outbox publish.");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting to release first outbox publish.", exception);
                }
            }
            return new BrokerPublishResult(event.getDestinationTopic(), 0, 42L);
        }

        void awaitFirstPublish() throws InterruptedException, TimeoutException {
            if (!firstPublishStarted.await(10, TimeUnit.SECONDS)) {
                throw new TimeoutException("Timed out waiting for first outbox publish to start.");
            }
        }

        void releaseFirstPublish() {
            releaseFirstPublish.countDown();
        }

        int publishInvocationCount() {
            return publishInvocationCount.get();
        }

        void reset() {
            publishInvocationCount.set(0);
            firstPublishStarted = new CountDownLatch(1);
            releaseFirstPublish = new CountDownLatch(1);
        }
    }
}

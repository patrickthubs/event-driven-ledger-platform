package za.co.patrick.ledgerplatform.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PostgresLedgerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("event_driven_ledger_test")
            .withUsername("ledger")
            .withPassword("ledger");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("app.outbox.processor.enabled", () -> false);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldPostJournalEntryAgainstPostgres() throws Exception {
        String assetLocation = createAccount("""
                {
                  "accountNumber": "9000",
                  "accountName": "Postgres Cash",
                  "accountType": "ASSET",
                  "currency": "USD",
                  "allowNegativeBalance": false
                }
                """);
        String equityLocation = createAccount("""
                {
                  "accountNumber": "9001",
                  "accountName": "Postgres Equity",
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
                                  "idempotencyKey": "postgres-funding-001",
                                  "externalReference": "PG-EXT-001",
                                  "description": "Postgres funding",
                                  "currency": "USD",
                                  "effectiveAt": "2026-07-10T10:00:00Z",
                                  "lines": [
                                    {
                                      "accountId": "%s",
                                      "direction": "DEBIT",
                                      "amount": 500.00
                                    },
                                    {
                                      "accountId": "%s",
                                      "direction": "CREDIT",
                                      "amount": 500.00
                                    }
                                  ]
                                }
                                """.formatted(assetId, equityId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/journal-entries/")))
                .andExpect(jsonPath("$.status").value("POSTED"));

        mockMvc.perform(get("/api/v1/reports/trial-balance?currency=USD").with(httpBasic("auditor", "auditor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDebits").value(500.00))
                .andExpect(jsonPath("$.totalCredits").value(500.00));
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

package za.co.patrick.ledgerplatform.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LedgerPlatformFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateAccountsPostJournalEntryAndExposeOutboxEvent() throws Exception {
        String cashLocation = createAccount("""
                {
                  "accountNumber": "1000",
                  "accountName": "Cash",
                  "accountType": "ASSET",
                  "currency": "ZAR",
                  "allowNegativeBalance": false
                }
                """);

        String equityLocation = createAccount("""
                {
                  "accountNumber": "3000",
                  "accountName": "Owner Equity",
                  "accountType": "EQUITY",
                  "currency": "ZAR",
                  "allowNegativeBalance": false
                }
                """);

        String cashId = cashLocation.substring(cashLocation.lastIndexOf('/') + 1);
        String equityId = equityLocation.substring(equityLocation.lastIndexOf('/') + 1);

        String requestBody = """
                {
                  "idempotencyKey": "funding-001",
                  "externalReference": "EXT-FUND-001",
                  "description": "Initial capital injection",
                  "currency": "ZAR",
                  "effectiveAt": "2026-07-01T08:30:00Z",
                  "lines": [
                    {
                      "accountId": "%s",
                      "direction": "DEBIT",
                      "amount": 1000.00,
                      "narrative": "Cash received"
                    },
                    {
                      "accountId": "%s",
                      "direction": "CREDIT",
                      "amount": 1000.00,
                      "narrative": "Capital contribution"
                    }
                  ]
                }
                """.formatted(cashId, equityId);

        String journalLocation = mockMvc.perform(post("/api/v1/journal-entries")
                        .with(httpBasic("operator", "operator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/journal-entries/")))
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.totalDebit").value(1000.00))
                .andExpect(jsonPath("$.totalCredit").value(1000.00))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(post("/api/v1/journal-entries")
                        .with(httpBasic("operator", "operator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idempotencyKey").value("funding-001"));

        mockMvc.perform(get(cashLocation).with(httpBasic("auditor", "auditor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(1000.00))
                .andExpect(jsonPath("$.accountType").value("ASSET"));

        mockMvc.perform(get(equityLocation).with(httpBasic("auditor", "auditor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(1000.00))
                .andExpect(jsonPath("$.accountType").value("EQUITY"));

        mockMvc.perform(get(journalLocation).with(httpBasic("auditor", "auditor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].accountNumber").exists())
                .andExpect(jsonPath("$.lines.length()").value(2));

        mockMvc.perform(get("/api/v1/outbox-events").with(httpBasic("publisher", "publisher")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].eventType").value("JOURNAL_ENTRY_POSTED"))
                .andExpect(jsonPath("$[0].destinationTopic").value("ledger.journal.entries"))
                .andExpect(jsonPath("$[0].payload").value(containsString("funding-001")));

        mockMvc.perform(get("/api/v1/outbox-events?published=false").with(httpBasic("publisher", "publisher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("JOURNAL_ENTRY_POSTED"));

        mockMvc.perform(get("/api/v1/accounts/number/1000").with(httpBasic("auditor", "auditor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountName").value("Cash"));

        mockMvc.perform(get("/api/v1/accounts/%s/journal-entries".formatted(cashId)).with(httpBasic("auditor", "auditor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idempotencyKey").value("funding-001"));

        mockMvc.perform(get("/api/v1/journal-entries?externalReference=EXT-FUND-001").with(httpBasic("auditor", "auditor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].externalReference").value("EXT-FUND-001"));

        mockMvc.perform(get("/api/v1/reports/trial-balance?currency=ZAR").with(httpBasic("auditor", "auditor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("ZAR"))
                .andExpect(jsonPath("$.totalDebits").value(1000.00))
                .andExpect(jsonPath("$.totalCredits").value(1000.00))
                .andExpect(jsonPath("$.entries[0].accountNumber").value("1000"))
                .andExpect(jsonPath("$.entries[0].debitBalance").value(1000.00))
                .andExpect(jsonPath("$.entries[1].accountNumber").value("3000"))
                .andExpect(jsonPath("$.entries[1].creditBalance").value(1000.00));

        mockMvc.perform(get("/api/v1/accounts/%s/statement".formatted(cashId))
                        .with(httpBasic("auditor", "auditor"))
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-07-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1000"))
                .andExpect(jsonPath("$.openingBalance").value(0.00))
                .andExpect(jsonPath("$.closingBalance").value(1000.00))
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].journalEntryId").exists())
                .andExpect(jsonPath("$.entries[0].runningBalance").value(1000.00));

        String unpublishedEventId = mockMvc.perform(get("/api/v1/outbox-events?published=false").with(httpBasic("publisher", "publisher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].publishedAt").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\\\"eventId\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/api/v1/outbox-events/%s/publish".formatted(unpublishedEventId))
                        .with(httpBasic("publisher", "publisher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(unpublishedEventId))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty())
                .andExpect(jsonPath("$.messageKey").isNotEmpty());

        mockMvc.perform(get("/api/v1/outbox-events?published=true").with(httpBasic("publisher", "publisher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(unpublishedEventId))
                .andExpect(jsonPath("$[0].publishedAt").isNotEmpty());

        String reconciliationLocation = mockMvc.perform(post("/api/v1/reconciliations")
                        .with(httpBasic("reconciler", "reconciler"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "%s",
                                  "from": "2026-07-01T00:00:00Z",
                                  "to": "2026-07-31T23:59:59Z",
                                  "externalBalance": 970.00,
                                  "toleranceAmount": 10.00,
                                  "externalReference": "BANK-STMT-001",
                                  "notes": "Month-end cash confirmation"
                                }
                                """.formatted(cashId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("MISMATCHED"))
                .andExpect(jsonPath("$.differenceAmount").value(30.00))
                .andExpect(jsonPath("$.toleranceAmount").value(10.00))
                .andExpect(jsonPath("$.accountNumber").value("1000"))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(post("%s/assign".formatted(reconciliationLocation))
                        .with(httpBasic("reconciler", "reconciler"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedTo": "ops.user",
                                  "reviewNotes": "Investigate bank timing difference"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.assignedTo").value("ops.user"));

        mockMvc.perform(post("%s/resolve".formatted(reconciliationLocation))
                        .with(httpBasic("reconciler", "reconciler"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resolutionType": "EXTERNAL_CORRECTION",
                                  "resolvedBy": "reconciler",
                                  "resolutionNotes": "External statement corrected after cutoff review"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolutionType").value("EXTERNAL_CORRECTION"))
                .andExpect(jsonPath("$.resolvedBy").value("reconciler"));

        mockMvc.perform(get("/api/v1/reconciliations").with(httpBasic("reconciler", "reconciler")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].externalReference").value("BANK-STMT-001"));

        mockMvc.perform(get("/api/v1/reconciliations?status=RESOLVED").with(httpBasic("reconciler", "reconciler")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RESOLVED"));

        mockMvc.perform(get("/api/v1/admin/users").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"));

        mockMvc.perform(post("/api/v1/journal-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnbalancedJournalEntry() throws Exception {
        String cashLocation = createAccount("""
                {
                  "accountNumber": "1100",
                  "accountName": "Operating Cash",
                  "accountType": "ASSET",
                  "currency": "USD",
                  "allowNegativeBalance": false
                }
                """);

        String revenueLocation = createAccount("""
                {
                  "accountNumber": "4000",
                  "accountName": "Revenue",
                  "accountType": "REVENUE",
                  "currency": "USD",
                  "allowNegativeBalance": false
                }
                """);

        String cashId = cashLocation.substring(cashLocation.lastIndexOf('/') + 1);
        String revenueId = revenueLocation.substring(revenueLocation.lastIndexOf('/') + 1);

        String requestBody = """
                {
                  "idempotencyKey": "broken-001",
                  "description": "Broken journal entry",
                  "currency": "USD",
                  "effectiveAt": "2026-07-01T08:30:00Z",
                  "lines": [
                    {
                      "accountId": "%s",
                      "direction": "DEBIT",
                      "amount": 1000.00
                    },
                    {
                      "accountId": "%s",
                      "direction": "CREDIT",
                      "amount": 900.00
                    }
                  ]
                }
                """.formatted(cashId, revenueId);

        mockMvc.perform(post("/api/v1/journal-entries")
                        .with(httpBasic("operator", "operator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid ledger request"))
                .andExpect(jsonPath("$.detail").value("Journal entry debits must equal credits."));
    }

    @Test
    void shouldReverseJournalEntryOnce() throws Exception {
        String expenseLocation = createAccount("""
                {
                  "accountNumber": "5100",
                  "accountName": "Processing Expense",
                  "accountType": "EXPENSE",
                  "currency": "USD",
                  "allowNegativeBalance": false
                }
                """);
        String cashLocation = createAccount("""
                {
                  "accountNumber": "1200",
                  "accountName": "Settlement Cash",
                  "accountType": "ASSET",
                  "currency": "USD",
                  "allowNegativeBalance": true
                }
                """);

        String expenseId = expenseLocation.substring(expenseLocation.lastIndexOf('/') + 1);
        String cashId = cashLocation.substring(cashLocation.lastIndexOf('/') + 1);

        String postRequest = """
                {
                  "idempotencyKey": "fee-001",
                  "externalReference": "EXT-FEE-001",
                  "description": "Processing fee",
                  "currency": "USD",
                  "effectiveAt": "2026-07-02T08:30:00Z",
                  "lines": [
                    {
                      "accountId": "%s",
                      "direction": "DEBIT",
                      "amount": 25.00,
                      "narrative": "Fee expense"
                    },
                    {
                      "accountId": "%s",
                      "direction": "CREDIT",
                      "amount": 25.00,
                      "narrative": "Cash reduction"
                    }
                  ]
                }
                """.formatted(expenseId, cashId);

        String originalLocation = mockMvc.perform(post("/api/v1/journal-entries")
                        .with(httpBasic("operator", "operator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postRequest))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(post("%s/reversals".formatted(originalLocation))
                        .with(httpBasic("operator", "operator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "fee-001-reversal",
                                  "reason": "Fee waiver"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reversalOfJournalEntryId").exists())
                .andExpect(jsonPath("$.reversalReason").value("Fee waiver"))
                .andExpect(jsonPath("$.lines[0].direction").exists());

        mockMvc.perform(post("%s/reversals".formatted(originalLocation))
                        .with(httpBasic("operator", "operator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "fee-001-reversal-second",
                                  "reason": "Duplicate reversal attempt"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("already been reversed")));
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

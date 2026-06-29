# Event-Driven Ledger Platform

Production-style Java 26 and Spring Boot 4 backend for an immutable, double-entry ledger with idempotent posting, Kafka-backed outbox publishing, reconciliation operations, trial balance reporting, account statements, and persisted role-based security.

This project exists to show more than CRUD. It demonstrates the sort of backend design recruiters expect to see in payment, banking, treasury, and financial platform work: correctness, auditability, event-driven integration boundaries, and careful API design.

## What The MVP Does

- creates ledger accounts with explicit account type, currency, and negative-balance rules
- posts balanced journal entries with idempotency protection
- calculates derived balances from immutable journal lines
- supports journal reversal flows
- persists outbox events for downstream publishing
- publishes outbox events to Kafka with topic metadata, message keys, and retry tracking
- exposes trial balance reporting
- exposes account statements with opening balance, running balance, and closing balance
- records reconciliation runs with tolerance, review assignment, and resolution workflows
- protects sensitive endpoints with persisted users, BCrypt passwords, and role-based HTTP Basic authentication
- includes PostgreSQL-backed integration validation with Testcontainers when Docker is available
- includes Kafka-backed integration validation with Testcontainers when Docker is available
- documents the API with Swagger/OpenAPI

## Architecture

- `api`
  REST controllers, request validation, response DTOs, and Problem Details error handling
- `application`
  orchestration of posting, reversals, statements, reporting, and outbox workflows
- `domain`
  account types, balance-side rules, posting validation, and journal semantics
- `infrastructure`
  JPA entities, repositories, and Flyway-backed schema persistence
- `config`
  OpenAPI and JSON configuration

The design intentionally keeps controllers thin and pushes business rules into the application and domain layers.

## Tech Stack

- Java 26
- Spring Boot 4.1
- Spring Data JPA
- Spring Security
- Spring for Apache Kafka
- PostgreSQL
- Flyway
- springdoc OpenAPI / Swagger UI
- JUnit 5
- Testcontainers
- Docker Compose
- H2 test profile for fast local verification

## Core API Surface

- `GET /api/v1/platform-info`
- `POST /api/v1/accounts`
- `GET /api/v1/accounts`
- `GET /api/v1/accounts/{accountId}`
- `GET /api/v1/accounts/number/{accountNumber}`
- `GET /api/v1/accounts/{accountId}/journal-entries`
- `GET /api/v1/accounts/{accountId}/statement?from=...&to=...`
- `POST /api/v1/journal-entries`
- `GET /api/v1/journal-entries`
- `GET /api/v1/journal-entries/{journalEntryId}`
- `POST /api/v1/journal-entries/{journalEntryId}/reversals`
- `GET /api/v1/outbox-events`
- `POST /api/v1/outbox-events/{eventId}/publish`
- `POST /api/v1/outbox-events/publish-batch`
- `GET /api/v1/reports/trial-balance`
- `POST /api/v1/reconciliations`
- `GET /api/v1/reconciliations`
- `GET /api/v1/reconciliations/{reconciliationId}`
- `POST /api/v1/reconciliations/{reconciliationId}/assign`
- `POST /api/v1/reconciliations/{reconciliationId}/resolve`
- `GET /api/v1/admin/users`

## Example Flows

### Create accounts

```json
POST /api/v1/accounts
{
  "accountNumber": "1000",
  "accountName": "Cash",
  "accountType": "ASSET",
  "currency": "ZAR",
  "allowNegativeBalance": false
}
```

### Post a journal entry

```json
POST /api/v1/journal-entries
{
  "idempotencyKey": "funding-001",
  "externalReference": "EXT-FUND-001",
  "description": "Initial capital injection",
  "currency": "ZAR",
  "effectiveAt": "2026-07-01T08:30:00Z",
  "lines": [
    {
      "accountId": "cash-account-id",
      "direction": "DEBIT",
      "amount": 1000.00,
      "narrative": "Cash received"
    },
    {
      "accountId": "equity-account-id",
      "direction": "CREDIT",
      "amount": 1000.00,
      "narrative": "Capital contribution"
    }
  ]
}
```

### Reverse a journal entry

```json
POST /api/v1/journal-entries/{journalEntryId}/reversals
{
  "idempotencyKey": "funding-001-reversal",
  "reason": "Correction"
}
```

### Get a trial balance

```text
GET /api/v1/reports/trial-balance?currency=ZAR
```

### Get an account statement

```text
GET /api/v1/accounts/{accountId}/statement?from=2026-07-01T00:00:00Z&to=2026-07-31T23:59:59Z
```

### Create a reconciliation run

```json
POST /api/v1/reconciliations
{
  "accountId": "cash-account-id",
  "from": "2026-07-01T00:00:00Z",
  "to": "2026-07-31T23:59:59Z",
  "externalBalance": 970.00,
  "toleranceAmount": 10.00,
  "externalReference": "BANK-STMT-001",
  "notes": "Month-end cash confirmation"
}
```

### Resolve a reconciliation exception

```json
POST /api/v1/reconciliations/{reconciliationId}/resolve
{
  "resolutionType": "EXTERNAL_CORRECTION",
  "resolvedBy": "reconciler",
  "resolutionNotes": "External statement corrected after cutoff review"
}
```

## Local Run

1. Start PostgreSQL and Kafka.

```bash
docker compose up -d
```

2. Start the application.

```bash
mvn spring-boot:run
```

Use a Java 26 runtime for local runs. If `mvn` works but `java -jar` fails with `UnsupportedClassVersionError`, your default `java` on `PATH` is likely pointing at an older JDK.

3. Open Swagger UI.

```text
http://localhost:8080/swagger-ui.html
```

## Configuration

Default local settings live in `src/main/resources/application.yml`.

Override these when needed:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`

Default seeded local users:

- `admin` / `admin`
- `auditor` / `auditor`
- `operator` / `operator`
- `publisher` / `publisher`
- `reconciler` / `reconciler`

## Testing

Run the required checks:

```bash
mvn test
mvn clean verify
```

The default tests use H2 for quick feedback while keeping Flyway migrations active. PostgreSQL-backed and Kafka-backed Testcontainers tests are also included and will run when Docker is available.

## End-To-End Smoke Test

This is a good first run for someone seeing the project for the first time. It proves:

- the app starts against PostgreSQL
- secured APIs work with seeded users
- journal posting creates an outbox event
- the outbox event is published to Kafka
- the broker really stores the ledger event

### 1. Start dependencies

```bash
docker compose up -d
docker compose ps
```

You should see both `postgres` and `kafka` running.

### 2. Start the application

```bash
mvn spring-boot:run
```

### 3. Check the public platform endpoint

```bash
curl http://localhost:8080/api/v1/platform-info
```

### 4. Create two accounts as the operator user

```bash
curl -u operator:operator -X POST http://localhost:8080/api/v1/accounts ^
  -H "Content-Type: application/json" ^
  -d "{\"accountNumber\":\"1000\",\"accountName\":\"Cash\",\"accountType\":\"ASSET\",\"currency\":\"USD\",\"allowNegativeBalance\":false}"
```

```bash
curl -u operator:operator -X POST http://localhost:8080/api/v1/accounts ^
  -H "Content-Type: application/json" ^
  -d "{\"accountNumber\":\"3000\",\"accountName\":\"Owner Equity\",\"accountType\":\"EQUITY\",\"currency\":\"USD\",\"allowNegativeBalance\":false}"
```

Copy the returned `accountId` values.

### 5. Post a balanced journal entry

```bash
curl -u operator:operator -X POST http://localhost:8080/api/v1/journal-entries ^
  -H "Content-Type: application/json" ^
  -d "{\"idempotencyKey\":\"manual-e2e-001\",\"externalReference\":\"MANUAL-E2E-001\",\"description\":\"Initial funding\",\"currency\":\"USD\",\"effectiveAt\":\"2026-07-15T10:00:00Z\",\"lines\":[{\"accountId\":\"<cash-account-id>\",\"direction\":\"DEBIT\",\"amount\":250.00,\"narrative\":\"Cash funded\"},{\"accountId\":\"<equity-account-id>\",\"direction\":\"CREDIT\",\"amount\":250.00,\"narrative\":\"Equity funded\"}]}"
```

### 6. Confirm the unpublished outbox event exists

```bash
curl -u publisher:publisher http://localhost:8080/api/v1/outbox-events?published=false
```

Find the event whose payload contains `manual-e2e-001`, then copy its `eventId`.

### 7. Publish the outbox event

```bash
curl -u publisher:publisher -X POST http://localhost:8080/api/v1/outbox-events/<event-id>/publish
```

The response should include:

- `destinationTopic` = `ledger.journal.entries`
- a non-null `publishedPartition`
- a non-null `publishedOffset`

### 8. Verify the message in Kafka

Use the broker's internal listener from inside the container:

```bash
docker exec event-driven-ledger-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:19092 \
  --topic ledger.journal.entries \
  --from-beginning \
  --timeout-ms 10000
```

You should see a JSON event containing the same journal entry and idempotency key, for example:

```json
{
  "journalEntryId": "36f164f8-2958-4917-98c1-9e64ce43bf1c",
  "idempotencyKey": "manual-e2e-001",
  "externalReference": "MANUAL-E2E-001",
  "currency": "USD",
  "totalDebit": 250.00,
  "totalCredit": 250.00,
  "lineCount": 2,
  "reversalOfJournalEntryId": null,
  "reversalReason": null
}
```

### 9. Optional deeper checks

- `curl -u auditor:auditor http://localhost:8080/api/v1/reports/trial-balance?currency=USD`
- `curl -u auditor:auditor http://localhost:8080/api/v1/accounts/<cash-account-id>/statement?from=2026-07-01T00:00:00Z&to=2026-07-31T23:59:59Z`
- `curl -u reconciler:reconciler -X POST http://localhost:8080/api/v1/reconciliations ...`
- `curl -u admin:admin http://localhost:8080/api/v1/admin/users`

## Roadmap

- as-of-date reporting beyond current balance snapshots
- holds or reservations
- multi-tenant ledger boundaries
- reconciliation line-level matching against external statements

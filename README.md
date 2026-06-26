# Event-Driven Ledger Platform

Portfolio-grade Java 26 and Spring Boot 4 backend for building an immutable, event-driven financial ledger.

This repository is the next step after loan projections in the GitHub portfolio roadmap. It is meant to showcase the kind of backend engineering that matters in financial systems: auditability, idempotency, double-entry design, durable event publication, and clean service boundaries.

## Why This Project Exists

Many backend portfolios stop at CRUD. This project is intended to demonstrate stronger system design skills through:

- immutable ledger thinking instead of mutable balances
- event-driven architecture for downstream consumers
- financial correctness and auditability
- clean modular boundaries across `api`, `application`, `domain`, `infrastructure`, and `config`
- PostgreSQL schema management with Flyway
- production-style testing and documentation

## First Commit Scope

This initial commit establishes the platform foundation:

- Spring Boot 4.1 project baseline
- Java 26 build configuration
- PostgreSQL and Flyway integration
- Swagger/OpenAPI setup
- Docker Compose for local database startup
- a lightweight discovery endpoint for the platform
- test profile and integration coverage

## Planned Product Direction

The eventual MVP for this repository will include:

- accounts and ledger books
- double-entry journal posting
- idempotent transaction ingestion
- posting rules and validations
- outbox-backed domain event publication
- balance snapshots and reconciliation workflows
- reversals, holds, and audit trails

## Architecture Direction

- `api`
  thin REST controllers and API contracts
- `application`
  orchestration for posting, querying, and publishing ledger events
- `domain`
  journal, account, entry, and posting rules
- `infrastructure`
  JPA persistence, outbox storage, and broker integration adapters
- `config`
  OpenAPI and environment configuration

## Tech Stack

- Java 26
- Spring Boot 4.1
- Spring Data JPA
- PostgreSQL
- Flyway
- springdoc OpenAPI / Swagger UI
- JUnit 5
- Mockito
- Testcontainers
- Docker Compose

## Local Run

1. Start PostgreSQL:

```bash
docker compose up -d
```

2. Start the application:

```bash
mvn spring-boot:run
```

3. Open Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## Configuration

Default local settings are in `src/main/resources/application.yml`.

Override these environment variables if needed:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

## API

Initial bootstrap endpoint:

- `GET /api/v1/platform-info`

Example response:

```json
{
  "name": "Event-Driven Ledger Platform",
  "status": "BOOTSTRAPPED",
  "capabilities": [
    "ledger-posting",
    "event-publication",
    "reconciliation",
    "audit-trail"
  ]
}
```

## Testing

Run the required checks with:

```bash
mvn clean verify
mvn test
```

The default test profile uses H2 in PostgreSQL compatibility mode for fast local feedback. A PostgreSQL-backed migration validation test can be added as the schema becomes more substantial.

## Recruiter-Friendly Highlights

- financial-systems domain instead of generic CRUD
- modern Java 26 and Spring Boot 4 stack
- API-first project setup with Swagger
- clear path toward event-driven architecture and outbox patterns
- intentional repo structure for incremental growth through the year


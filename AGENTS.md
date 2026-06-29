# AGENTS.md

## Project Overrides
This repository follows the shared `NEW_PROJECTS/AGENTS.md` guidance and keeps only repo-specific rules here.

## Runtime Rules
- Keep Docker-backed verification in the standard close-out path for this repo because Kafka and PostgreSQL behavior matter to correctness here.

## Spring Security Rules
- Do not add a custom `AuthenticationProvider` when the existing persistent `UserDetailsService` is enough.
- If authentication changes are needed later, verify startup logs stay free of Spring Security warnings before merging.

## Kafka / Outbox Rules
- The `ledger.journal.entries` topic must be explicitly verified at startup before first publish.
- Kafka integration tests should wait for configured topic availability rather than relying on asynchronous topic creation timing.
- Outbox publish paths must preserve the current duplicate-prevention behavior based on transactional boundaries and pessimistic locking.

## Verification Expectations
- Run `mvn test`.
- Run `mvn clean verify`.
- Confirm no Spring Security startup warnings appear in application or test startup logs.

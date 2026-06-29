# ADR-001: Runtime Hardening and Test Reliability

Date: 2026-06-29
Status: Accepted

## Context
The project needed to run cleanly on a Windows Docker Desktop machine with Java 26, Spring Boot 4.1, Kafka, PostgreSQL, and Testcontainers. We hit three recurring issues:
- Spring Security emitted a startup warning when a custom `AuthenticationProvider` duplicated the existing `UserDetailsService` flow.
- Kafka integration tests could fail because topic metadata was not ready when publish logic executed.
- Docker Desktop was available locally, but older Testcontainers wiring was unreliable for Java-based verification on this machine.

## Decision
- Use Spring Security's default authentication manager wiring from the existing `UserDetailsService` and remove the redundant custom `AuthenticationProvider`.
- Explicitly provision and verify the Kafka topic with `AdminClient` during startup, and make Kafka integration tests wait for topic readiness.
- Standardize this repository on current Testcontainers 2.x module coordinates for Kafka and PostgreSQL integration testing.
- Preserve pessimistic locking and transactional boundaries in the outbox publish flow to prevent duplicate broker publication under concurrent execution.

## Consequences
What becomes easier:
- Local startup logs stay clean of avoidable Spring Security warnings.
- Full-suite Kafka tests are deterministic on this machine.
- Docker-backed verification is more representative of production behavior.

What becomes harder:
- Startup now performs a small amount of explicit Kafka admin work.
- Integration tests depend more clearly on Docker Desktop availability.

What risks remain:
- Java 26 still exposes warnings from the Maven/tooling stack itself, which are outside this repository's Spring application code.
- Future changes to Kafka image versions or Testcontainers behavior should still be validated with real local runs.

## Alternatives Considered
- Keep the custom `AuthenticationProvider` and ignore the warning.
- Rely on `NewTopic` bean creation timing alone.
- Leave older Testcontainers coordinates in place and document the flakiness.

Those options were rejected because they preserve noise or instability instead of removing the underlying cause.

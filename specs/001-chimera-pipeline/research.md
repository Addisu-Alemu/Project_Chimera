# Research: Project Chimera — Autonomous Influencer Network

**Feature**: 001-chimera-pipeline
**Date**: 2026-03-11
**Phase**: 0 — Technical Research

---

## Decision 1: Java 21 Virtual Threads in Spring Boot 3

**Decision**: Enable virtual threads via `spring.threads.virtual.enabled=true` in each
service's `application.properties`.

**Rationale**: Spring Boot 3.2+ natively supports Project Loom virtual threads. This single
property switches the embedded Tomcat thread pool and `@Async` executors to virtual threads,
enabling 1,000+ concurrent pipeline cycles with minimal memory overhead (virtual threads are
cheap, ~1KB stack vs ~1MB for platform threads). No code changes required — existing blocking
JDBC and HTTP calls work correctly as virtual threads park instead of block.

**Caveats**:
- Spring MVC (servlet-based) is the correct choice here — not WebFlux. WebFlux uses reactive
  programming (Mono/Flux), which conflicts with virtual thread semantics. Spring MVC + virtual
  threads is the recommended path for Java 21 + Spring Boot 3.
- `synchronized` blocks that wrap blocking I/O will pin virtual threads to carrier threads;
  use `ReentrantLock` instead where long-held locks are needed.
- Tested with: Spring Boot 3.2+, Tomcat 10.1+.

**Alternatives considered**:
- Spring WebFlux (reactive): Rejected — higher complexity, incompatible with standard Spring
  Data JPA, and unnecessary given virtual thread support.
- Manual `ExecutorService.newVirtualThreadPerTaskExecutor()`: Rejected — Spring's built-in
  property is cleaner and integrates with `@Async`, scheduling, and request handling.

---

## Decision 2: Redis 7 TrendReport Cache (24-hour TTL)

**Decision**: Use Spring Cache abstraction with Lettuce client (Spring Boot default) and a
`RedisCacheManager` configured with a `RedisCacheConfiguration` that sets default TTL to
24 hours. Cache key pattern: `trend-report::{agentId}::{reportId}`.

**Rationale**: Redis 7 TTL enforcement guarantees TrendReport staleness (FR-003) at the
infrastructure level — no application-level staleness check can accidentally be skipped.
Spring Cache (`@Cacheable`, `@CacheEvict`) integrates cleanly with Spring Boot 3. Lettuce
is the default Redis client in Spring Boot (no extra dependency).

**Implementation notes**:
- `spring.cache.type=redis` in `application.properties`
- Define `RedisCacheManager` bean with 24h default TTL
- Annotate `TrendReportCacheService.getReport()` with `@Cacheable(value="trend-report", key="#agentId + ':' + #reportId")`
- Annotate save method with `@CachePut`
- On 404 (cache miss + no DB record), caller must trigger new PERCEIVE cycle

**Alternatives considered**:
- Caffeine (in-memory): Rejected — does not survive service restarts; does not support
  distributed access across multiple service replicas.
- Manual Redis template: Rejected — more boilerplate, loses Spring Cache abstraction benefits.

---

## Decision 3: Append-Only Transaction Records (PostgreSQL)

**Decision**: No `UPDATE` or `DELETE` SQL statements are permitted on the `transactions`
table. Status transitions append a new row with the updated `status` and `actor` fields.
Current state = latest row ordered by `created_at` for a given logical transaction.

**Rationale**: FR-011 requires full audit trail and prohibits deletion. Append-only is the
simplest pattern that satisfies immutability (constitution Principle V) and traceability
(Principle VI) simultaneously. Spring Data JPA enforces this by:
- Declaring only `save()` operations (no `update*` methods on `TransactionRepository`)
- Annotating the entity with `@Immutable` (Hibernate) to prevent dirty-checking updates
- No `@Modifying @Query("UPDATE ...")` anywhere in the codebase

**Alternatives considered**:
- State machine with single-row update: Rejected — loses the full state history required
  for audit and human review.
- Event sourcing with a separate events table: Considered — provides richer history but
  adds significant complexity for an initial implementation. Deferred to a future amendment.

---

## Decision 4: Cross-Service DTO Strategy (Maven Multi-Module)

**Decision**: Each service defines its own local Java Record representations of the DTOs it
consumes. DTOs are serialized to/from JSON over HTTP. There is **no shared DTO library**.

**Rationale**: The constitution (Principle II) states each service owns its stage. A shared
DTO library creates coupling: a change to `TrendReport` forces recompilation and redeployment
of all four services simultaneously, defeating independent deployability. JSON serialization
provides natural loose coupling — consuming services declare only the fields they need.
The `contracts/` directory in this spec is the source of truth for field names and types;
services must not deviate from it.

**Practical rule**: If service A consumes service B's response, service A defines a local
record (e.g., `TrendReportDto`) matching the fields it uses. Jackson handles deserialization.

**Alternatives considered**:
- Shared `chimera-dto` Maven module: Rejected — tight build coupling, violates Single
  Responsibility, increases blast radius of schema changes.
- OpenAPI code generation: Deferred — valid for v2, but adds tooling overhead for MVP.

---

## Decision 5: Integration Testing with Testcontainers

**Decision**: Use Testcontainers (`testcontainers-junit-jupiter`) with
`@Testcontainers` + `@Container` at the test class level. Spin up
`PostgreSQLContainer<?>` (Postgres 16) and `GenericContainer<?>` (Redis 7 via `redis:7-alpine`
image) per test class. Spring Boot's `@DynamicPropertySource` wires the container ports into
the application context.

**Rationale**: Testcontainers provides real infrastructure in CI without a pre-provisioned
database, matching the GitHub Actions integration test pipeline. Tests are hermetic and
reproducible. Spring Boot 3.1+ has built-in Testcontainers support via `@ServiceConnection`
for common containers, further reducing boilerplate.

**Alternatives considered**:
- H2 in-memory: Rejected — does not emulate PostgreSQL dialect fully; TTL behaviour for
  Redis cannot be tested.
- Mocking repositories: For unit tests only — integration tests require real infrastructure.

---

## Decision 6: Inter-Service Communication Pattern

**Decision**: Synchronous HTTP using Spring WebClient (non-blocking, virtual-thread-friendly)
for service-to-service calls within the pipeline.

**Rationale**: The user has not specified a message broker. HTTP is the simplest pattern
consistent with the Spring Boot 3 stack. Virtual threads make blocking HTTP calls efficient
at scale — WebClient with virtual threads handles 1,000+ concurrent pipeline cycles without
thread exhaustion. Each service calls its downstream over HTTP; retry logic is implemented
in the caller (ACT retries publish up to 3×).

**Future evolution**: A message broker (Kafka or RabbitMQ) would decouple services and
enable backpressure. This is deferred to a future architectural phase per the constitution's
amendment procedure.

**Alternatives considered**:
- Apache Kafka: Rejected for MVP — adds operational complexity (ZooKeeper/KRaft, consumer
  groups, offset management) not warranted at this stage.
- gRPC: Rejected — proto-file schema sharing creates the same coupling problem as a shared
  DTO module.

---

## Decision 7: Docker Compose Orchestration

**Decision**: Single `docker-compose.yml` at repository root. Services declare `depends_on`
with `condition: service_healthy`. PostgreSQL and Redis declare `healthcheck:`. All services
use `eclipse-temurin:21-jre-alpine` as the base image for minimal footprint.

**Service startup order**:
```
postgres (healthy) → redis (healthy) → trend-watcher → content-creator → act-service → learn-service
```

**Port mapping**:
| Service | Internal Port | Host Port |
|---------|--------------|-----------|
| trend-watcher | 8081 | 8081 |
| content-creator | 8082 | 8082 |
| act-service | 8083 | 8083 |
| learn-service | 8084 | 8084 |
| PostgreSQL | 5432 | 5432 |
| Redis | 6379 | 6379 |

**Environment variables**: All credentials supplied via `.env` file (gitignored). Docker
Compose loads `.env` automatically.

**Alternatives considered**:
- Kubernetes: Deferred — appropriate for production scale, not MVP local development.
- Separate compose files per service: Rejected — makes full-stack startup cumbersome.

---

## Decision 8: GitHub Actions CI/CD

**Decision**:
- `pr-checks.yml`: Trigger on `pull_request`. Steps: checkout → set up Java 21 → build
  all services (`mvn package -DskipTests`) → run unit tests (`mvn test -Dgroups=unit`)
  for each service in parallel matrix.
- `integration-tests.yml`: Trigger on `push` to `main`. Steps: checkout → set up Java 21
  → run integration tests (`mvn verify -Dgroups=integration`) for each service. Testcontainers
  manages infrastructure — no external services needed in the runner.

**Rationale**: Separating unit tests (fast, PR gate) from integration tests (slower, merge
gate) keeps PR feedback loops under 3 minutes while still catching integration regressions
before they reach main.

**Alternatives considered**:
- Run integration tests on every PR: Rejected — Testcontainers startup adds 30–60s per
  service; with 4 services this significantly slows PR feedback.

---

## Resolved Unknowns Summary

| Unknown | Resolution |
|---------|-----------|
| Virtual thread config | `spring.threads.virtual.enabled=true` in each service |
| Redis TTL enforcement | `RedisCacheManager` with 24h TTL + Spring Cache abstraction |
| Append-only transactions | New row per state transition; `@Immutable` on entity |
| DTO sharing strategy | No shared library; JSON over HTTP; contracts/ is the source of truth |
| Integration test infra | Testcontainers (PostgreSQL 16 + Redis 7-alpine) |
| Inter-service comms | Spring WebClient (sync HTTP, virtual-thread-friendly) |
| Docker orchestration | Single compose file; `depends_on` with health checks |
| CI/CD split | Unit tests on PRs; integration tests on merge to main |

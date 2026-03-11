# Implementation Plan: Project Chimera — Autonomous Influencer Network

**Branch**: `001-chimera-pipeline` | **Date**: 2026-03-11 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-chimera-pipeline/spec.md`

## Summary

Build a closed-loop autonomous influencer network as four independent Spring Boot 3
microservices — PERCEIVE (trend-watcher), CREATE (content-creator), ACT (act-service),
LEARN (learn-service) — each owning exactly one pipeline stage. Services communicate
over HTTP using Java Records as immutable DTOs. TrendReports are cached in Redis with a
24-hour TTL; all data is persisted in PostgreSQL 16. The system supports 1,000+ concurrent
agent cycles via Java 21 Virtual Threads and is deployed as Docker containers orchestrated
by Docker Compose, with CI/CD via GitHub Actions.

## Technical Context

**Language/Version**: Java 21 (Virtual Threads via `spring.threads.virtual.enabled=true`)
**Primary Dependencies**: Spring Boot 3.x, Spring Data JPA, Spring Cache, Spring WebClient,
  Lettuce (Redis client), Jackson (JSON), Testcontainers (integration tests)
**Storage**: PostgreSQL 16 (all persistent entities), Redis 7 (TrendReport cache, 24hr TTL)
**Testing**: JUnit 5, Spring Boot Test, Testcontainers (PostgreSQLContainer + GenericContainer
  for Redis), Mockito
**Target Platform**: Linux server — Docker containers based on eclipse-temurin:21-jre-alpine
**Project Type**: 4 standalone Spring Boot 3 microservices, each with its own pom.xml
**Performance Goals**: 1,000+ concurrent AI influencer agent pipeline cycles without
  data loss or cross-agent contamination
**Constraints**:
  - TrendReport data MUST NOT be older than 24 hours (Redis TTL enforced)
  - Transaction records are append-only (no UPDATE statements on Transaction table)
  - All inter-service DTOs are Java Records (immutable, no setters)
  - Human alert delivery channel configurable via environment variable
**Scale/Scope**: 4 microservices, 7 core DTOs, 1,000+ concurrent pipeline cycles

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Gate Question | Status |
|-----------|--------------|--------|
| I. Spec-Driven Development | Was spec.md ratified before this plan? | ✅ PASS — spec.md complete with 3 user stories, 17 FRs, 8 SCs |
| II. Single Responsibility | Does each service own exactly one pipeline stage? | ✅ PASS — trend-watcher=PERCEIVE, content-creator=CREATE, act-service=ACT, learn-service=LEARN. No cross-stage logic. |
| III. No Silent Failures | Is every error path explicitly defined? | ✅ PASS — FR-003 (stale discard logged), FR-004 (safety filter discard logged), FR-009 (retry halt + alert), FR-010 (tx pause), FR-013 (confidence hold + alert), FR-017 (no silent failures) |
| IV. Human-in-the-Loop | Are all three alert thresholds captured? | ✅ PASS — FR-009 (3 retries), FR-010 ($500 tx), FR-013 (confidence <0.6), FR-015 (all alerts include record link) |
| V. Immutability | Are all inter-service DTOs Java Records? | ✅ PASS — TrendReport, ContentBundle, PostResult, FeedbackReport, TrendSignal, HumanAlert all specified as Java Records |
| VI. Traceability | Does every post reference its ContentBundle→TrendReport? | ✅ PASS — FR-007 (ContentBundle carries TrendReport ID), FR-011 (full transaction detail), SC-003 (100% PostResult→ContentBundle→TrendReport traceable) |

**Result: All 6 principles pass. No violations. Proceed to Phase 0.**

## Project Structure

### Documentation (this feature)

```text
specs/001-chimera-pipeline/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── trend-watcher-api.md
│   ├── content-creator-api.md
│   ├── act-service-api.md
│   └── learn-service-api.md
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
trend-watcher/
├── pom.xml
└── src/
    ├── main/java/com/chimera/trendwatcher/
    │   ├── model/        (TrendReport, TrendTopic, SafetyResult — Java Records)
    │   ├── fetcher/      (SocialMediaFetcher interface, TikTokFetcher, InstagramFetcher, XFetcher)
    │   ├── filter/       (ContentSafetyFilter)
    │   ├── cache/        (TrendReportCacheService — Redis, 24hr TTL)
    │   ├── service/      (TrendWatcherService, TrendAggregator)
    │   ├── alert/        (HumanAlertService)
    │   └── TrendWatcherApplication.java
    └── src/test/java/com/chimera/trendwatcher/

content-creator/
├── pom.xml
└── src/
    ├── main/java/com/chimera/contentcreator/
    │   ├── model/        (ContentBundle, GenerationParameters — Java Records)
    │   ├── generator/    (ContentGenerator interface, TemplateContentGenerator)
    │   ├── filter/       (ContentSafetyFilter)
    │   ├── client/       (TrendWatcherClient, LearnServiceClient — Spring WebClient)
    │   ├── service/      (ContentCreatorService)
    │   ├── alert/        (HumanAlertService)
    │   └── ContentCreatorApplication.java
    └── src/test/java/com/chimera/contentcreator/

act-service/
├── pom.xml
└── src/
    ├── main/java/com/chimera/actservice/
    │   ├── model/        (PostResult, Transaction, AudienceInteraction — Java Records/entities)
    │   ├── publisher/    (ContentPublisher interface, TikTokPublisher, InstagramPublisher, XPublisher)
    │   ├── transaction/  (TransactionManager, TransactionRepository — append-only)
    │   ├── interaction/  (InteractionHandler, InteractionQueue)
    │   ├── client/       (ContentCreatorClient, LearnServiceClient — Spring WebClient)
    │   ├── alert/        (HumanAlertService)
    │   ├── service/      (ActService)
    │   └── ActServiceApplication.java
    └── src/test/java/com/chimera/actservice/

learn-service/
├── pom.xml
└── src/
    ├── main/java/com/chimera/learnservice/
    │   ├── model/        (FeedbackReport, TrendSignal, EngagementSignal — Java Records)
    │   ├── analyzer/     (PerformanceAnalyzer, ConfidenceScorer)
    │   ├── connector/    (CreateFeedbackAdapter, PerceiveFeedbackAdapter — Spring WebClient)
    │   ├── alert/        (HumanAlertService)
    │   ├── service/      (LearnService, ReportBuilder)
    │   └── LearnServiceApplication.java
    └── src/test/java/com/chimera/learnservice/

docker-compose.yml            # Full-stack local orchestration
.github/
└── workflows/
    ├── pr-checks.yml         # Compile + unit tests on every PR
    └── integration-tests.yml # Integration tests on merge to main
```

**Structure Decision**: 4 standalone Spring Boot microservices (Option: multi-service).
Each service has its own `pom.xml` with no shared parent across services (each is independently
buildable and deployable). DTOs produced by a service live in that service's `model/` package;
consumers receive them over HTTP (deserialized from JSON into their own local Record definitions
— see contracts/ for field contracts). Build order: trend-watcher → content-creator →
act-service → learn-service.

## Complexity Tracking

> No constitution violations found. Table omitted.

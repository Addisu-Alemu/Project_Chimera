---

description: "Task list for Project Chimera — Autonomous Influencer Network"
---

# Tasks: Project Chimera — Autonomous Influencer Network

**Input**: Design documents from `/specs/001-chimera-pipeline/`
**Prerequisites**: plan.md ✅ spec.md ✅ research.md ✅ data-model.md ✅ contracts/ ✅ quickstart.md ✅

**Tests**: No test tasks generated — not explicitly requested in the feature specification.

**Organization**: Tasks are grouped by user story to enable independent implementation
and testing of each story.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- File paths are relative to repository root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project scaffolding for all four microservices and CI/CD pipelines.

- [ ] T001 Create root docker-compose.yml with postgres:16 and redis:7-alpine services (with healthchecks) and 4 Spring Boot service stubs (build paths, ports 8081–8084, depends_on with service_healthy condition) at /docker-compose.yml
- [ ] T002 [P] Initialize trend-watcher Maven project with Spring Boot 3 parent, spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-data-redis, spring-boot-starter-cache, flyway-core dependencies and TrendWatcherApplication.java in trend-watcher/pom.xml and trend-watcher/src/main/java/com/chimera/trendwatcher/TrendWatcherApplication.java
- [ ] T003 [P] Initialize content-creator Maven project with Spring Boot 3 parent, spring-boot-starter-web, spring-boot-starter-data-redis dependencies and ContentCreatorApplication.java in content-creator/pom.xml and content-creator/src/main/java/com/chimera/contentcreator/ContentCreatorApplication.java
- [ ] T004 [P] Initialize act-service Maven project with Spring Boot 3 parent, spring-boot-starter-web, spring-boot-starter-data-jpa, flyway-core dependencies and ActServiceApplication.java in act-service/pom.xml and act-service/src/main/java/com/chimera/actservice/ActServiceApplication.java
- [ ] T005 [P] Initialize learn-service Maven project with Spring Boot 3 parent, spring-boot-starter-web, spring-boot-starter-data-jpa, flyway-core dependencies and LearnServiceApplication.java in learn-service/pom.xml and learn-service/src/main/java/com/chimera/learnservice/LearnServiceApplication.java
- [ ] T006 [P] Create Dockerfile (FROM eclipse-temurin:21-jre-alpine, COPY target/*.jar app.jar, ENTRYPOINT java -jar /app.jar, HEALTHCHECK via /actuator/health) for all 4 services: trend-watcher/Dockerfile, content-creator/Dockerfile, act-service/Dockerfile, learn-service/Dockerfile
- [ ] T007 [P] Create .github/workflows/pr-checks.yml: trigger on pull_request, matrix over [trend-watcher, content-creator, act-service, learn-service], steps: setup-java 21 → mvn package -DskipTests → mvn test -Dgroups=unit
- [ ] T008 [P] Create .github/workflows/integration-tests.yml: trigger on push to main, matrix over all services, steps: setup-java 21 → mvn verify -Dgroups=integration (Testcontainers manages PostgreSQL 16 + Redis 7)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Database schema, infrastructure configuration, and virtual thread setup that
all user stories depend on. No user story work can begin until this phase is complete.

**⚠️ CRITICAL**: All 4 services must compile and start before Phase 3 begins.

- [ ] T009 Create Flyway migration V1__create_schema.sql for act-service: tables post_results (id UUID PK, agent_id, content_bundle_id, platform, published_at, status, attempt_count, failure_reason, platform_post_id, created_at), transactions (id UUID PK, agent_id, type, amount DECIMAL(12,2), currency CHAR(3), platform, content_bundle_id, status, actor, created_at, approver_id, completed_at), human_alerts (id UUID PK, agent_id, type, triggering_record_id, triggering_record_link, threshold_value, actual_value, issued_at, resolved_at, resolving_operator_id) in act-service/src/main/resources/db/migration/V1__create_schema.sql
- [ ] T010 [P] Create Flyway migration V1__create_schema.sql for learn-service: tables engagement_signals (id UUID PK, agent_id, post_result_id, signal_type, value BIGINT, recorded_at), feedback_reports (id UUID PK, agent_id, content_bundle_id, confidence_score DECIMAL(4,3), likes, shares, comments, views, click_through_rate, review_status, generated_at, dispatched_at), human_alerts (same schema as act-service) in learn-service/src/main/resources/db/migration/V1__create_schema.sql
- [ ] T011 [P] Configure application.properties and application-docker.properties for all 4 services: set spring.threads.virtual.enabled=true, spring.datasource.url/username/password (act-service and learn-service only), spring.data.redis.host/port (trend-watcher and content-creator), ALERT_WEBHOOK_URL placeholder env var (act-service and learn-service) in each service's src/main/resources/
- [ ] T012 Create CacheConfig @Configuration bean in trend-watcher defining RedisCacheManager with RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(24)) and key prefix "trend-report::" in trend-watcher/src/main/java/com/chimera/trendwatcher/config/CacheConfig.java

**Checkpoint**: All 4 services compile (`mvn package -DskipTests`), Flyway migrations run
cleanly against Postgres 16, Redis connection verified. Foundation ready.

---

## Phase 3: User Story 1 — Full Autonomous Content Cycle (Priority: P1) 🎯 MVP

**Goal**: One complete PERCEIVE → CREATE → ACT → LEARN cycle runs end-to-end without
human intervention, producing a traced PostResult and FeedbackReport.

**Independent Test**: Run quickstart.md§"Triggering a Full Pipeline Cycle" — verify
TrendReport → ContentBundle (with trendReportId) → PostResult (with contentBundleId) →
FeedbackReport (with confidenceScore in [0.0, 1.0]) all produced with no errors.

### PERCEIVE: trend-watcher

- [ ] T013 [P] [US1] Create Platform enum (TIKTOK, INSTAGRAM, X) and TrendTopic Java record (name, hashtags, engagementScore, safetyPassed) in trend-watcher/src/main/java/com/chimera/trendwatcher/model/Platform.java and TrendTopic.java
- [ ] T014 [P] [US1] Create TrendReport Java record (id UUID, agentId UUID, fetchedAt Instant, platforms List<Platform>, topics List<TrendTopic>, categoryWeights Map<String,Double>) in trend-watcher/src/main/java/com/chimera/trendwatcher/model/TrendReport.java
- [ ] T015 [US1] Implement ContentSafetyFilter @Component (method: boolean passes(String text); uses keyword blocklist loaded from classpath resource safety-blocklist.txt; logs discards at WARN level with topic name) in trend-watcher/src/main/java/com/chimera/trendwatcher/filter/ContentSafetyFilter.java
- [ ] T016 [US1] Implement SocialMediaFetcher interface (method: List<TrendTopic> fetch(String agentId)) and stub implementations TikTokFetcher, InstagramFetcher, XFetcher (each returns 5 hardcoded TrendTopic objects for local dev; reads API token from env var) in trend-watcher/src/main/java/com/chimera/trendwatcher/fetcher/
- [ ] T017 [US1] Implement TrendAggregator @Service (inject all SocialMediaFetcher beans; run each fetch on a virtual thread via ExecutorService.newVirtualThreadPerTaskExecutor(); pass each topic through ContentSafetyFilter and discard failures; collect passing topics, sort by engagementScore desc, return top 20) in trend-watcher/src/main/java/com/chimera/trendwatcher/service/TrendAggregator.java
- [ ] T018 [US1] Implement TrendReportCacheService @Service (@Cacheable(value="trend-report", key="#agentId+':'+#reportId", sync=true) on getReport(); @CachePut on saveReport(); @CacheEvict on evictReport()) in trend-watcher/src/main/java/com/chimera/trendwatcher/cache/TrendReportCacheService.java
- [ ] T019 [US1] Implement TrendWatcherService @Service (generate UUID reportId; call TrendAggregator.aggregate(); set fetchedAt=Instant.now(); construct TrendReport record; call TrendReportCacheService.saveReport(); on retrieval check fetchedAt within 24h else throw StaleTrendReportException and log at WARN) in trend-watcher/src/main/java/com/chimera/trendwatcher/service/TrendWatcherService.java
- [ ] T020 [US1] Implement TrendReportController @RestController (POST /trend-reports → call TrendWatcherService.generate(agentId) → return 202 with reportId and status PROCESSING; GET /trend-reports/{reportId} → fetch from cache → return 200 with TrendReport JSON or 404 if not found or 410 if stale) in trend-watcher/src/main/java/com/chimera/trendwatcher/controller/TrendReportController.java

### CREATE: content-creator

- [ ] T021 [P] [US1] Create ContentBundle Java record (id UUID, agentId UUID, trendReportId UUID, caption String, hashtags List<String>, videoDescription String, safetyPassedAt Instant, generatedAt Instant) in content-creator/src/main/java/com/chimera/contentcreator/model/ContentBundle.java
- [ ] T022 [P] [US1] Create local TrendReportDto Java record (id, agentId, fetchedAt, platforms, topics as List<TrendTopicDto>, categoryWeights) matching trend-watcher contract fields in content-creator/src/main/java/com/chimera/contentcreator/client/dto/TrendReportDto.java (and nested TrendTopicDto)
- [ ] T023 [US1] Implement TrendWatcherClient @Component (Spring WebClient; baseUrl from env TREND_WATCHER_URL; method: TrendReportDto getReport(UUID reportId) — GET /trend-reports/{reportId}; throw TrendReportNotFoundException on 404, StaleTrendReportException on 410) in content-creator/src/main/java/com/chimera/contentcreator/client/TrendWatcherClient.java
- [ ] T024 [US1] Implement ContentSafetyFilter @Component in content-creator (same interface as trend-watcher: boolean passes(String text); uses own safety-blocklist.txt; logs discards at WARN) in content-creator/src/main/java/com/chimera/contentcreator/filter/ContentSafetyFilter.java
- [ ] T025 [US1] Implement TemplateContentGenerator @Component (generate caption ≤2200 chars from top 3 TrendTopic names; generate 5–15 hashtags from topic hashtags; generate videoDescription ≤500 chars; all fields populated from TrendReportDto) in content-creator/src/main/java/com/chimera/contentcreator/generator/TemplateContentGenerator.java
- [ ] T026 [US1] Implement ContentCreatorService @Service (1. fetch TrendReportDto via TrendWatcherClient; 2. validate fetchedAt within 24h or throw StaleTrendReportException; 3. call TemplateContentGenerator; 4. run ContentSafetyFilter on caption+description; 5. on filter fail: regenerate up to 3×, on 3rd failure log ERROR and throw ContentSafetyException; 6. return ContentBundle with safetyPassedAt=Instant.now()) in content-creator/src/main/java/com/chimera/contentcreator/service/ContentCreatorService.java
- [ ] T027 [US1] Implement ContentBundleController @RestController (POST /content-bundles → call ContentCreatorService.generate(agentId, trendReportId) → return 202 with bundleId; GET /content-bundles/{bundleId} → return 200 with ContentBundle JSON or 404) in content-creator/src/main/java/com/chimera/contentcreator/controller/ContentBundleController.java

### ACT: act-service

- [ ] T028 [P] [US1] Create PostResult @Entity (id UUID @Id, agentId UUID, contentBundleId UUID, platform String, publishedAt Instant nullable, status PostStatus @Enumerated, attemptCount int, failureReason String nullable, platformPostId String nullable, createdAt Instant @CreatedDate) + PostResultRepository extends JpaRepository in act-service/src/main/java/com/chimera/actservice/model/
- [ ] T029 [P] [US1] Create PostStatus enum (SUCCESS, RETRYING, HELD_FOR_HUMAN, FAILED) and local ContentBundleDto Java record (id, agentId, trendReportId, caption, hashtags, videoDescription, safetyPassedAt, generatedAt) in act-service/src/main/java/com/chimera/actservice/model/PostStatus.java and act-service/src/main/java/com/chimera/actservice/client/dto/ContentBundleDto.java
- [ ] T030 [US1] Implement ContentCreatorClient @Component (Spring WebClient; baseUrl from env CONTENT_CREATOR_URL; method: ContentBundleDto getBundle(UUID bundleId) — GET /content-bundles/{bundleId}) in act-service/src/main/java/com/chimera/actservice/client/ContentCreatorClient.java
- [ ] T031 [US1] Implement ContentSpecValidator @Component (assert contentBundle.trendReportId() != null and contentBundle.safetyPassedAt() != null; throw ContentSpecException with field name if invalid; log validation failures at WARN) in act-service/src/main/java/com/chimera/actservice/validator/ContentSpecValidator.java
- [ ] T032 [US1] Implement ContentPublisher interface (method: String publish(ContentBundleDto bundle, Platform platform) returns platformPostId) and stub implementations TikTokPublisher, InstagramPublisher, XPublisher (return UUID as mock platformPostId; log publish attempt at INFO) in act-service/src/main/java/com/chimera/actservice/publisher/
- [ ] T033 [US1] Implement ActService @Service (1. fetch ContentBundleDto; 2. run ContentSpecValidator; 3. attempt publish via platform publisher; 4. on failure: retry up to 3×, incrementing attemptCount, log each retry at WARN; 5. on success: persist PostResult(status=SUCCESS) + forward to LEARN via LearnServiceClient; 6. on 3rd failure: persist PostResult(status=HELD_FOR_HUMAN) — no HumanAlert yet, added in US2) in act-service/src/main/java/com/chimera/actservice/service/ActService.java
- [ ] T034 [US1] Implement PublishController @RestController (POST /publish body={agentId, contentBundleId, platform} → call ActService.publish() → return 202 with postResultId and status; GET /post-results/{postResultId} → return 200 with PostResult JSON or 404) in act-service/src/main/java/com/chimera/actservice/controller/PublishController.java
- [ ] T035 [US1] Implement LearnServiceClient @Component in act-service (Spring WebClient; baseUrl from env LEARN_SERVICE_URL; methods: void sendEngagementSignal(EngagementSignalDto signal), void triggerAnalysis(UUID agentId, UUID postResultId) — POST /engagement-signals, POST /analyze) in act-service/src/main/java/com/chimera/actservice/client/LearnServiceClient.java

### LEARN: learn-service

- [ ] T036 [P] [US1] Create EngagementSignal @Entity (id UUID, agentId UUID, postResultId UUID, signalType SignalType @Enumerated, value long, recordedAt Instant @CreatedDate) + EngagementSignalRepository in learn-service/src/main/java/com/chimera/learnservice/model/
- [ ] T037 [P] [US1] Create FeedbackReport @Entity (id UUID, agentId UUID, contentBundleId UUID, confidenceScore BigDecimal, likes long, shares long, comments long, views long, clickThroughRate BigDecimal, reviewStatus ReviewStatus @Enumerated, generatedAt Instant, dispatchedAt Instant nullable) + FeedbackReportRepository in learn-service/src/main/java/com/chimera/learnservice/model/
- [ ] T038 [P] [US1] Create SignalType enum (LIKE, SHARE, COMMENT, VIEW, CLICK), ReviewStatus enum (AUTO_DISPATCHED, HELD_PENDING_REVIEW, HUMAN_APPROVED), FeedbackReport DTO record, TrendSignal DTO record (id, agentId, sourceFeedbackReportId, categoryWeights Map<String,Double>, issuedAt) in learn-service/src/main/java/com/chimera/learnservice/model/
- [ ] T039 [US1] Implement PerformanceAnalyzer @Service (query EngagementSignalRepository by postResultId; sum values by signalType; compute clickThroughRate = CLICK count / VIEW count; return EngagementSummary record) in learn-service/src/main/java/com/chimera/learnservice/analyzer/PerformanceAnalyzer.java
- [ ] T040 [US1] Implement ConfidenceScorer @Service (input: EngagementSummary; compute weighted score: 0.3*normalized_shares + 0.25*normalized_likes + 0.2*normalized_comments + 0.15*normalized_views + 0.1*ctr; clamp result to [0.0, 1.0]; return double) in learn-service/src/main/java/com/chimera/learnservice/analyzer/ConfidenceScorer.java
- [ ] T041 [US1] Implement ReportBuilder @Service (input: agentId, contentBundleId, EngagementSummary, confidenceScore; build FeedbackReport entity; set reviewStatus=AUTO_DISPATCHED (confidence ≥ 0.6) or HELD_PENDING_REVIEW (confidence < 0.6); persist via FeedbackReportRepository; return saved entity) in learn-service/src/main/java/com/chimera/learnservice/service/ReportBuilder.java
- [ ] T042 [US1] Implement PerceiveFeedbackAdapter @Component (Spring WebClient; baseUrl from env TREND_WATCHER_URL; method: void dispatch(TrendSignal signal) — POST /trend-signals; log dispatch at INFO; log error at ERROR if call fails but do not rethrow) in learn-service/src/main/java/com/chimera/learnservice/connector/PerceiveFeedbackAdapter.java
- [ ] T043 [US1] Implement CreateFeedbackAdapter @Component (Spring WebClient; baseUrl from env CONTENT_CREATOR_URL; method: void dispatch(FeedbackReport report) — POST /feedback-reports with FeedbackReport DTO; only called by LearnService when reviewStatus = AUTO_DISPATCHED) in learn-service/src/main/java/com/chimera/learnservice/connector/CreateFeedbackAdapter.java
- [ ] T044 [US1] Implement LearnService @Service (1. persist EngagementSignal; 2. call PerformanceAnalyzer; 3. call ConfidenceScorer; 4. call ReportBuilder (persists FeedbackReport); 5. build TrendSignal record; 6. always call PerceiveFeedbackAdapter.dispatch(trendSignal); 7. if confidence ≥ 0.6: call CreateFeedbackAdapter.dispatch(feedbackReport) — US2 will add the alert branch) in learn-service/src/main/java/com/chimera/learnservice/service/LearnService.java
- [ ] T045 [US1] Implement LearnController @RestController (POST /analyze body={agentId, postResultId} → call LearnService.analyze() → return 202 with feedbackReportId and reviewStatus; POST /engagement-signals body={agentId,postResultId,signalType,value,recordedAt} → persist signal → return 204) in learn-service/src/main/java/com/chimera/learnservice/controller/LearnController.java

**Checkpoint**: User Story 1 complete. Run quickstart.md full pipeline cycle:
verify TrendReport → ContentBundle (trendReportId present) → PostResult → FeedbackReport.
SC-001 and SC-003 should pass.

---

## Phase 4: User Story 2 — Human-in-the-Loop Alerts (Priority: P2)

**Goal**: All three autonomy thresholds trigger correct human alerts and pipeline pauses.

**Independent Test**: Test each trigger independently using quickstart.md§"Human Alert Test":
- Submit $501 transaction → verify 202 + PENDING_APPROVAL + alert dispatched.
- Force 3 post failures (stub publisher returns error) → verify HELD_FOR_HUMAN status + alert.
- Produce FeedbackReport with confidence 0.45 → verify HELD_PENDING_REVIEW + alert.

### Alert infrastructure (act-service + learn-service)

- [ ] T046 [P] [US2] Create HumanAlert @Entity (id UUID, agentId UUID, type AlertType @Enumerated, triggeringRecordId UUID, triggeringRecordLink String, thresholdValue String, actualValue String, issuedAt Instant @CreatedDate, resolvedAt Instant nullable, resolvingOperatorId String nullable) + HumanAlertRepository in both act-service/src/main/java/com/chimera/actservice/model/ and learn-service/src/main/java/com/chimera/learnservice/model/
- [ ] T047 [P] [US2] Implement HumanAlertService @Service in act-service (1. build HumanAlert entity; 2. persist via HumanAlertRepository; 3. POST JSON payload to ALERT_WEBHOOK_URL via RestTemplate; 4. log at ERROR if webhook call fails but do not rethrow) in act-service/src/main/java/com/chimera/actservice/alert/HumanAlertService.java
- [ ] T048 [P] [US2] Implement HumanAlertService @Service in learn-service (same pattern as act-service) in learn-service/src/main/java/com/chimera/learnservice/alert/HumanAlertService.java

### Transaction threshold (act-service)

- [ ] T049 [P] [US2] Create Transaction @Entity with @Immutable (id UUID, agentId UUID, type TransactionType, amount BigDecimal, currency String, platform String, contentBundleId UUID, status TransactionStatus, actor String, createdAt Instant @CreatedDate, approverId String nullable, completedAt Instant nullable) + TransactionRepository (insert-only: no update/delete methods) and TransactionType/TransactionStatus enums in act-service/src/main/java/com/chimera/actservice/model/
- [ ] T050 [US2] Implement TransactionManager @Service (1. persist new Transaction(status=PENDING_APPROVAL or COMPLETED based on amount); 2. if amount > 500: call HumanAlertService(type=TRANSACTION_THRESHOLD, triggeringRecordId=transactionId, thresholdValue="$500", actualValue="$"+amount); return transaction with status PENDING_APPROVAL; 3. if amount ≤ 500: insert new Transaction(status=COMPLETED); return COMPLETED) in act-service/src/main/java/com/chimera/actservice/transaction/TransactionManager.java
- [ ] T051 [US2] Implement TransactionController @RestController (POST /transactions → TransactionManager.process() → 202 if PENDING_APPROVAL or 201 if COMPLETED with transaction JSON; POST /transactions/{id}/approve body={operatorId, decision} → insert new Transaction row(status=APPROVED/REJECTED, approverId=operatorId, completedAt=now) → return 200 with updated status) in act-service/src/main/java/com/chimera/actservice/controller/TransactionController.java

### Post failure alert (act-service)

- [ ] T052 [US2] Update ActService in act-service: after 3rd publish retry failure, call HumanAlertService(type=POST_FAILURE, triggeringRecordId=contentBundleId, thresholdValue="3 retries", actualValue="3 failures", triggeringRecordLink="/post-results/{postResultId}") before persisting PostResult(status=HELD_FOR_HUMAN) in act-service/src/main/java/com/chimera/actservice/service/ActService.java

### Confidence threshold alert (learn-service)

- [ ] T053 [US2] Update LearnService in learn-service: when FeedbackReport.reviewStatus = HELD_PENDING_REVIEW, call HumanAlertService(type=LOW_CONFIDENCE, triggeringRecordId=feedbackReportId, thresholdValue="0.6", actualValue=String.valueOf(confidenceScore), triggeringRecordLink="/feedback-reports/{feedbackReportId}") and skip CreateFeedbackAdapter.dispatch() in learn-service/src/main/java/com/chimera/learnservice/service/LearnService.java
- [ ] T054 [US2] Add GET /feedback-reports/{feedbackReportId} (return FeedbackReport JSON or 404) and POST /feedback-reports/{id}/approve body={operatorId, decision} (APPROVED → set reviewStatus=HUMAN_APPROVED + call CreateFeedbackAdapter.dispatch(); REJECTED → set reviewStatus=REJECTED; update resolvedAt on HumanAlert) to LearnController in learn-service/src/main/java/com/chimera/learnservice/controller/LearnController.java

**Checkpoint**: User Stories 1 AND 2 independently functional.
SC-004 ($500 threshold), SC-005 (3 retries), SC-006 (confidence < 0.6) should all pass.

---

## Phase 5: User Story 3 — Continuous Performance Improvement (Priority: P3)

**Goal**: After two consecutive cycles, TrendReport category weighting and ContentBundle
generation parameters demonstrably differ between cycle 1 and cycle 2.

**Independent Test**: Run two full cycles with the same agentId. After cycle 2, GET
/trend-reports/{id} and compare categoryWeights to cycle 1 TrendReport; GET
/content-bundles/{id} and verify caption/hashtags differ between cycles, confirming
feedback was applied.

### TrendSignal intake (trend-watcher)

- [ ] T055 [P] [US3] Create TrendSignal local DTO record (id UUID, agentId UUID, sourceFeedbackReportId UUID, categoryWeights Map<String,Double>, issuedAt Instant) and TrendSignalStore @Component (ConcurrentHashMap<UUID agentId, TrendSignal> backed by Redis via @Cacheable; method: void store(TrendSignal), TrendSignal getLatest(UUID agentId)) in trend-watcher/src/main/java/com/chimera/trendwatcher/model/TrendSignal.java and trend-watcher/src/main/java/com/chimera/trendwatcher/cache/TrendSignalStore.java
- [ ] T056 [US3] Add POST /trend-signals endpoint to TrendReportController (accept TrendSignal JSON body; validate agentId and categoryWeights non-empty; call TrendSignalStore.store(); return 204) in trend-watcher/src/main/java/com/chimera/trendwatcher/controller/TrendReportController.java
- [ ] T057 [US3] Update TrendAggregator to load TrendSignal for agentId via TrendSignalStore.getLatest(); if signal present, multiply each TrendTopic.engagementScore by categoryWeights.getOrDefault(topic.category, 1.0) before sorting; re-rank topics after weighting in trend-watcher/src/main/java/com/chimera/trendwatcher/service/TrendAggregator.java

### FeedbackReport intake (content-creator)

- [ ] T058 [P] [US3] Create FeedbackReportDto local record (id UUID, agentId UUID, contentBundleId UUID, confidenceScore double, engagementSummary nested record, reviewStatus String) and GenerationParametersStore @Component (ConcurrentHashMap<UUID agentId, GenerationParameters>; methods: void applyFeedback(FeedbackReportDto), GenerationParameters getParameters(UUID agentId)) in content-creator/src/main/java/com/chimera/contentcreator/client/dto/FeedbackReportDto.java and content-creator/src/main/java/com/chimera/contentcreator/service/GenerationParametersStore.java
- [ ] T059 [US3] Add POST /feedback-reports endpoint (accept FeedbackReportDto; validate reviewStatus = AUTO_DISPATCHED or HUMAN_APPROVED, reject HELD_PENDING_REVIEW with 422; call GenerationParametersStore.applyFeedback(); derive tone and categoryBias from confidenceScore and engagementSummary; return 204) to content-creator/src/main/java/com/chimera/contentcreator/controller/ContentBundleController.java
- [ ] T060 [US3] Update ContentCreatorService to call GenerationParametersStore.getParameters(agentId) before generating ContentBundle; pass GenerationParameters to TemplateContentGenerator so caption tone, hashtag strategy, and category focus reflect feedback in content-creator/src/main/java/com/chimera/contentcreator/service/ContentCreatorService.java

**Checkpoint**: User Stories 1, 2, AND 3 all independently functional.
SC-008 (measurable differences between cycle 1 and cycle 2) should pass.

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Observability, audience interaction handling, and final validation.

- [ ] T061 [P] Add spring-boot-starter-actuator to all 4 service pom.xml files; configure management.endpoints.web.exposure.include=health in each service's application.properties; update docker-compose.yml HEALTHCHECK directives to use curl -f http://localhost:{port}/actuator/health
- [ ] T062 [P] Add SLF4J MDC logging to all 4 service request handlers: on every inbound request, set MDC keys agentId, service (e.g., "trend-watcher"), and operation (e.g., "generate-trend-report"); clear MDC in finally block; log stage entry and exit at INFO across all controllers and services
- [ ] T063 Implement InteractionQueue (ConcurrentLinkedQueue<AudienceInteraction>) and InteractionHandler @Service (drain queue in background virtual thread, log interaction type at INFO) in act-service; add POST /interactions endpoint (body: agentId, postResultId, platform, interactionType, content; enqueue → return 202) in act-service/src/main/java/com/chimera/actservice/interaction/ and act-service/src/main/java/com/chimera/actservice/controller/InteractionController.java
- [ ] T064 [P] Run quickstart.md end-to-end validation: start docker-compose stack, execute every curl command in quickstart.md in sequence, verify expected HTTP status codes and JSON fields (reportId, bundleId with trendReportId present, postResultId, feedbackReportId with confidenceScore); fix any issues found

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Requires Phase 1 complete (services must exist before configuring them)
- **User Story 1 (Phase 3)**: Requires Phase 2 complete — BLOCKS US2 and US3
- **User Story 2 (Phase 4)**: Requires Phase 3 (US2 extends US1's ActService and LearnService)
- **User Story 3 (Phase 5)**: Requires Phase 3 (US3 extends US1's TrendAggregator and ContentCreatorService)
- **Polish (Phase N)**: Requires all user stories complete

### User Story Dependencies

- **US1 (P1)**: Depends on Foundational. No dependency on US2 or US3.
- **US2 (P2)**: Depends on US1 (ActService + LearnService must exist to be updated).
- **US3 (P3)**: Depends on US1 (TrendAggregator + ContentCreatorService must exist to be updated).
  US3 does not depend on US2.

### Within Each User Story (per service)

- Models → Client DTOs → Client → Service → Controller
- All [P]-marked tasks within a service can start simultaneously once the service is initialized
- Services build in order: trend-watcher → content-creator → act-service → learn-service
  (each service's client depends on the upstream service's contract being known)

### Parallel Opportunities

**Phase 1**: T002, T003, T004, T005, T006, T007, T008 all parallel after T001
**Phase 2**: T010, T011 parallel with T009; T012 after T011
**Phase 3 setup per service**:
  - trend-watcher: T013, T014 parallel → T015 → T016 → T017 → T018 → T019 → T020
  - content-creator: T021, T022 parallel → T023 → T024, T025 parallel → T026 → T027
  - act-service: T028, T029 parallel → T030, T031, T032 parallel → T033 → T034, T035 parallel
  - learn-service: T036, T037, T038 parallel → T039, T040 parallel → T041 → T042, T043 parallel → T044 → T045
**Phase 4**: T046, T047, T048, T049 all parallel → T050, T051, T052, T053 parallel → T054
**Phase 5**: T055, T058 parallel → T056, T059 parallel → T057, T060 parallel
**Phase N**: T061, T062, T064 parallel after all stories complete

---

## Parallel Example: User Story 1 — LEARN Service

```bash
# Launch all model/entity tasks for LEARN together (no inter-dependencies):
Task: "Create EngagementSignal @Entity + EngagementSignalRepository (T036)"
Task: "Create FeedbackReport @Entity + FeedbackReportRepository (T037)"
Task: "Create enums and DTO records: SignalType, ReviewStatus, FeedbackReport, TrendSignal (T038)"

# Once all three complete, launch analytics layer in parallel:
Task: "Implement PerformanceAnalyzer (T039)"
Task: "Implement ConfidenceScorer (T040)"

# Once analytics complete:
Task: "Implement ReportBuilder (T041)"

# Implement dispatchers in parallel:
Task: "Implement PerceiveFeedbackAdapter (T042)"
Task: "Implement CreateFeedbackAdapter (T043)"

# Orchestrate:
Task: "Implement LearnService (T044)"
Task: "Implement LearnController (T045)"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T008)
2. Complete Phase 2: Foundational (T009–T012) — CRITICAL
3. Build trend-watcher core (T013–T020)
4. Build content-creator core (T021–T027)
5. Build act-service core (T028–T035)
6. Build learn-service core (T036–T045)
7. **STOP and VALIDATE**: Run quickstart.md full cycle. Confirm SC-001 and SC-003 pass.

### Incremental Delivery

1. Complete Setup + Foundational + US1 → **MVP: full autonomous pipeline cycle**
2. Add US2 → **Human oversight: alerts for all 3 thresholds** → validate SC-004/005/006
3. Add US3 → **Learning: feedback loop active** → validate SC-008
4. Polish → actuator, MDC, interactions, final validation

### Parallel Team Strategy (if multiple developers)

1. All: complete Phase 1 + Phase 2 together
2. Once Foundational done:
   - Dev A: trend-watcher (T013–T020) + content-creator (T021–T027) in sequence
   - Dev B: act-service (T028–T035) — can start stubs before upstream services are ready
   - Dev C: learn-service (T036–T045) — stub client calls initially
3. Integrate all services → run full pipeline cycle
4. Dev A: US2 alert infrastructure + confidence threshold (T046–T048, T053–T054)
5. Dev B: US2 transaction + post-failure threshold (T049–T052)
6. Dev A+B: US3 signal/feedback handling (T055–T060) — different services, full parallel

---

## Notes

- [P] tasks = different files with no incomplete dependencies — safe to parallelize
- [USN] label maps each task to a user story for traceability and independent delivery
- Stub publishers (T032) return mock success — replace with real API calls when credentials available
- Stub fetchers (T016) return hardcoded topics — replace with real API calls when credentials available
- Services start without each other's APIs by using stub responses; wire real URLs via env vars in docker-compose
- Commit after each task or logical group; tag commits with task ID (e.g., `feat(T020): add TrendReportController`)
- Avoid modifying the same file in parallel — check [P] markers carefully before assigning tasks
- Stop at each **Checkpoint** to validate the story independently before moving to the next phase

# Data Model: Project Chimera — Autonomous Influencer Network

**Feature**: 001-chimera-pipeline
**Date**: 2026-03-11

## Overview

All inter-service DTOs are Java Records (immutable, no setters). Persistent entities stored
in PostgreSQL 16 are JPA `@Entity` classes with append-only constraints where required.
TrendReport is cached in Redis 7 with a 24-hour TTL; it is never the source of truth for
persistence — it is a processing artifact. All IDs are UUIDs.

---

## DTOs (Java Records — Inter-Service Contracts)

### TrendReport

Produced by: **PERCEIVE (trend-watcher)**
Consumed by: **CREATE (content-creator)**
Cached in: **Redis 7** (key: `trend-report:{agentId}:{reportId}`, TTL: 24 hours)

| Field | Type | Constraints |
|-------|------|-------------|
| `id` | UUID | Required, globally unique |
| `agentId` | UUID | Required — identifies the influencer agent |
| `fetchedAt` | Instant | Required — MUST be within last 24 hours at time of use |
| `platforms` | List\<Platform\> | Required, non-empty — TIKTOK, INSTAGRAM, X |
| `topics` | List\<TrendTopic\> | Required, non-empty — safety-filtered |
| `categoryWeights` | Map\<String, Double\> | Optional — injected from TrendSignal by LEARN |

#### TrendTopic (nested record)

| Field | Type | Constraints |
|-------|------|-------------|
| `name` | String | Required, non-blank |
| `hashtags` | List\<String\> | Required, min 1 |
| `engagementScore` | Double | Required, ≥ 0.0 |
| `safetyPassed` | Boolean | Required — MUST be true before forwarding |

#### Staleness rule

At CREATE intake: `if (Instant.now().isAfter(fetchedAt.plus(24, HOURS))) → discard + log + retry`

---

### ContentBundle

Produced by: **CREATE (content-creator)**
Consumed by: **ACT (act-service)**

| Field | Type | Constraints |
|-------|------|-------------|
| `id` | UUID | Required, globally unique |
| `agentId` | UUID | Required — must match source TrendReport.agentId |
| `trendReportId` | UUID | Required — traceability reference to source TrendReport |
| `caption` | String | Required, non-blank, max 2,200 chars |
| `hashtags` | List\<String\> | Required, 1–30 items |
| `videoDescription` | String | Required, non-blank, max 500 chars |
| `safetyPassedAt` | Instant | Required — timestamp of safety filter approval |
| `generatedAt` | Instant | Required |

#### Validation rules

- `trendReportId` MUST NOT be null — bundles without it are rejected at ACT intake
- `safetyPassedAt` MUST be present — bundles without it are rejected
- `caption` + `videoDescription` re-checked by ACT's ContentSpecValidator before publishing

---

### PostResult

Produced by: **ACT (act-service)**
Consumed by: **LEARN (learn-service)**

| Field | Type | Constraints |
|-------|------|-------------|
| `id` | UUID | Required, globally unique |
| `agentId` | UUID | Required |
| `contentBundleId` | UUID | Required — traceability reference |
| `platform` | Platform | Required — TIKTOK, INSTAGRAM, X |
| `publishedAt` | Instant | Required if status=SUCCESS; null otherwise |
| `status` | PostStatus | Required — SUCCESS, FAILED, HELD_FOR_HUMAN |
| `attemptCount` | int | Required, 1–3 |
| `failureReason` | String | Required if status≠SUCCESS; null otherwise |
| `platformPostId` | String | Set by platform on success; null otherwise |

#### State transitions

```
RETRYING (attempt 1) → RETRYING (attempt 2) → RETRYING (attempt 3) → HELD_FOR_HUMAN
RETRYING (any)       → SUCCESS
```

---

### FeedbackReport

Produced by: **LEARN (learn-service)**
Consumed by: **CREATE (content-creator)**

| Field | Type | Constraints |
|-------|------|-------------|
| `id` | UUID | Required, globally unique |
| `agentId` | UUID | Required |
| `contentBundleId` | UUID | Required — traceability reference |
| `confidenceScore` | Double | Required, 0.0–1.0 inclusive |
| `engagementSummary` | EngagementSummary | Required |
| `reviewStatus` | ReviewStatus | Required — AUTO_DISPATCHED, HELD_PENDING_REVIEW, HUMAN_APPROVED |
| `generatedAt` | Instant | Required |
| `dispatchedAt` | Instant | Set when dispatched to CREATE; null if held |

#### EngagementSummary (nested record)

| Field | Type | Constraints |
|-------|------|-------------|
| `likes` | long | ≥ 0 |
| `shares` | long | ≥ 0 |
| `comments` | long | ≥ 0 |
| `views` | long | ≥ 0 |
| `clickThroughRate` | Double | 0.0–1.0 |

#### Confidence threshold rule

`if (confidenceScore < 0.6) → status=HELD_PENDING_REVIEW, alert human, do NOT dispatch`

---

### TrendSignal

Produced by: **LEARN (learn-service)**
Consumed by: **PERCEIVE (trend-watcher)**

| Field | Type | Constraints |
|-------|------|-------------|
| `id` | UUID | Required |
| `agentId` | UUID | Required |
| `sourceFeedbackReportId` | UUID | Required |
| `categoryWeights` | Map\<String, Double\> | Required, values sum ≈ 1.0 |
| `issuedAt` | Instant | Required |

Note: TrendSignal is dispatched regardless of FeedbackReport hold status (FR-014).

---

### HumanAlert

Produced by: **any service** when an autonomy threshold is crossed
Consumed by: **human operators** via configured alert channel

| Field | Type | Constraints |
|-------|------|-------------|
| `id` | UUID | Required |
| `agentId` | UUID | Required |
| `type` | AlertType | Required — TRANSACTION_THRESHOLD, POST_FAILURE, LOW_CONFIDENCE |
| `triggeringRecordId` | UUID | Required — ID of Transaction, ContentBundle, or FeedbackReport |
| `triggeringRecordLink` | String | Required — navigable URL to the record |
| `thresholdValue` | String | Required — e.g., "$500", "3 retries", "0.6" |
| `actualValue` | String | Required — e.g., "$750", "3 failures", "0.45" |
| `issuedAt` | Instant | Required |
| `resolvedAt` | Instant | Set when operator acts; null until then |
| `resolvingOperatorId` | String | Set on resolution |

---

## Persistent Entities (PostgreSQL 16)

### transactions table (append-only)

Owned by: **ACT (act-service)**

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `agent_id` | UUID | NOT NULL, indexed |
| `type` | VARCHAR(50) | NOT NULL — SPONSORED_POST, AFFILIATE, WITHDRAWAL |
| `amount` | DECIMAL(12,2) | NOT NULL, > 0 |
| `currency` | CHAR(3) | NOT NULL — ISO 4217 |
| `platform` | VARCHAR(20) | NOT NULL |
| `content_bundle_id` | UUID | NOT NULL, indexed |
| `status` | VARCHAR(30) | NOT NULL — PENDING_APPROVAL, APPROVED, REJECTED, COMPLETED |
| `actor` | VARCHAR(50) | NOT NULL — SYSTEM or operator ID |
| `created_at` | TIMESTAMPTZ | NOT NULL, set once |
| `approver_id` | VARCHAR(100) | Nullable — set on human approval |
| `completed_at` | TIMESTAMPTZ | Nullable |

**Append-only enforcement**: No `UPDATE` or `DELETE` statements are permitted on this table.
Status transitions create new rows; the latest row by `created_at` for a given transaction
chain is the current state.

### post_results table

Owned by: **ACT (act-service)**

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `agent_id` | UUID | NOT NULL, indexed |
| `content_bundle_id` | UUID | NOT NULL, indexed |
| `platform` | VARCHAR(20) | NOT NULL |
| `published_at` | TIMESTAMPTZ | Nullable |
| `status` | VARCHAR(30) | NOT NULL |
| `attempt_count` | SMALLINT | NOT NULL, 1–3 |
| `failure_reason` | TEXT | Nullable |
| `platform_post_id` | VARCHAR(200) | Nullable |
| `created_at` | TIMESTAMPTZ | NOT NULL |

### engagement_signals table

Owned by: **LEARN (learn-service)**

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `agent_id` | UUID | NOT NULL, indexed |
| `post_result_id` | UUID | NOT NULL, indexed |
| `signal_type` | VARCHAR(50) | NOT NULL — LIKE, SHARE, COMMENT, VIEW, CLICK |
| `value` | BIGINT | NOT NULL, ≥ 0 |
| `recorded_at` | TIMESTAMPTZ | NOT NULL |

### feedback_reports table

Owned by: **LEARN (learn-service)**

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `agent_id` | UUID | NOT NULL, indexed |
| `content_bundle_id` | UUID | NOT NULL, indexed |
| `confidence_score` | DECIMAL(4,3) | NOT NULL, 0.000–1.000 |
| `likes` | BIGINT | NOT NULL |
| `shares` | BIGINT | NOT NULL |
| `comments` | BIGINT | NOT NULL |
| `views` | BIGINT | NOT NULL |
| `click_through_rate` | DECIMAL(5,4) | NOT NULL |
| `review_status` | VARCHAR(30) | NOT NULL |
| `generated_at` | TIMESTAMPTZ | NOT NULL |
| `dispatched_at` | TIMESTAMPTZ | Nullable |

### human_alerts table

Owned by: **all services** (shared schema or replicated per service — TBD in implementation)

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `agent_id` | UUID | NOT NULL, indexed |
| `type` | VARCHAR(50) | NOT NULL |
| `triggering_record_id` | UUID | NOT NULL |
| `triggering_record_link` | TEXT | NOT NULL |
| `threshold_value` | VARCHAR(100) | NOT NULL |
| `actual_value` | VARCHAR(100) | NOT NULL |
| `issued_at` | TIMESTAMPTZ | NOT NULL |
| `resolved_at` | TIMESTAMPTZ | Nullable |
| `resolving_operator_id` | VARCHAR(100) | Nullable |

---

## Entity Relationships

```
TrendReport (Redis cache)
  └─▶ ContentBundle.trendReportId
        └─▶ PostResult.contentBundleId
              └─▶ EngagementSignal.postResultId
              └─▶ Transaction.contentBundleId
        └─▶ FeedbackReport.contentBundleId
              └─▶ TrendSignal.sourceFeedbackReportId

HumanAlert.triggeringRecordId → Transaction.id | ContentBundle.id | FeedbackReport.id
```

---

## Enumerations

| Enum | Values |
|------|--------|
| `Platform` | TIKTOK, INSTAGRAM, X |
| `PostStatus` | SUCCESS, RETRYING, HELD_FOR_HUMAN, FAILED |
| `ReviewStatus` | AUTO_DISPATCHED, HELD_PENDING_REVIEW, HUMAN_APPROVED |
| `AlertType` | TRANSACTION_THRESHOLD, POST_FAILURE, LOW_CONFIDENCE |
| `TransactionType` | SPONSORED_POST, AFFILIATE, WITHDRAWAL |
| `TransactionStatus` | PENDING_APPROVAL, APPROVED, REJECTED, COMPLETED |
| `SignalType` | LIKE, SHARE, COMMENT, VIEW, CLICK |

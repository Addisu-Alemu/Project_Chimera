<!--
  SYNC IMPACT REPORT
  ==================
  Version change: UNVERSIONED (blank template) → 1.0.0

  Modified principles: N/A — initial ratification from template

  Added sections:
  - I.   Spec-Driven Development
  - II.  Single Responsibility
  - III. No Silent Failures
  - IV.  Human-in-the-Loop
  - V.   Immutability
  - VI.  Traceability  (template had 5 principle slots; extended to 6 per user input)
  - Technology & Build Standards (new)
  - Service Responsibilities (new)
  - Governance (concrete rules replacing placeholder)

  Removed sections: N/A

  Templates reviewed:
  - .specify/templates/plan-template.md     ✅ "Constitution Check" gate present; gates derive
                                               from this file at plan time — no edits needed
  - .specify/templates/spec-template.md     ✅ User-story + FR + SC structure aligns with
                                               Spec-Driven and Traceability principles — no edits needed
  - .specify/templates/tasks-template.md    ✅ Phase structure supports safety, observability,
                                               and foundational setup — no edits needed
  - .specify/templates/checklist-template.md ✅ Generic placeholder template — no edits needed
  - .specify/templates/agent-file-template.md ⚠ Still contains only generic placeholders;
                                               populate with Chimera stack and commands once
                                               Spring Boot migration is complete
  - .claude/commands/*.md                   ✅ Commands reference constitution by path, not
                                               content — no stale principle references found
  - README.md                               ⚠ Currently a single-line stub; expand with
                                               stack, service map, and quick-start once stable
  - CLAUDE.md                               ⚠ Empty; populate with session-level guidance
                                               that references constitution principles

  Deferred TODOs:
  - None. All placeholders resolved from user input and repo context.

  Architecture note:
  - The tech stack declared here (Spring Boot 3, PostgreSQL, Redis) supersedes the
    "pure Java, no framework" approach used in the current four-service codebase.
    Existing services (trend-watcher, content-creator, act-service, learn-service) were
    built as pure-Java 21 prototypes. Migration to the Spring Boot 3 stack is a
    forward-looking decision governed by this constitution. Until migration is complete,
    new features MUST comply with this constitution; existing code is grandfathered.
-->

# Project Chimera Constitution

## Core Principles

### I. Spec-Driven Development

No line of production code MAY be written before a feature specification (`spec.md`) is
ratified by the lead engineer.

- The spec MUST define user stories, functional requirements, and measurable success criteria
  before any implementation plan or task list is generated.
- Design artifacts (plan, data model, contracts) MUST be completed and reviewed before coding
  begins; code written ahead of a ratified spec MUST be discarded, not merged.
- This principle applies to all four services and to any shared infrastructure changes.
- The workflow enforced by `.claude/commands/speckit.*.md` commands is the canonical
  implementation of this principle.

**Rationale**: Speccing before coding prevents wasted effort, surfaces misunderstandings early,
and ensures every feature is traceable to an agreed-upon requirement.

### II. Single Responsibility

Each service MUST own exactly one stage of the PERCEIVE → CREATE → ACT → LEARN pipeline
and MUST NOT contain business logic belonging to another stage.

- `trend-watcher` (PERCEIVE): MUST produce `TrendReport` — nothing else.
- `content-creator` (CREATE): MUST consume `TrendReport` and produce `ContentBundle` — nothing else.
- `act-service` (ACT): MUST publish `ContentBundle`, handle interactions, and record
  transactions — nothing else.
- `learn-service` (LEARN): MUST ingest engagement signals and dispatch feedback to
  PERCEIVE and CREATE — nothing else.
- Cross-stage logic introduced in a service constitutes a constitution violation and
  MUST be refactored out before the feature is merged.

**Rationale**: Stage isolation enables independent deployment, testing, and replacement of
any pipeline component without cascading changes across the system.

### III. No Silent Failures

Every error path in every service MUST be explicitly defined, handled, and logged.

- Exceptions MUST NOT be swallowed without at minimum a structured log entry at WARN or
  ERROR level that includes: service name, operation, input identifier, and failure reason.
- Retry logic MUST be explicit: maximum attempt count and back-off strategy MUST be declared
  in code (not implicit through framework defaults).
- A service that cannot fulfil its stage obligation after exhausting retries MUST emit an
  alert event (see Principle IV) and enter a defined degraded state — it MUST NOT crash
  silently or return a partial result without flagging it.
- `try { ... } catch (Exception e) { /* ignored */ }` blocks are prohibited and MUST be
  rejected in code review.

**Rationale**: An autonomous system without explicit failure handling is undebuggable and
untrustworthy. Every failure mode must be a first-class design concern.

### IV. Human-in-the-Loop

The system operates autonomously by default. Humans MUST be alerted — and autonomous
action MUST be paused — when any of the following thresholds are crossed:

| Trigger | Threshold | Required Action |
|---------|-----------|-----------------|
| Financial transaction | Amount > $500 | Pause and await explicit approval |
| Post failure (ACT) | ≥ 3 consecutive retries without success | Alert and halt publishing for that ContentBundle |
| Confidence score (LEARN) | Score < 0.6 on a ContentBundle | Flag for human review before next publish cycle |

- Alert delivery mechanism (email, Slack, webhook) MUST be configurable via environment
  variable; a missing alert config MUST prevent service startup, not be silently ignored.
- Humans MUST NOT be alerted for routine operations; alert fatigue defeats this principle.
- Every human alert MUST include a deep link to the triggering record for one-click review.

**Rationale**: Full autonomy is the goal; human oversight is the safety net. The system
must know its own limits and escalate precisely and only when necessary.

### V. Immutability

All data transfer objects crossing service boundaries MUST be Java Records with no mutable
state.

- DTOs MUST be Java `record` types; classes with setters or mutable fields are prohibited
  for inter-service data.
- Records MUST be declared `final` implicitly (Java records are final by definition) and
  MUST NOT use `@Builder` patterns that enable post-construction mutation.
- If a transformation is required between pipeline stages, a new record instance MUST be
  constructed — existing records MUST NOT be modified.
- Collections within records MUST be wrapped with `Collections.unmodifiableList()` or use
  `List.of()` / `Set.of()` / `Map.of()` to prevent external mutation.
- Core inter-service types: `TrendReport`, `ContentBundle`, `PostResult`, `FeedbackReport`.

**Rationale**: Immutable DTOs eliminate a class of concurrency bugs critical in a
virtual-thread-heavy system, make data flow auditable, and simplify serialization.

### VI. Traceability

Every output artifact MUST carry a reference to its upstream source, and every financial
transaction MUST be logged in full detail.

- Every `ContentBundle` produced by CREATE MUST reference the `TrendReport.id` that generated
  it; this `trendReportId` MUST be persisted alongside the bundle.
- Every post published by ACT MUST reference its `ContentBundle.id`; the publish record MUST
  include platform, timestamp, status, and attempt count.
- Every financial transaction MUST be persisted with: transaction ID, type, amount, currency,
  platform, associated post ID, status, timestamp, and actor (human or system).
- No transaction record MAY be deleted; soft deletion with `deletedAt` timestamp is the only
  permitted form of removal.
- Audit queries (e.g., "which trend led to this post?") MUST be answerable from the database
  without application-level joins across more than two service schemas.

**Rationale**: Autonomous publishing and financial operations require a full audit trail for
compliance, debugging, and rollback decision-making.

## Technology & Build Standards

The canonical tech stack for Project Chimera is:

| Concern | Technology | Version |
|---------|-----------|---------|
| Language | Java | 21 (LTS) |
| Concurrency | Project Loom virtual threads | Java 21 built-in |
| Framework | Spring Boot | 3.x (latest stable) |
| Persistence | PostgreSQL | 16 |
| Cache / pub-sub | Redis | 7 |
| Containerisation | Docker Compose | latest stable |
| CI/CD | GitHub Actions | — |

- Each service MUST be packaged as a Docker image and declared in a root-level
  `docker-compose.yml`; services MUST NOT be run bare-metal in production.
- GitHub Actions pipelines MUST include: build, unit tests, integration tests (against
  Testcontainers-managed Postgres + Redis), and constitution-compliance check gate.
- Credentials and environment-specific config MUST be supplied via environment variables or
  Docker secrets; they MUST NOT appear in source code, Dockerfiles, or committed YAML files.
- All services share the same parent Maven POM (`chimera-parent`) for version management;
  individual modules MUST NOT override dependency versions declared in `<dependencyManagement>`.

**Architecture note**: The existing four services were built as pure-Java 21 prototypes
(no framework). This constitution governs forward development; new features and refactors
MUST target the Spring Boot 3 stack declared above.

## Service Responsibilities

The pipeline is a closed feedback loop. Each service's boundary is hard:

```
PERCEIVE (trend-watcher)
  Input : social platform APIs
  Output: TrendReport → CREATE

CREATE (content-creator)
  Input : TrendReport (from PERCEIVE) + FeedbackReport (from LEARN)
  Output: ContentBundle → ACT

ACT (act-service)
  Input : ContentBundle (from CREATE)
  Output: PostResult + Transaction → LEARN

LEARN (learn-service)
  Input : PostResult + engagement signals (from ACT)
  Output: FeedbackReport → CREATE (and alert events → humans per Principle IV)
```

- No service MAY read from another service's database schema directly; cross-service data
  exchange MUST go through defined DTOs over the agreed messaging channel.
- Placeholder interfaces (no-op implementations) MUST be provided for any downstream
  service not yet built, keeping the full build green at all times.

## Governance

This constitution supersedes all informal conventions (CLAUDE.md session notes, inline
code comments, README stubs). When a conflict exists, the constitution wins.

**Amendment procedure**:

1. Any principle change MUST be proposed as a written amendment describing: (a) what changes,
   (b) why it changes, and (c) the migration path for all affected services.
2. MINOR and MAJOR version bumps require a compliance review of all four services before
   ratification; findings MUST be documented in the Sync Impact Report of the updated file.
3. PATCH amendments (clarifications, rewording, typo fixes) MAY be applied immediately and
   do not require a full service compliance review.

**Versioning policy** (semantic):

- **MAJOR**: A principle is removed, fundamentally redefined, or a new mandatory constraint
  that breaks existing service design is added.
- **MINOR**: A new principle or section is added, or guidance is materially expanded.
- **PATCH**: Clarifications, rewording, typo fixes, or non-semantic refinements.

**Compliance review**: Every new feature plan MUST include a "Constitution Check" gate
(present in `.specify/templates/plan-template.md`) that explicitly cites which principles
were checked and whether violations were found or justified in the Complexity Tracking table.

**Runtime guidance**: See `.claude/commands/` for agent-specific development workflows
and `.specify/templates/` for spec, plan, and task templates that encode these principles.

---

**Version**: 1.0.0 | **Ratified**: 2026-03-11 | **Last Amended**: 2026-03-11

# Project Chimera — Agent Rules File
# Version: 1.0.0 | Ratified: 2026-03-11

## 1. Spec Enforcement
- NEVER write production code without a ratified spec in specs/001-chimera-pipeline/
- Every commit MUST follow: [SERVICE][SPEC-REF] Description
- Valid services: PERCEIVE | CREATE | ACT | LEARN | INFRA | ALL

## 2. Service Boundaries
- trend-watcher → owns TrendReport only
- content-creator → owns ContentBundle only
- act-service → owns PostResult + Transaction only
- learn-service → owns FeedbackReport + TrendSignal only
- Services communicate ONLY via HTTP JSON — no shared DB tables

## 3. Forbidden Actions
FORBIDDEN-001: Commit secrets or API keys to any file
FORBIDDEN-002: Skip ContentSafetyFilter before CREATE → ACT handoff
FORBIDDEN-003: Publish content without valid non-null ContentBundle
FORBIDDEN-004: Auto-approve any transaction above $500 USD
FORBIDDEN-005: Swallow exceptions silently — every error MUST be logged
FORBIDDEN-006: Access another service's database directly
FORBIDDEN-007: DELETE or UPDATE any Transaction record (append-only)
FORBIDDEN-008: Use TrendReport older than 24 hours as CREATE input
FORBIDDEN-009: Dispatch FeedbackReport with confidenceScore < 0.6 without human review
FORBIDDEN-010: Push directly to main — all changes via pull request

## 4. HITL Thresholds (immutable without architect sign-off)
- Transaction > $500 USD → PENDING_APPROVAL (SLA: 30 min)
- Post failures ≥ 3 → HELD_FOR_HUMAN (SLA: 2 hours)
- Confidence score < 0.6 → HELD_PENDING_REVIEW (SLA: 4 hours)

## 5. Java Standards
- Java 21+ required — spring.threads.virtual.enabled=true on ALL services
- All inter-service DTOs MUST be Java Records (immutable)
- @Version column required on all shared-state entities (OCC)
- Append-only: Transaction, AuditLog, PostResult

## 6. Error Logging Mandate
Every caught exception MUST log: [stage][agentId][operation] errorType — message
Silent catch blocks = FORBIDDEN-005 violation.

## 7. Dev MCP vs Runtime Skills Separation
- Dev MCP (.cursor/mcp.json): development session tooling only
- Runtime Skills (.chimera/skills/): agent capabilities at runtime
- NEVER mix dev tooling config with runtime skill definitions

## 8. Security
SEC-001: No secrets in source code, Dockerfiles, or YAML
SEC-002: All secrets via environment variables at runtime
SEC-003: ContentSafetyFilter runs in BOTH PERCEIVE and CREATE
SEC-004: Trivy scan must pass (zero CRITICAL) before production deploy

## 9. Architect Sign-Off Required For
1. Any change to HITL thresholds
2. Any change to Transaction entity schema
3. Any new inter-service dependency
4. Any change to this rules file

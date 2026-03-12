# Skill: publish_content | Service: ACT | Spec: FR-008, FR-009, FR-010, FR-011
## Input: { contentBundleId, agentId, targetPlatforms, transaction }
## Output: PostResult { id, contentBundleId, platform, status, attemptCount }
## MCP Tools: post_to_platform (port 3003), process_transaction (port 3004)
## HITL Triggers:
- Transaction > $500 → PENDING_APPROVAL
- Post fails 3x → HELD_FOR_HUMAN
## Invariants:
- MUST NEVER publish without valid ContentBundle with PASS safety status
- MUST log every transaction — append-only, 7-year retention
- Retry 3x with exponential backoff (1s, 3s, 9s)

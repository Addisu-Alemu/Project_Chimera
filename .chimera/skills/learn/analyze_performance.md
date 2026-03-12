# Skill: analyze_performance | Service: LEARN | Spec: FR-012, FR-013, FR-014, FR-015
## Input: { postResultId, contentBundleId, agentId, engagementMetrics }
## Output: FeedbackReport { id, contentBundleId, confidenceScore [0.0-1.0], reviewStatus }
##         TrendSignal { id, sourceFeedbackReportId, categoryWeights }
## MCP Tool: analyze_interaction_data (chimera-learn-mcp port 3005)
## HITL Trigger: confidenceScore < 0.6 → HELD_PENDING_REVIEW
## Invariants:
- confidenceScore MUST be in [0.0, 1.0] — IllegalArgumentException if violated
- TrendSignal MUST be dispatched to PERCEIVE after EVERY cycle

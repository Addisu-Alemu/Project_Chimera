# Skill: generate_content | Service: CREATE | Spec: FR-004, FR-005, FR-006, FR-007
## Input: { trendReportId, agentId, agentPersona }
## Output: ContentBundle { id, agentId, trendReportId, caption, hashtags, safetyPassedAt }
## MCP Tool: generate_content_bundle (chimera-create-mcp port 3002)
## Invariants:
- ContentBundle MUST carry trendReportId (traceability)
- MUST pass ContentSafetyFilter — regenerate up to 3x on failure
- Stale TrendReport (>24h) rejected — request fresh from PERCEIVE

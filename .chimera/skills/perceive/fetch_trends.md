# Skill: fetch_trends | Service: PERCEIVE | Spec: FR-001, FR-002, FR-003
## Input: { agentId, platforms, timeWindow }
## Output: TrendReport { id, agentId, fetchedAt, platforms, topics, categoryWeights }
## MCP Tool: fetch_trending_data (chimera-perceive-mcp port 3001)
## Invariants:
- fetchedAt MUST be within 24h — StaleTrendReportException if violated
- All items MUST pass ContentSafetyFilter
- Data from verified platforms only (TikTok, Instagram, X, BBC, Reuters)

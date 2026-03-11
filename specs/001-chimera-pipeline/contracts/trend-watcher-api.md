# Contract: trend-watcher (PERCEIVE)

**Service**: trend-watcher
**Stage**: PERCEIVE
**Base URL**: `http://trend-watcher:8081`
**Protocol**: HTTP/1.1 REST + JSON
**Auth**: Internal service-to-service (no public exposure)

---

## Endpoints

### POST /trend-reports

Trigger a new trend-watching cycle for an agent.

**Request**

```json
{
  "agentId": "uuid"
}
```

**Response 202 Accepted**

```json
{
  "reportId": "uuid",
  "agentId": "uuid",
  "status": "PROCESSING"
}
```

**Response 422 Unprocessable Entity** — agentId missing or malformed

**Response 503 Service Unavailable** — all platform fetchers failed (returns after exhausting retries)

---

### GET /trend-reports/{reportId}

Retrieve a TrendReport by ID (served from Redis cache if available).

**Response 200 OK**

```json
{
  "id": "uuid",
  "agentId": "uuid",
  "fetchedAt": "2026-03-11T10:00:00Z",
  "platforms": ["TIKTOK", "INSTAGRAM", "X"],
  "topics": [
    {
      "name": "string",
      "hashtags": ["#example"],
      "engagementScore": 0.87,
      "safetyPassed": true
    }
  ],
  "categoryWeights": {
    "fashion": 0.4,
    "fitness": 0.3,
    "food": 0.3
  }
}
```

**Response 404 Not Found** — reportId unknown or TTL expired (stale — must re-generate)

**Response 410 Gone** — report existed but was discarded as stale (fetchedAt > 24h ago)

---

### POST /trend-signals

Receive a TrendSignal from LEARN to adjust category weighting for an agent's next cycle.

**Request body**: TrendSignal record

```json
{
  "id": "uuid",
  "agentId": "uuid",
  "sourceFeedbackReportId": "uuid",
  "categoryWeights": {
    "fashion": 0.6,
    "fitness": 0.2,
    "food": 0.2
  },
  "issuedAt": "2026-03-11T11:00:00Z"
}
```

**Response 204 No Content** — signal accepted and stored for next cycle

**Response 422 Unprocessable Entity** — malformed signal body

---

## Error Format (all error responses)

```json
{
  "error": "string — machine-readable code",
  "message": "string — human-readable description",
  "timestamp": "ISO-8601",
  "service": "trend-watcher"
}
```

---

## Events Emitted (logged, not yet async)

| Event | When |
|-------|------|
| `TREND_REPORT_PRODUCED` | Successful TrendReport produced |
| `TREND_REPORT_STALE_DISCARDED` | TrendReport discarded for staleness |
| `SAFETY_FILTER_DISCARD` | Content item removed by safety filter |
| `PLATFORM_FETCH_FAILED` | A platform fetcher failed (with platform name) |
| `HUMAN_ALERT_ISSUED` | Not emitted by this service directly |

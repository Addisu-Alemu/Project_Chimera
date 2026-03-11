# Contract: content-creator (CREATE)

**Service**: content-creator
**Stage**: CREATE
**Base URL**: `http://content-creator:8082`
**Protocol**: HTTP/1.1 REST + JSON
**Auth**: Internal service-to-service

---

## Endpoints

### POST /content-bundles

Generate a ContentBundle from a given TrendReport.

**Request**

```json
{
  "agentId": "uuid",
  "trendReportId": "uuid"
}
```

**Behaviour**:
1. Fetch TrendReport from trend-watcher by `trendReportId`
2. Validate TrendReport freshness (reject if `fetchedAt` > 24h ago)
3. Apply FeedbackReport parameters for `agentId` if available
4. Generate caption, hashtags, video description
5. Run safety filter; regenerate up to 3× on failure
6. Return ContentBundle

**Response 202 Accepted**

```json
{
  "bundleId": "uuid",
  "agentId": "uuid",
  "status": "PROCESSING"
}
```

**Response 422 Unprocessable Entity** — missing agentId or trendReportId

**Response 409 Conflict** — TrendReport is stale (fetchedAt > 24h); caller must request new TrendReport

**Response 503 Service Unavailable** — all 3 safety-filter-regeneration attempts failed

---

### GET /content-bundles/{bundleId}

Retrieve a generated ContentBundle.

**Response 200 OK**

```json
{
  "id": "uuid",
  "agentId": "uuid",
  "trendReportId": "uuid",
  "caption": "string — max 2200 chars",
  "hashtags": ["#example"],
  "videoDescription": "string — max 500 chars",
  "safetyPassedAt": "2026-03-11T10:05:00Z",
  "generatedAt": "2026-03-11T10:04:55Z"
}
```

**Response 404 Not Found** — bundleId unknown

---

### POST /feedback-reports

Receive a FeedbackReport from LEARN to adjust generation parameters for an agent.
Only called for reports with `reviewStatus = AUTO_DISPATCHED` or `HUMAN_APPROVED`.

**Request body**: FeedbackReport record

```json
{
  "id": "uuid",
  "agentId": "uuid",
  "contentBundleId": "uuid",
  "confidenceScore": 0.82,
  "engagementSummary": {
    "likes": 1500,
    "shares": 320,
    "comments": 88,
    "views": 45000,
    "clickThroughRate": 0.034
  },
  "reviewStatus": "AUTO_DISPATCHED",
  "generatedAt": "2026-03-11T12:00:00Z",
  "dispatchedAt": "2026-03-11T12:00:05Z"
}
```

**Response 204 No Content** — feedback accepted and applied to `agentId` generation parameters

**Response 422 Unprocessable Entity** — malformed body or `reviewStatus = HELD_PENDING_REVIEW`
  (LEARN must not dispatch held reports)

---

## Error Format

```json
{
  "error": "string",
  "message": "string",
  "timestamp": "ISO-8601",
  "service": "content-creator"
}
```

---

## Events Emitted

| Event | When |
|-------|------|
| `CONTENT_BUNDLE_PRODUCED` | ContentBundle passed safety filter and ready |
| `SAFETY_FILTER_DISCARD` | ContentBundle discarded by safety filter (with attempt count) |
| `STALE_TREND_REPORT_REJECTED` | TrendReport rejected for staleness |
| `FEEDBACK_APPLIED` | FeedbackReport successfully applied to generation parameters |

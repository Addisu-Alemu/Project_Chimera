# Contract: learn-service (LEARN)

**Service**: learn-service
**Stage**: LEARN
**Base URL**: `http://learn-service:8084`
**Protocol**: HTTP/1.1 REST + JSON
**Auth**: Internal service-to-service

---

## Endpoints

### POST /analyze

Submit a PostResult for engagement analysis. Triggers the full LEARN cycle:
score computation → FeedbackReport → dispatch to CREATE + TrendSignal to PERCEIVE.

**Request**

```json
{
  "agentId": "uuid",
  "postResultId": "uuid"
}
```

**Behaviour**:
1. Collect engagement signals for `postResultId` from `engagement_signals` table
2. Compute confidence score [0.0, 1.0]
3. Produce FeedbackReport
4. If `confidenceScore < 0.6`:
   - Set `reviewStatus=HELD_PENDING_REVIEW`
   - Issue HumanAlert (type=LOW_CONFIDENCE)
   - Do NOT dispatch FeedbackReport to CREATE
5. If `confidenceScore ≥ 0.6`:
   - Set `reviewStatus=AUTO_DISPATCHED`
   - Dispatch FeedbackReport to CREATE
6. Always dispatch TrendSignal to PERCEIVE (regardless of hold status)

**Response 202 Accepted**

```json
{
  "feedbackReportId": "uuid",
  "agentId": "uuid",
  "confidenceScore": 0.82,
  "reviewStatus": "AUTO_DISPATCHED | HELD_PENDING_REVIEW"
}
```

**Response 422 Unprocessable Entity** — missing or malformed request

---

### POST /engagement-signals

Ingest a raw engagement signal from ACT.

**Request**

```json
{
  "agentId": "uuid",
  "postResultId": "uuid",
  "signalType": "LIKE | SHARE | COMMENT | VIEW | CLICK",
  "value": 1500,
  "recordedAt": "2026-03-11T10:30:00Z"
}
```

**Response 204 No Content** — signal accepted and persisted

**Response 422 Unprocessable Entity** — invalid signalType or negative value

---

### GET /feedback-reports/{feedbackReportId}

Retrieve a FeedbackReport by ID.

**Response 200 OK**

```json
{
  "id": "uuid",
  "agentId": "uuid",
  "contentBundleId": "uuid",
  "confidenceScore": 0.45,
  "engagementSummary": {
    "likes": 200,
    "shares": 10,
    "comments": 5,
    "views": 3000,
    "clickThroughRate": 0.007
  },
  "reviewStatus": "HELD_PENDING_REVIEW",
  "generatedAt": "2026-03-11T12:00:00Z",
  "dispatchedAt": null
}
```

**Response 404 Not Found**

---

### POST /feedback-reports/{feedbackReportId}/approve

Human operator approves a held FeedbackReport for dispatch to CREATE.

**Request**

```json
{
  "operatorId": "string",
  "decision": "APPROVED | REJECTED"
}
```

**Behaviour**:
- `APPROVED`: set `reviewStatus=HUMAN_APPROVED`, dispatch FeedbackReport to CREATE
- `REJECTED`: set `reviewStatus=REJECTED`, do not dispatch

**Response 200 OK**

```json
{
  "feedbackReportId": "uuid",
  "reviewStatus": "HUMAN_APPROVED | REJECTED",
  "dispatchedAt": "2026-03-11T13:00:00Z"
}
```

---

## Error Format

```json
{
  "error": "string",
  "message": "string",
  "timestamp": "ISO-8601",
  "service": "learn-service"
}
```

---

## Events Emitted

| Event | When |
|-------|------|
| `ENGAGEMENT_SIGNAL_INGESTED` | Raw signal stored |
| `FEEDBACK_REPORT_PRODUCED` | Confidence score computed |
| `FEEDBACK_REPORT_DISPATCHED` | Report sent to CREATE (confidence ≥ 0.6 or human-approved) |
| `FEEDBACK_REPORT_HELD` | Report held for human review (confidence < 0.6) |
| `TREND_SIGNAL_DISPATCHED` | TrendSignal sent to PERCEIVE |
| `HUMAN_ALERT_ISSUED` | Low-confidence threshold alert sent |

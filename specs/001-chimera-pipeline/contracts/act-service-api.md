# Contract: act-service (ACT)

**Service**: act-service
**Stage**: ACT
**Base URL**: `http://act-service:8083`
**Protocol**: HTTP/1.1 REST + JSON
**Auth**: Internal service-to-service

---

## Endpoints

### POST /publish

Publish a ContentBundle to a social platform.

**Request**

```json
{
  "agentId": "uuid",
  "contentBundleId": "uuid",
  "platform": "TIKTOK | INSTAGRAM | X"
}
```

**Behaviour**:
1. Fetch ContentBundle from content-creator; validate `trendReportId` present and `safetyPassedAt` present
2. Attempt publish to platform
3. On failure: retry up to 3 times (exponential back-off)
4. After 3 failures: set `status=HELD_FOR_HUMAN`, issue HumanAlert, return 202
5. On success: record PostResult, forward to LEARN

**Response 202 Accepted** (async — result via GET /post-results/{id})

```json
{
  "postResultId": "uuid",
  "agentId": "uuid",
  "status": "PROCESSING | SUCCESS | HELD_FOR_HUMAN"
}
```

**Response 400 Bad Request** — missing fields or invalid platform

**Response 422 Unprocessable Entity** — ContentBundle missing `trendReportId` or `safetyPassedAt`

---

### GET /post-results/{postResultId}

Retrieve the outcome of a publish attempt.

**Response 200 OK**

```json
{
  "id": "uuid",
  "agentId": "uuid",
  "contentBundleId": "uuid",
  "platform": "TIKTOK",
  "publishedAt": "2026-03-11T10:10:00Z",
  "status": "SUCCESS | RETRYING | HELD_FOR_HUMAN | FAILED",
  "attemptCount": 1,
  "failureReason": null,
  "platformPostId": "platform-assigned-id"
}
```

**Response 404 Not Found**

---

### POST /transactions

Record and process a financial transaction.

**Request**

```json
{
  "agentId": "uuid",
  "contentBundleId": "uuid",
  "type": "SPONSORED_POST | AFFILIATE | WITHDRAWAL",
  "amount": 750.00,
  "currency": "USD",
  "platform": "TIKTOK"
}
```

**Behaviour**:
- If `amount > 500`: set `status=PENDING_APPROVAL`, issue HumanAlert, return 202
- If `amount ≤ 500`: process immediately, set `status=COMPLETED`, return 201
- All transactions are persisted on receipt (before processing decision)

**Response 201 Created** — processed immediately (amount ≤ $500)

```json
{
  "transactionId": "uuid",
  "status": "COMPLETED",
  "amount": 150.00,
  "currency": "USD"
}
```

**Response 202 Accepted** — pending human approval (amount > $500)

```json
{
  "transactionId": "uuid",
  "status": "PENDING_APPROVAL",
  "amount": 750.00,
  "alertId": "uuid"
}
```

**Response 422 Unprocessable Entity** — missing fields or invalid currency

---

### POST /transactions/{transactionId}/approve

Human operator approves a paused transaction.

**Request**

```json
{
  "operatorId": "string",
  "decision": "APPROVED | REJECTED"
}
```

**Response 200 OK**

```json
{
  "transactionId": "uuid",
  "status": "COMPLETED | REJECTED",
  "approvedBy": "operator-id",
  "completedAt": "2026-03-11T11:00:00Z"
}
```

---

### POST /interactions

Receive and enqueue an audience interaction for processing.

**Request**

```json
{
  "agentId": "uuid",
  "postResultId": "uuid",
  "platform": "TIKTOK",
  "interactionType": "COMMENT | LIKE | SHARE | FOLLOW",
  "content": "string — optional, for COMMENT type"
}
```

**Response 202 Accepted**

---

## Error Format

```json
{
  "error": "string",
  "message": "string",
  "timestamp": "ISO-8601",
  "service": "act-service"
}
```

---

## Events Emitted

| Event | When |
|-------|------|
| `POST_PUBLISHED` | Successful platform publish |
| `POST_RETRY` | Publish failed, retry attempt (with attempt number) |
| `POST_HELD_FOR_HUMAN` | All 3 retries failed, HumanAlert issued |
| `TRANSACTION_CREATED` | Transaction persisted |
| `TRANSACTION_PENDING_APPROVAL` | Transaction > $500, awaiting human |
| `TRANSACTION_COMPLETED` | Transaction approved and completed |
| `TRANSACTION_REJECTED` | Transaction rejected by operator |
| `HUMAN_ALERT_ISSUED` | Any threshold crossed (type included in payload) |

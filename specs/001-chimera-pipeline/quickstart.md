# Quickstart: Project Chimera — Local Development

**Branch**: `001-chimera-pipeline`
**Stack**: Java 21, Spring Boot 3, PostgreSQL 16, Redis 7, Docker Compose

---

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java | 21 (LTS) | `sdk install java 21-tem` (SDKMAN) or download eclipse-temurin |
| Maven | 3.9+ | `sdk install maven` or system package manager |
| Docker | 24+ | https://docs.docker.com/get-docker/ |
| Docker Compose | v2+ | Bundled with Docker Desktop |

---

## Environment Variables

Create a `.env` file at the repository root (never commit this file):

```env
# Platform API credentials
TIKTOK_ACCESS_TOKEN=your-tiktok-token
INSTAGRAM_ACCESS_TOKEN=your-instagram-token
X_BEARER_TOKEN=your-x-bearer-token

# Human alert delivery (configure one)
ALERT_CHANNEL=slack
ALERT_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK

# Database (overridden in Docker Compose — set these for local bare-metal dev only)
POSTGRES_URL=jdbc:postgresql://localhost:5432/chimera
POSTGRES_USER=chimera
POSTGRES_PASSWORD=chimera_dev_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
```

---

## Running the Full Stack (Docker Compose)

```bash
# 1. From repository root — start all infrastructure + services
docker compose up --build

# Services will start on:
#   trend-watcher:    http://localhost:8081
#   content-creator:  http://localhost:8082
#   act-service:      http://localhost:8083
#   learn-service:    http://localhost:8084
#   PostgreSQL:       localhost:5432
#   Redis:            localhost:6379

# 2. Verify all services are healthy
docker compose ps

# 3. Trigger a test pipeline cycle (replace agentId with a UUID)
curl -X POST http://localhost:8081/trend-reports \
  -H "Content-Type: application/json" \
  -d '{"agentId": "11111111-1111-1111-1111-111111111111"}'

# 4. Stop all services
docker compose down
```

---

## Running Individual Services (development)

Start infrastructure first:

```bash
docker compose up postgres redis -d
```

Then run any service independently:

```bash
# trend-watcher
cd trend-watcher
mvn spring-boot:run

# content-creator
cd content-creator
mvn spring-boot:run

# act-service
cd act-service
mvn spring-boot:run

# learn-service
cd learn-service
mvn spring-boot:run
```

---

## Building

```bash
# Build all services in order
cd trend-watcher   && mvn package && cd ..
cd content-creator && mvn package && cd ..
cd act-service     && mvn package && cd ..
cd learn-service   && mvn package && cd ..

# Build Docker images
docker compose build
```

---

## Running Tests

```bash
# Unit tests (no infrastructure required)
cd trend-watcher && mvn test -Dgroups=unit
# Repeat for each service

# Integration tests (requires Docker for Testcontainers)
cd trend-watcher && mvn verify -Dgroups=integration
# Testcontainers will auto-start PostgreSQL 16 and Redis 7 containers
```

---

## Triggering a Full Pipeline Cycle (manual test)

```bash
AGENT_ID="11111111-1111-1111-1111-111111111111"

# Step 1 — PERCEIVE: generate TrendReport
REPORT_ID=$(curl -s -X POST http://localhost:8081/trend-reports \
  -H "Content-Type: application/json" \
  -d "{\"agentId\": \"$AGENT_ID\"}" | jq -r '.reportId')
echo "TrendReport: $REPORT_ID"

# Step 2 — CREATE: generate ContentBundle
BUNDLE_ID=$(curl -s -X POST http://localhost:8082/content-bundles \
  -H "Content-Type: application/json" \
  -d "{\"agentId\": \"$AGENT_ID\", \"trendReportId\": \"$REPORT_ID\"}" | jq -r '.bundleId')
echo "ContentBundle: $BUNDLE_ID"

# Step 3 — ACT: publish
POST_RESULT_ID=$(curl -s -X POST http://localhost:8083/publish \
  -H "Content-Type: application/json" \
  -d "{\"agentId\": \"$AGENT_ID\", \"contentBundleId\": \"$BUNDLE_ID\", \"platform\": \"TIKTOK\"}" \
  | jq -r '.postResultId')
echo "PostResult: $POST_RESULT_ID"

# Step 4 — LEARN: analyze
FEEDBACK_ID=$(curl -s -X POST http://localhost:8084/analyze \
  -H "Content-Type: application/json" \
  -d "{\"agentId\": \"$AGENT_ID\", \"postResultId\": \"$POST_RESULT_ID\"}" \
  | jq -r '.feedbackReportId')
echo "FeedbackReport: $FEEDBACK_ID"

# Verify traceability
curl -s http://localhost:8084/feedback-reports/$FEEDBACK_ID | jq .
```

---

## Validating Quickstart

After running the full pipeline cycle above, verify:

- [ ] TrendReport created with `fetchedAt` within the last 24 hours
- [ ] ContentBundle references `trendReportId` matching the TrendReport ID above
- [ ] PostResult references `contentBundleId` matching the ContentBundle ID above
- [ ] FeedbackReport contains a `confidenceScore` in range [0.0, 1.0]
- [ ] FeedbackReport `contentBundleId` matches the ContentBundle ID above
- [ ] No errors in `docker compose logs` for any service

---

## Human Alert Test

```bash
# Trigger a transaction above the $500 threshold
curl -X POST http://localhost:8083/transactions \
  -H "Content-Type: application/json" \
  -d "{
    \"agentId\": \"$AGENT_ID\",
    \"contentBundleId\": \"$BUNDLE_ID\",
    \"type\": \"SPONSORED_POST\",
    \"amount\": 750.00,
    \"currency\": \"USD\",
    \"platform\": \"TIKTOK\"
  }"

# Expected: 202 Accepted with status=PENDING_APPROVAL
# Check your configured alert channel for the HumanAlert notification
```

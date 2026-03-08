# Project Chimera — Architectural Decision Record

## Project
Autonomous Influencer Network built with Java 21.

## Philosophy

### Spec before code
No code was written before the spec was defined. Every service has a written spec with inputs, outputs, rules, and failure handling before implementation.

### Single responsibility
Each service does only one job:
- **PERCEIVE** (`trend-watcher`) — watches trends, nothing else
- **CREATE** (`content-creator`) — creates content, nothing else
- **ACT** (`act-service`) — publishes and transacts, nothing else
- **LEARN** (`learn-service`) — feeds results back, nothing else

No service crosses into another's responsibility.

### Autonomous by default, human in the loop for edge cases only
The system handles everything autonomously. Humans are only alerted for:
- Suspicious financial transactions
- Repeated publish failures after retries exhausted
- Corrupt signals in the data pipeline
- Negative content performance requiring strategy review

### No silent failures
Every failure has a defined response. All error paths are explicit: primary→backup fallback, bounded retry queues, human alerts after retries exhausted. Nothing fails quietly.

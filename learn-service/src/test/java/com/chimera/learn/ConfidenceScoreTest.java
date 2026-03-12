package com.chimera.learn;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * RED PHASE — TDD contract tests for LEARN (learn-service).
 *
 * These tests define the contract mandated by spec.md before integration is wired.
 * Every test MUST remain failing until the corresponding production behaviour is
 * implemented and verified green.
 *
 * Spec reference : specs/001-chimera-pipeline/spec.md
 * Constitution   : .specify/memory/constitution.md (Principle IV — Human-in-the-Loop)
 * Agent rules    : .chimera/agent.rules.md (FORBIDDEN-009)
 */
@DisplayName("LEARN — Confidence Score and TrendSignal Dispatch Contracts")
class ConfidenceScoreTest {

    // -------------------------------------------------------------------------
    // FR-013 — Low confidence → HELD_PENDING_REVIEW, NOT dispatched to CREATE
    // -------------------------------------------------------------------------

    /**
     * Contract: LearnService.analyze() MUST set FeedbackReport.reviewStatus to
     * ReviewStatus.HELD_PENDING_REVIEW when ConfidenceScorer returns a score < 0.6.
     * The report MUST NOT be dispatched to CREATE (CreateFeedbackAdapter.dispatch()
     * MUST NOT be called).  A HumanAlert of type LOW_CONFIDENCE MUST be raised.
     *
     * Spec text (FR-013):
     *   "LEARN MUST hold any FeedbackReport with a confidence score below 0.6 for
     *    human review; it MUST NOT dispatch such a report to CREATE until a human
     *    approves or overrides it."
     *
     * Constitution Principle IV — HITL table:
     *   "Confidence score (LEARN) | Score < 0.6 on a ContentBundle |
     *    Flag for human review before next publish cycle"
     *
     * Agent rules: FORBIDDEN-009 — Dispatching a FeedbackReport with confidenceScore
     * < 0.6 without human review is explicitly forbidden.
     *
     * Spec SC-006 (success criterion):
     *   "100% of FeedbackReports with confidence below 0.6 are held for human review
     *    and not dispatched to CREATE without approval; zero auto-dispatch of
     *    low-confidence feedback."
     *
     * Implementation wire-up required:
     *   com.chimera.learnservice.service.LearnService.analyze()
     *   com.chimera.learnservice.model.ReviewStatus.HELD_PENDING_REVIEW
     *   com.chimera.learnservice.connector.CreateFeedbackAdapter (must NOT be called)
     *   com.chimera.learnservice.alert.HumanAlertService.raise(AlertType.LOW_CONFIDENCE)
     */
    @Test
    @DisplayName("[FR-013] confidenceScore < 0.6 sets HELD_PENDING_REVIEW, never dispatched to CREATE")
    void lowConfidenceScore_setsHeldPendingReview_notDispatchedToCreate() {
        fail("RED PHASE [FR-013]: LearnService.analyze() must produce a FeedbackReport with " +
             "reviewStatus == ReviewStatus.HELD_PENDING_REVIEW when confidenceScore < 0.6. " +
             "CreateFeedbackAdapter.dispatch() must NOT be called for this report. " +
             "HumanAlertService.raise() must be called once with AlertType.LOW_CONFIDENCE. " +
             "Wire: mock ConfidenceScorer to return 0.45; mock CreateFeedbackAdapter; " +
             "mock PerceiveFeedbackAdapter (TrendSignal must still be dispatched — see FR-014); " +
             "call LearnService.analyze(); assert reviewStatus == HELD_PENDING_REVIEW and " +
             "verify createAdapter.dispatch() zero interactions.");
    }

    // -------------------------------------------------------------------------
    // FR-012 — confidenceScore must be in [0.0, 1.0]
    // -------------------------------------------------------------------------

    /**
     * Contract: ConfidenceScorer.score() MUST always return a value in the closed
     * range [0.0, 1.0].  If the computed raw score falls outside this range,
     * the scorer MUST clamp it rather than return an invalid value.
     * Callers (e.g. ReportBuilder) MUST throw IllegalArgumentException if a score
     * outside [0.0, 1.0] is supplied — ensuring no invalid score can be persisted.
     *
     * Spec text (FR-012):
     *   "LEARN MUST compute a confidence score in the range [0.0, 1.0] for each
     *    ContentBundle based on engagement signals received from ACT."
     *
     * Two sub-contracts this test covers:
     *   (a) ConfidenceScorer.score() must never produce a value < 0.0 or > 1.0.
     *   (b) Any code path that accepts an externally supplied score (e.g. override)
     *       must reject values outside [0.0, 1.0] with IllegalArgumentException.
     *
     * Implementation wire-up required:
     *   com.chimera.learnservice.analyzer.ConfidenceScorer.score() — clamp to [0,1]
     *   com.chimera.learnservice.service.ReportBuilder — guard on supplied score
     */
    @Test
    @DisplayName("[FR-012] confidenceScore outside [0.0, 1.0] throws IllegalArgumentException")
    void confidenceScore_outsideValidRange_throwsIllegalArgumentException() {
        fail("RED PHASE [FR-012]: ConfidenceScorer.score() must return a value clamped to [0.0, 1.0]. " +
             "Additionally, ReportBuilder (or LearnService) must throw IllegalArgumentException " +
             "if a caller attempts to build a FeedbackReport with a score outside this range " +
             "(e.g. -0.1 or 1.1). " +
             "Wire: (a) Unit-test ConfidenceScorer with an EngagementSummary whose raw weighted " +
             "total exceeds 1.0 — assert returned score <= 1.0. " +
             "(b) Call ReportBuilder.build() with score = -0.1 — assert IllegalArgumentException thrown. " +
             "Both sub-cases must pass for this test to go green.");
    }

    // -------------------------------------------------------------------------
    // FR-014 / FR-015 — TrendSignal dispatched to PERCEIVE after every cycle
    // -------------------------------------------------------------------------

    /**
     * Contract: LearnService.analyze() MUST dispatch exactly one TrendSignalDto to
     * PerceiveFeedbackAdapter after every analysis cycle — regardless of whether the
     * FeedbackReport was held (LOW_CONFIDENCE) or auto-dispatched.
     *
     * Spec text (FR-014):
     *   "LEARN MUST dispatch a TrendSignal to PERCEIVE after each analysis cycle,
     *    regardless of whether the associated FeedbackReport was held."
     *
     * Spec text (FR-015):
     *   "Every human alert MUST include: alert type, threshold exceeded, triggering
     *    record ID, and a direct navigable link to that record."
     *   (Verified here in conjunction: the TrendSignal path and the HumanAlert path
     *    are independent; both must fire on a low-confidence cycle.)
     *
     * Spec SC-001 (success criterion) — indirect dependency:
     *   "All inter-stage outputs [are] traceable to their source inputs by record ID."
     *   TrendSignal carries sourceFeedbackReportId; omitting the dispatch breaks this chain.
     *
     * Implementation wire-up required:
     *   com.chimera.learnservice.service.LearnService.analyze()
     *   com.chimera.learnservice.connector.PerceiveFeedbackAdapter.dispatch(TrendSignalDto)
     *   com.chimera.learnservice.model.TrendSignalDto (sourceFeedbackReportId, categoryWeights)
     */
    @Test
    @DisplayName("[FR-014] TrendSignal dispatched to PERCEIVE after every analysis cycle, held or not")
    void trendSignal_dispatchedToPerceive_afterEveryAnalysisCycle() {
        fail("RED PHASE [FR-014]: LearnService.analyze() must call " +
             "PerceiveFeedbackAdapter.dispatch(TrendSignalDto) exactly once per analyze() " +
             "invocation, regardless of whether the FeedbackReport is HELD_PENDING_REVIEW " +
             "or AUTO_DISPATCHED. " +
             "TrendSignalDto must carry: a non-null id, the agentId, the FeedbackReport id " +
             "as sourceFeedbackReportId, a non-empty categoryWeights map, and a non-null issuedAt. " +
             "Wire: run two scenarios — (a) confidence = 0.45 (held) and (b) confidence = 0.75 " +
             "(auto-dispatched); in both cases verify perceiveAdapter.dispatch() called exactly once " +
             "and the dispatched TrendSignalDto.sourceFeedbackReportId() equals report.getId().");
    }
}

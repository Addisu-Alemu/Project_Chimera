package com.chimera.create;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * RED PHASE — TDD contract tests for CREATE (content-creator).
 *
 * These tests define the contract mandated by spec.md before integration is wired.
 * Every test MUST remain failing until the corresponding production behaviour is
 * implemented and verified green.
 *
 * Spec reference : specs/001-chimera-pipeline/spec.md
 * Constitution   : .specify/memory/constitution.md (Principle VI — Traceability)
 * Agent rules    : .chimera/agent.rules.md (FORBIDDEN-002, FORBIDDEN-008)
 */
@DisplayName("CREATE — ContentBundle Validation Contracts")
class ContentBundleValidationTest {

    // -------------------------------------------------------------------------
    // FR-007 — Traceability: trendReportId is mandatory
    // -------------------------------------------------------------------------

    /**
     * Contract: ContentCreatorService MUST reject any ContentBundle that does not
     * carry a non-null trendReportId before the bundle is forwarded to ACT.
     * A bundle without this reference breaks the full audit chain
     * PostResult → ContentBundle → TrendReport.
     *
     * Spec text (FR-007):
     *   "Every ContentBundle MUST carry a reference to the TrendReport ID that
     *    produced it; bundles without this reference MUST be rejected before
     *    reaching ACT."
     *
     * Constitution Principle VI (Traceability):
     *   "Every ContentBundle produced by CREATE MUST reference the TrendReport.id
     *    that generated it; this trendReportId MUST be persisted alongside the bundle."
     *
     * Expected behaviour: throw IllegalStateException (or InvalidContentBundleException)
     * when ContentBundle.trendReportId() is null at the ACT handoff boundary.
     *
     * Implementation wire-up required:
     *   com.chimera.contentcreator.model.ContentBundle (record — trendReportId field)
     *   com.chimera.contentcreator.service.ContentCreatorService — pre-handoff guard
     */
    @Test
    @DisplayName("[FR-007] ContentBundle without trendReportId is rejected before reaching ACT")
    void contentBundle_withNullTrendReportId_isRejectedBeforeAct() {
        fail("RED PHASE [FR-007]: ContentCreatorService must reject any ContentBundle " +
             "whose trendReportId() is null before the bundle is dispatched to ACT. " +
             "Throw IllegalStateException (or a dedicated InvalidContentBundleException) " +
             "and log at ERROR with agentId and bundleId. " +
             "Wire: ContentCreatorService pre-handoff null-check on ContentBundle.trendReportId().");
    }

    // -------------------------------------------------------------------------
    // FR-005 / FR-006 — Safety filter regeneration, max 3 attempts
    // -------------------------------------------------------------------------

    /**
     * Contract: When ContentSafetyFilter rejects a generated ContentBundle,
     * ContentCreatorService MUST regenerate the bundle and retry — up to a maximum
     * of 3 attempts.  After 3 consecutive safety failures it MUST throw
     * ContentSafetyException and MUST NOT silently return a partial or unsafe bundle.
     *
     * Spec text (FR-005 / FR-006):
     *   "CREATE MUST produce ContentBundles comprising a caption, a hashtag set,
     *    and a video description derived from the current TrendReport."
     *   "CREATE MUST pass all ContentBundles through a safety filter before
     *    forwarding to ACT; bundles failing the filter MUST be discarded and
     *    regenerated."
     *
     * Constitution Principle III (No Silent Failures):
     *   "Retry logic MUST be explicit: maximum attempt count and back-off strategy
     *    MUST be declared in code (not implicit through framework defaults)."
     *
     * Implementation wire-up required:
     *   com.chimera.contentcreator.filter.ContentSafetyFilter
     *   com.chimera.contentcreator.generator.TemplateContentGenerator
     *   com.chimera.contentcreator.service.ContentCreatorService — retry loop (MAX_SAFETY_RETRIES = 3)
     *   com.chimera.contentcreator.exception.ContentSafetyException — thrown after exhaustion
     */
    @Test
    @DisplayName("[FR-005] ContentSafetyFilter failure triggers regeneration up to 3x, then throws")
    void safetyFilterFailure_triggersRegeneration_maxThreeAttempts_thenThrows() {
        fail("RED PHASE [FR-005]: ContentCreatorService must retry content generation when " +
             "ContentSafetyFilter.passes() returns false, up to MAX_SAFETY_RETRIES (3). " +
             "After 3 consecutive failures, throw ContentSafetyException — never return an " +
             "unsafe bundle. Each failure must be logged at WARN with attempt number. " +
             "Wire: ContentCreatorService retry loop; stub TemplateContentGenerator to always " +
             "produce blocked text; assert ContentSafetyException is thrown on 3rd failure.");
    }

    // -------------------------------------------------------------------------
    // FR-003 / FR-004 — Stale TrendReport rejection at CREATE boundary
    // -------------------------------------------------------------------------

    /**
     * Contract: ContentCreatorService MUST reject a TrendReport whose fetchedAt is
     * older than 24 hours by throwing StaleTrendReportException.  On rejection it
     * MUST NOT produce any ContentBundle and MUST signal to the caller that a fresh
     * TrendReport is required from PERCEIVE.
     *
     * Spec text (FR-003 / FR-004):
     *   "PERCEIVE MUST discard and regenerate any TrendReport whose fetched data is
     *    older than 24 hours."
     *   Implicit: CREATE must also enforce this boundary at ingestion time to prevent
     *    stale data from propagating further.
     *
     * Agent rule: FORBIDDEN-008 — Use of TrendReport older than 24 hours as CREATE
     * input is explicitly forbidden.
     *
     * Expected behaviour: throw StaleTrendReportException before any content is
     * generated; caller must request a fresh report from PERCEIVE and retry.
     *
     * Implementation wire-up required:
     *   com.chimera.contentcreator.client.dto.TrendReportDto (fetchedAt field)
     *   com.chimera.contentcreator.service.ContentCreatorService.validateFreshness()
     *   com.chimera.contentcreator.exception.StaleTrendReportException
     */
    @Test
    @DisplayName("[FR-004] Stale TrendReport input throws StaleTrendReportException, no bundle generated")
    void staleTrendReport_atCreateInput_throwsExceptionAndRequestsFreshReport() {
        fail("RED PHASE [FR-004]: ContentCreatorService must throw StaleTrendReportException " +
             "when TrendReportDto.fetchedAt() is more than 24 hours old. " +
             "No ContentBundle must be generated or returned in this path. " +
             "The exception message must include the stale fetchedAt timestamp so the caller " +
             "can log evidence and request a fresh TrendReport from PERCEIVE. " +
             "Wire: ContentCreatorService.validateFreshness() — already scaffolded, needs test coverage.");
    }
}

package com.chimera.perceive;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * RED PHASE — TDD contract tests for PERCEIVE (trend-watcher).
 *
 * These tests define the contract mandated by spec.md before integration is wired.
 * Every test MUST remain failing until the corresponding production behaviour is
 * implemented and verified green.
 *
 * Spec reference : specs/001-chimera-pipeline/spec.md
 * Constitution   : .specify/memory/constitution.md (Principle III — No Silent Failures)
 * Agent rules    : .chimera/agent.rules.md (FORBIDDEN-008)
 */
@DisplayName("PERCEIVE — TrendReport Validation Contracts")
class TrendReportValidationTest {

    // -------------------------------------------------------------------------
    // FR-003 — Stale data guard
    // -------------------------------------------------------------------------

    /**
     * Contract: TrendWatcherService (or its staleness validator) MUST throw
     * StaleTrendReportException when it receives or attempts to forward a
     * TrendReport whose fetchedAt timestamp is older than 24 hours.
     *
     * Spec text (FR-003):
     *   "PERCEIVE MUST discard and regenerate any TrendReport whose fetched data
     *    is older than 24 hours; discards MUST be logged with fetch timestamp
     *    and reason."
     *
     * Agent rule: FORBIDDEN-008 — Using a TrendReport older than 24 hours as
     * CREATE input is explicitly forbidden.
     *
     * Implementation wire-up required:
     *   com.chimera.trendwatcher.service.TrendWatcherService
     *   com.chimera.trendwatcher.exception.StaleTrendReportException
     */
    @Test
    @DisplayName("[FR-003] TrendReport older than 24h throws StaleTrendReportException")
    void staleTrendReport_olderThan24Hours_throwsStaleTrendReportException() {
        fail("RED PHASE [FR-003]: TrendWatcherService must throw StaleTrendReportException " +
             "when TrendReport.fetchedAt is more than 24 hours in the past. " +
             "Discard must be logged at WARN with fetchedAt timestamp and reason. " +
             "Wire: com.chimera.trendwatcher.service.TrendWatcherService staleness check.");
    }

    // -------------------------------------------------------------------------
    // FR-001 — Null fetchedAt guard
    // -------------------------------------------------------------------------

    /**
     * Contract: A TrendReport with a null fetchedAt field MUST be rejected before
     * it can reach the CREATE stage.  A null timestamp cannot be validated for
     * staleness; accepting it would violate the 24-hour freshness guarantee.
     *
     * Spec text (FR-001):
     *   "The system MUST execute PERCEIVE → CREATE → ACT → LEARN pipeline cycles
     *    continuously and autonomously."
     *   Implicit: pipeline inputs without mandatory timestamps are invalid inputs.
     *
     * Expected behaviour: throw IllegalArgumentException (or a dedicated
     * InvalidTrendReportException) with a message identifying the missing field.
     *
     * Implementation wire-up required:
     *   com.chimera.trendwatcher.model.TrendReport (record — fetchedAt is non-null)
     *   com.chimera.trendwatcher.service.TrendWatcherService input validation
     */
    @Test
    @DisplayName("[FR-001] TrendReport with null fetchedAt is rejected before forwarding to CREATE")
    void trendReport_withNullFetchedAt_isRejected() {
        fail("RED PHASE [FR-001]: TrendWatcherService must reject a TrendReport whose " +
             "fetchedAt is null with IllegalArgumentException (or InvalidTrendReportException). " +
             "A null timestamp makes staleness verification impossible; the report must never " +
             "reach CREATE. Wire: TrendWatcherService input validation guard.");
    }

    // -------------------------------------------------------------------------
    // FR-002 — Content safety filter + WARN logging
    // -------------------------------------------------------------------------

    /**
     * Contract: Any content item that fails ContentSafetyFilter.passes() MUST be
     * discarded, MUST NOT appear in the resulting TrendReport, and MUST be logged
     * at WARN level.
     *
     * Spec text (FR-002 / FR-004):
     *   "PERCEIVE MUST fetch trending content from TikTok, Instagram, and X."
     *   "PERCEIVE MUST pass all content through a safety filter before forwarding
     *    to CREATE; items failing the filter MUST be discarded and the discard
     *    logged with item identifier and filter reason."
     *
     * Agent rule: SEC-003 — ContentSafetyFilter runs in BOTH PERCEIVE and CREATE.
     *
     * Implementation wire-up required:
     *   com.chimera.trendwatcher.filter.ContentSafetyFilter (already exists)
     *   com.chimera.trendwatcher.service.TrendWatcherService — must call filter
     *   per topic and log at WARN on discard via SLF4J Logger
     */
    @Test
    @DisplayName("[FR-002] Harmful content is filtered by ContentSafetyFilter and logged at WARN")
    void harmfulContent_failsSafetyFilter_isDiscardedAndLoggedAtWarn() {
        fail("RED PHASE [FR-002]: TrendWatcherService must invoke ContentSafetyFilter.passes() " +
             "for every TrendTopic text before including it in the TrendReport. " +
             "Topics that fail must be discarded (excluded from TrendReport.topics()) and " +
             "a WARN-level log entry must be emitted with the item identifier and the " +
             "blocked keyword. Wire: TrendWatcherService filter loop + logger.warn(...).");
    }
}

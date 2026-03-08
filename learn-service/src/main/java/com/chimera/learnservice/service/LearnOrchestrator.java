package com.chimera.learnservice.service;

import com.chimera.actservice.model.AudienceInteraction;
import com.chimera.actservice.model.PostResult;
import com.chimera.actservice.model.Reply;
import com.chimera.learnservice.connector.FeedbackDispatcher;
import com.chimera.learnservice.impl.ChimeraLearnService;
import com.chimera.learnservice.model.FeedbackReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core orchestrator of the LEARN service. Closes the autonomous feedback loop.
 *
 * Responsibilities:
 * 1. Accept signals from ACT via {@link ChimeraLearnService} (implements LearnService)
 * 2. Trigger report generation on a scheduled cadence and after N signals
 * 3. Dispatch reports to CREATE and PERCEIVE via {@link FeedbackDispatcher}
 *
 * Virtual threads:
 * - Report generation runs on a background virtual thread (scheduled cadence)
 * - Dispatch retry loop runs on its own virtual thread (inside FeedbackDispatcher)
 * - Individual signal ingestion runs on the caller's thread (ACT's virtual thread)
 *
 * Spec rules enforced:
 * - Track which contents perform best per platform        → PerformanceMemory
 * - Store all historical performance                      → PerformanceMemory.history
 * - Negative performance flagged and reported back        → LearnAlertService + FeedbackReport
 * - Bad/corrupt signal → flag and skip                   → SignalValidator
 * - Connection fails → queue and retry                   → FeedbackDispatcher
 */
public class LearnOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(LearnOrchestrator.class);

    /** Generate and dispatch a feedback report after this many signals. */
    private static final int    REPORT_EVERY_N_SIGNALS = 10;

    /** Also generate a report on this cadence regardless of signal count. */
    private static final Duration REPORT_CADENCE = Duration.ofMinutes(5);

    private final ChimeraLearnService learnService;
    private final ReportBuilder       reportBuilder;
    private final FeedbackDispatcher  dispatcher;

    private final AtomicInteger signalCount = new AtomicInteger(0);
    private final AtomicBoolean running     = new AtomicBoolean(false);

    public LearnOrchestrator(ChimeraLearnService learnService,
                              ReportBuilder reportBuilder,
                              FeedbackDispatcher dispatcher) {
        this.learnService  = learnService;
        this.reportBuilder = reportBuilder;
        this.dispatcher    = dispatcher;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void start() {
        if (running.compareAndSet(false, true)) {
            dispatcher.start();
            startReportCadenceLoop();
            log.info("=== LearnOrchestrator started — loop CLOSED ===");
            log.info("    Reporting: every {} signals or every {}",
                    REPORT_EVERY_N_SIGNALS, REPORT_CADENCE);
        }
    }

    public void stop() {
        running.set(false);
        dispatcher.stop();
    }

    // -------------------------------------------------------------------------
    // Signal entry points (called by ACT via ChimeraLearnService)
    // -------------------------------------------------------------------------

    /**
     * Called when ACT submits an interaction + reply pair.
     * Triggers a report after every REPORT_EVERY_N_SIGNALS signals.
     */
    public void onInteractionData(AudienceInteraction interaction, Reply reply) {
        learnService.submitInteractionData(interaction, reply);
        maybeGenerateReport();
    }

    /**
     * Called when ACT submits post-level engagement metrics.
     * Triggers a report after every REPORT_EVERY_N_SIGNALS signals.
     */
    public void onEngagementMetrics(PostResult result, List<AudienceInteraction> interactions) {
        learnService.submitEngagementMetrics(result, interactions);
        maybeGenerateReport();
    }

    // -------------------------------------------------------------------------
    // Report generation
    // -------------------------------------------------------------------------

    private void maybeGenerateReport() {
        if (signalCount.incrementAndGet() % REPORT_EVERY_N_SIGNALS == 0) {
            generateAndDispatch("signal-threshold");
        }
    }

    /** Generates a report and dispatches it to CREATE and PERCEIVE. */
    public void generateAndDispatch(String trigger) {
        log.info("LEARN: generating feedback report (trigger={})", trigger);
        FeedbackReport report = reportBuilder.build();
        dispatcher.dispatch(report);
    }

    // -------------------------------------------------------------------------
    // Scheduled cadence loop — runs on a virtual thread
    // -------------------------------------------------------------------------

    private void startReportCadenceLoop() {
        Thread.ofVirtual()
                .name("learn-report-cadence")
                .start(() -> {
                    while (running.get() && !Thread.currentThread().isInterrupted()) {
                        try {
                            Thread.sleep(REPORT_CADENCE);
                            generateAndDispatch("cadence");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                });
    }

    // -------------------------------------------------------------------------
    // Accessors for wiring into CREATE and PERCEIVE
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link ChimeraLearnService} for injection into the ACT service.
     * ACT replaces {@code LearnService.noOp()} with this instance.
     */
    public ChimeraLearnService asLearnService() {
        return learnService;
    }
}

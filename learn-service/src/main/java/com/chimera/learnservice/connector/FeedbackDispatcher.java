package com.chimera.learnservice.connector;

import com.chimera.learnservice.alert.LearnAlertService;
import com.chimera.learnservice.feedback.PerceiveFeedbackAdapter;
import com.chimera.learnservice.model.FeedbackReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dispatches {@link FeedbackReport} instances to downstream consumers
 * (CREATE via {@link com.chimera.learnservice.feedback.CreateFeedbackAdapter} and
 * PERCEIVE via {@link PerceiveFeedbackAdapter}) with automatic retry on failure.
 *
 * Rule: if the connection fails to PERCEIVE and CREATE → queue feedback and retry.
 *
 * Retry strategy:
 * - Failed reports are queued in {@code retryQueue}
 * - A background virtual thread retries every {@value RETRY_INTERVAL_SECONDS} seconds
 * - After {@value MAX_DISPATCH_RETRIES} consecutive failures → human alert dispatched
 */
public class FeedbackDispatcher {

    private static final Logger log = LoggerFactory.getLogger(FeedbackDispatcher.class);

    private static final int  MAX_QUEUE_SIZE          = 500;
    private static final int  RETRY_INTERVAL_SECONDS  = 30;
    private static final int  MAX_DISPATCH_RETRIES    = 5;

    private final PerceiveFeedbackAdapter perceiveFeedback;
    private final LearnAlertService       alertService;
    private final LinkedBlockingQueue<FeedbackReport> retryQueue =
            new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private final AtomicBoolean running = new AtomicBoolean(false);

    public FeedbackDispatcher(PerceiveFeedbackAdapter perceiveFeedback,
                               LearnAlertService alertService) {
        this.perceiveFeedback = perceiveFeedback;
        this.alertService     = alertService;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void start() {
        if (running.compareAndSet(false, true)) {
            Thread.ofVirtual()
                    .name("feedback-retry-loop")
                    .start(this::retryLoop);
            log.info("FeedbackDispatcher started — retry interval={}s", RETRY_INTERVAL_SECONDS);
        }
    }

    public void stop() {
        running.set(false);
    }

    // -------------------------------------------------------------------------
    // Dispatch
    // -------------------------------------------------------------------------

    /**
     * Attempts to dispatch a FeedbackReport immediately.
     * On failure, queues the report for retry.
     *
     * Rule: if connection fails → queue and retry.
     */
    public void dispatch(FeedbackReport report) {
        try {
            deliverToConsumers(report);
            log.info("DISPATCH: FeedbackReport id={} delivered successfully", report.id());
        } catch (Exception e) {
            log.warn("DISPATCH: failed to deliver report id={} — queuing for retry: {}",
                    report.id(), e.getMessage());
            boolean queued = retryQueue.offer(report);
            if (!queued) {
                log.error("DISPATCH: retry queue full — FeedbackReport id={} dropped", report.id());
                alertService.alertDispatchFailure("CREATE/PERCEIVE", MAX_DISPATCH_RETRIES,
                        "Retry queue full: " + e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Retry loop — runs on a virtual thread
    // -------------------------------------------------------------------------

    private void retryLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                FeedbackReport report = retryQueue.poll(RETRY_INTERVAL_SECONDS, TimeUnit.SECONDS);
                if (report != null) {
                    retryDispatch(report);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void retryDispatch(FeedbackReport report) {
        for (int attempt = 1; attempt <= MAX_DISPATCH_RETRIES; attempt++) {
            try {
                deliverToConsumers(report);
                log.info("DISPATCH_RETRY: FeedbackReport id={} delivered on attempt {}",
                        report.id(), attempt);
                return;
            } catch (Exception e) {
                log.warn("DISPATCH_RETRY: attempt {}/{} failed for report id={}: {}",
                        attempt, MAX_DISPATCH_RETRIES, report.id(), e.getMessage());
            }
        }
        // All retries exhausted
        alertService.alertDispatchFailure("CREATE/PERCEIVE", MAX_DISPATCH_RETRIES,
                "All retries exhausted for FeedbackReport id=" + report.id());
    }

    // -------------------------------------------------------------------------
    // Delivery
    // -------------------------------------------------------------------------

    /**
     * Delivers the feedback report to all downstream consumers.
     *
     * CREATE: the {@link com.chimera.learnservice.feedback.CreateFeedbackAdapter} is a
     *   live pull-based adapter — CREATE reads from PerformanceMemory on demand,
     *   so no push is needed. The delivery here logs the state for observability.
     *
     * PERCEIVE: logs the current high/low performing topic sets so PERCEIVE can
     *   query them on its next cycle. In a distributed setup this would be an HTTP call.
     */
    private void deliverToConsumers(FeedbackReport report) {
        // PERCEIVE feedback — log current topic intelligence
        perceiveFeedback.logState();

        // CREATE feedback — already live via CreateFeedbackAdapter (pull model)
        log.info("DISPATCH: CREATE adapter updated — overallScore={} underperforming={} topPerforming={}",
                String.format("%.3f", report.overallEngagementScore()),
                report.underperformingTopics(),
                report.topPerformingTopics());

        // In a distributed deployment: POST report to CREATE/PERCEIVE HTTP endpoints here
    }
}

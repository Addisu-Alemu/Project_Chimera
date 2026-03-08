package com.chimera.learnservice.alert;

import com.chimera.learnservice.model.ContentPerformance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatches urgent alerts from the LEARN service to human operators.
 *
 * Alert triggers:
 * - Corrupt or bad signal received — human must investigate the ACT→LEARN data pipeline
 * - Negative performance detected — creative strategy needs human review
 * - Feedback dispatch failure after exhausting retries — human must check CREATE/PERCEIVE connections
 */
public class LearnAlertService {

    private static final Logger log = LoggerFactory.getLogger(LearnAlertService.class);
    private static final String ALERT = "🚨 HUMAN ALERT [LEARN]";

    /**
     * Rule: if signal is bad or corrupt → flag to human intervention and skip.
     */
    public void alertCorruptSignal(String signalId, String reason) {
        log.error("{} [CORRUPT_SIGNAL] signalId={} reason='{}' — signal discarded, pipeline investigation required",
                ALERT, signalId, reason);
        // TODO: route to PagerDuty / Slack
    }

    /**
     * Rule: negative performance must be flagged and reported back.
     */
    public void alertNegativePerformance(ContentPerformance performance) {
        log.error("{} [NEGATIVE_PERFORMANCE] contentPieceId={} topic='{}' platform={} score={}" +
                        " — content strategy review required",
                ALERT,
                performance.contentPieceId(),
                performance.topic(),
                performance.platform(),
                String.format("%.3f", performance.engagementScore()));
        // TODO: notify content strategy team
    }

    /**
     * Rule: if connection to PERCEIVE/CREATE fails → alert after retries exhausted.
     */
    public void alertDispatchFailure(String target, int attempts, String reason) {
        log.error("{} [DISPATCH_FAILURE] target={} failedAfter={} attempts reason='{}'" +
                        " — feedback loop broken, manual reconnection required",
                ALERT, target, attempts, reason);
        // TODO: trigger infrastructure alert
    }
}

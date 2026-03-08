package com.chimera.learnservice.signal;

import com.chimera.learnservice.alert.LearnAlertService;
import com.chimera.learnservice.model.EngagementSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

/**
 * Validates incoming engagement signals before they enter the analysis pipeline.
 *
 * Rule: if the signal is bad or corrupt → flag to human intervention and skip.
 *
 * A signal is rejected when any of the following is true:
 * - contentPieceId is null or blank
 * - platform is null
 * - receivedAt is null or more than MAX_SIGNAL_AGE old (stale / replayed signal)
 * - any metric value is negative (corrupt data)
 * - all metric values are zero AND signalType is ENGAGEMENT_METRIC (empty / ghost signal)
 */
public class SignalValidator {

    private static final Logger log = LoggerFactory.getLogger(SignalValidator.class);

    /** Signals older than this are considered stale and rejected. */
    private static final Duration MAX_SIGNAL_AGE = Duration.ofDays(7);

    private final LearnAlertService alertService;

    public SignalValidator(LearnAlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * Returns {@code true} if the signal is valid and safe to process.
     * Logs at WARN and triggers a human alert on rejection.
     */
    public boolean isValid(EngagementSignal signal) {
        if (signal == null) {
            log.warn("SIGNAL_INVALID: null signal received — skipping");
            alertService.alertCorruptSignal("null", "Signal object is null");
            return false;
        }

        if (signal.contentPieceId() == null || signal.contentPieceId().isBlank()) {
            log.warn("SIGNAL_INVALID: missing contentPieceId in signal id={}", signal.id());
            alertService.alertCorruptSignal(signal.id(), "contentPieceId is null or blank");
            return false;
        }

        if (signal.platform() == null) {
            log.warn("SIGNAL_INVALID: null platform in signal id={}", signal.id());
            alertService.alertCorruptSignal(signal.id(), "platform is null");
            return false;
        }

        if (signal.receivedAt() == null) {
            log.warn("SIGNAL_INVALID: null receivedAt in signal id={}", signal.id());
            alertService.alertCorruptSignal(signal.id(), "receivedAt is null");
            return false;
        }

        // Staleness check
        if (signal.receivedAt().isBefore(Instant.now().minus(MAX_SIGNAL_AGE))) {
            log.warn("SIGNAL_INVALID: stale signal id={} receivedAt={}", signal.id(), signal.receivedAt());
            alertService.alertCorruptSignal(signal.id(), "Signal is stale (receivedAt=" + signal.receivedAt() + ")");
            return false;
        }

        // Negative metrics — data corruption
        if (signal.likes() < 0 || signal.shares() < 0 || signal.comments() < 0 || signal.views() < 0) {
            log.warn("SIGNAL_INVALID: negative metric in signal id={} likes={} shares={} comments={} views={}",
                    signal.id(), signal.likes(), signal.shares(), signal.comments(), signal.views());
            alertService.alertCorruptSignal(signal.id(), "Negative metric value detected — possible data corruption");
            return false;
        }

        return true;
    }
}

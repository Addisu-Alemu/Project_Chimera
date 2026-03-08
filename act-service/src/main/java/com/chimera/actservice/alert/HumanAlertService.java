package com.chimera.actservice.alert;

import com.chimera.actservice.model.Transaction;
import com.chimera.contentcreator.model.ContentPiece;
import com.chimera.trendwatcher.model.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Dispatches urgent alerts to human operators when automated handling is insufficient.
 *
 * In production, route alerts through PagerDuty, Slack, email, or SMS.
 * The current implementation logs at ERROR level as the notification channel.
 *
 * Alert triggers:
 * - Post failure after MAX_RETRIES attempts
 * - Platform API confirmed down
 * - Transaction amount above alert threshold
 * - Suspicious / frozen transaction
 * - Interaction queue overflow
 */
public class HumanAlertService {

    private static final Logger log = LoggerFactory.getLogger(HumanAlertService.class);

    /** Prefix used in all alert messages for easy grep / monitoring rule matching. */
    private static final String ALERT_PREFIX = "🚨 HUMAN ALERT";

    /**
     * Rule: if the post fails after MAX_RETRIES → alert human.
     */
    public void alertPostFailure(ContentPiece piece, Platform platform, int attempts) {
        log.error("{} [POST_FAILURE] contentPieceId={} platform={} failedAfter={} attempts — manual intervention required",
                ALERT_PREFIX, piece.id(), platform, attempts);
        // TODO: integrate with PagerDuty / Slack webhook
    }

    /**
     * Rule: if the platform API is down → alert human and log state.
     */
    public void alertPlatformDown(Platform platform) {
        log.error("{} [PLATFORM_DOWN] platform={} — publishing PAUSED, state logged. Will resume on API recovery.",
                ALERT_PREFIX, platform);
        // TODO: integrate with monitoring / on-call system
    }

    /**
     * Rule: alert human for transactions above the configured threshold.
     */
    public void alertHighTransactionAmount(Transaction tx, BigDecimal threshold) {
        log.error("{} [HIGH_VALUE_TX] transactionId={} userId={} amount={} {} exceeds threshold={} — awaiting human approval",
                ALERT_PREFIX, tx.id(), tx.userId(), tx.amount(), tx.currency(), threshold);
        // TODO: send approval request to finance team
    }

    /**
     * Rule: suspicious transaction → freeze and alert human.
     */
    public void alertSuspiciousTransaction(Transaction tx) {
        log.error("{} [SUSPICIOUS_TX] transactionId={} userId={} amount={} {} type={} — FROZEN, human review required",
                ALERT_PREFIX, tx.id(), tx.userId(), tx.amount(), tx.currency(), tx.type());
        // TODO: trigger fraud review workflow
    }

    /**
     * Rule: if interaction volume is too high and queue is overflowing → alert human.
     */
    public void alertQueueOverflow(int droppedCount) {
        log.error("{} [QUEUE_OVERFLOW] {} interactions dropped — consider scaling the interaction processor",
                ALERT_PREFIX, droppedCount);
        // TODO: trigger scaling alert
    }

    /**
     * Alerts human when an audience response deadline is breached.
     */
    public void alertResponseWindowBreached(String interactionId, Platform platform, long delayMinutes) {
        log.warn("{} [RESPONSE_WINDOW_BREACH] interactionId={} platform={} delayMinutes={}",
                ALERT_PREFIX, interactionId, platform, delayMinutes);
    }
}

package com.chimera.actservice.transaction;

import com.chimera.actservice.alert.HumanAlertService;
import com.chimera.actservice.exception.SuspiciousTransactionException;
import com.chimera.actservice.model.Transaction;
import com.chimera.actservice.model.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processes financial transactions with full compliance to spec rules:
 *
 * 1. Log every transaction — {@link TransactionLogger} is called at every status change.
 * 2. Alert human for high-value transactions — amounts above {@value #ALERT_THRESHOLD} USD.
 * 3. Suspicious transaction → freeze and alert — detected by amount or rapid-fire rate.
 *
 * Thread-safe: uses ConcurrentHashMap for per-user rate tracking.
 */
public class TransactionManager {

    private static final Logger log = LoggerFactory.getLogger(TransactionManager.class);

    /** Transactions above this amount trigger an immediate human alert. */
    private static final BigDecimal ALERT_THRESHOLD      = new BigDecimal("1000.00");

    /** Transactions above this amount are considered suspicious and frozen. */
    private static final BigDecimal SUSPICIOUS_THRESHOLD = new BigDecimal("10000.00");

    /** Maximum transactions per user within the rate window before flagging as suspicious. */
    private static final int    MAX_TX_PER_WINDOW  = 5;
    private static final long   RATE_WINDOW_SECONDS = 60;

    private final TransactionLogger  logger;
    private final HumanAlertService  alertService;

    /** Tracks recent transaction timestamps per userId for rate-limit detection. */
    private final ConcurrentHashMap<String, List<Instant>> recentTxByUser = new ConcurrentHashMap<>();

    public TransactionManager(TransactionLogger logger, HumanAlertService alertService) {
        this.logger       = logger;
        this.alertService = alertService;
    }

    /**
     * Processes a transaction through all spec rules.
     *
     * @return The completed (or frozen) transaction with updated status.
     * @throws SuspiciousTransactionException if the transaction is flagged and frozen.
     */
    public Transaction process(Transaction tx) {
        // Rule 1: log every transaction at initiation
        logger.log(tx);

        // Rule 3 (part 1): detect suspicious transaction — freeze before doing anything else
        if (isSuspicious(tx)) {
            Transaction frozen = tx.withStatus(TransactionStatus.FROZEN);
            logger.log(frozen);                         // Rule 1: log the freeze
            alertService.alertSuspiciousTransaction(frozen);  // Rule 3: alert human
            throw new SuspiciousTransactionException(
                    "Transaction id=" + tx.id() + " frozen: suspicious activity detected");
        }

        // Rule 2: alert human for high-value transactions (but still process them)
        if (tx.amount().compareTo(ALERT_THRESHOLD) > 0) {
            alertService.alertHighTransactionAmount(tx, ALERT_THRESHOLD);
            log.warn("HIGH_VALUE_TX: id={} amount={} {} — human notified, processing continues",
                    tx.id(), tx.amount(), tx.currency());
        }

        // Record transaction timestamp for rate limiting
        recordTransactionTime(tx.userId());

        // Complete the transaction
        Transaction completed = tx.withStatus(TransactionStatus.COMPLETED);
        logger.log(completed);  // Rule 1: log the completion
        log.info("TRANSACTION COMPLETED: id={} userId={} type={} amount={} {}",
                completed.id(), completed.userId(), completed.type(),
                completed.amount(), completed.currency());
        return completed;
    }

    // -------------------------------------------------------------------------
    // Suspicious activity detection
    // -------------------------------------------------------------------------

    private boolean isSuspicious(Transaction tx) {
        // Single transaction above suspicious threshold
        if (tx.amount().compareTo(SUSPICIOUS_THRESHOLD) >= 0) {
            log.warn("SUSPICIOUS: tx id={} amount={} exceeds SUSPICIOUS_THRESHOLD={}",
                    tx.id(), tx.amount(), SUSPICIOUS_THRESHOLD);
            return true;
        }
        // Rapid-fire transactions from same user
        if (isRateLimitBreached(tx.userId())) {
            log.warn("SUSPICIOUS: userId={} exceeded {} transactions within {} seconds",
                    tx.userId(), MAX_TX_PER_WINDOW, RATE_WINDOW_SECONDS);
            return true;
        }
        return false;
    }

    private boolean isRateLimitBreached(String userId) {
        List<Instant> timestamps = recentTxByUser.getOrDefault(userId, List.of());
        Instant windowStart = Instant.now().minusSeconds(RATE_WINDOW_SECONDS);
        long recentCount = timestamps.stream().filter(t -> t.isAfter(windowStart)).count();
        return recentCount >= MAX_TX_PER_WINDOW;
    }

    private void recordTransactionTime(String userId) {
        recentTxByUser.compute(userId, (uid, list) -> {
            List<Instant> updated = list == null ? new ArrayList<>() : new ArrayList<>(list);
            updated.add(Instant.now());
            // Prune old entries outside the window
            Instant windowStart = Instant.now().minusSeconds(RATE_WINDOW_SECONDS);
            updated.removeIf(t -> t.isBefore(windowStart));
            return updated;
        });
    }
}

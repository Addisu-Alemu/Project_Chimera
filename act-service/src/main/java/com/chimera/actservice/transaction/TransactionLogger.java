package com.chimera.actservice.transaction;

import com.chimera.actservice.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides an immutable audit trail for every financial transaction.
 *
 * Rule: must log every financial transaction — no exceptions.
 *
 * All entries are written to the dedicated TRANSACTION_AUDIT logger which
 * routes to a separate, long-retention log file (see logback.xml).
 * In production, also persist to a database or append-only event store.
 */
public class TransactionLogger {

    /** Named logger — routes to dedicated transaction appender in logback.xml. */
    private static final Logger audit = LoggerFactory.getLogger("TRANSACTION_AUDIT");

    /**
     * Records a transaction entry. Called at every status transition:
     * PENDING → COMPLETED / FAILED / FROZEN / SUSPICIOUS.
     */
    public void log(Transaction tx) {
        audit.info(
                "id={} userId={} type={} amount={} {} status={} initiatedAt={} description='{}'",
                tx.id(),
                tx.userId(),
                tx.type(),
                tx.amount(),
                tx.currency(),
                tx.status(),
                tx.initiatedAt(),
                tx.description()
        );
    }
}

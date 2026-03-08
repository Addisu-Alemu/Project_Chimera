package com.chimera.actservice.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A financial transaction processed by the ACT service.
 *
 * Rules enforced at the TransactionManager level:
 * - Every transaction is logged (regardless of outcome)
 * - Amounts above the alert threshold trigger a human alert
 * - Suspicious transactions are frozen before any further processing
 *
 * @param id           Unique transaction identifier
 * @param userId       Account owner initiating the transaction
 * @param type         Nature of the transaction
 * @param amount       Monetary value (BigDecimal for precision)
 * @param currency     ISO 4217 currency code (e.g. "USD")
 * @param description  Human-readable description
 * @param initiatedAt  UTC timestamp of transaction initiation
 * @param status       Current lifecycle status
 */
public record Transaction(
        String id,
        String userId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        String description,
        Instant initiatedAt,
        TransactionStatus status
) {
    /** Returns a copy of this transaction with an updated status. */
    public Transaction withStatus(TransactionStatus newStatus) {
        return new Transaction(id, userId, type, amount, currency, description, initiatedAt, newStatus);
    }
}

package com.chimera.actservice.model;

/** Lifecycle status of a financial transaction. */
public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,

    /** Account frozen — suspicious activity detected. Human alert dispatched. */
    FROZEN,

    /** Flagged for manual review — human must clear before processing resumes. */
    SUSPICIOUS
}

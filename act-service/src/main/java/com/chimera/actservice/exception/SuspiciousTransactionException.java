package com.chimera.actservice.exception;

/**
 * Thrown when a transaction is flagged as suspicious.
 *
 * Rule: suspicious transaction → freeze account and alert human immediately.
 *
 * The transaction is frozen in the log before this exception is thrown.
 */
public class SuspiciousTransactionException extends RuntimeException {

    public SuspiciousTransactionException(String message) {
        super(message);
    }
}

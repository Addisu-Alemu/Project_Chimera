package com.chimera.actservice.exception;

/**
 * Thrown when a single publish attempt fails.
 * The orchestrator retries up to MAX_RETRIES times before alerting a human.
 */
public class PublishException extends Exception {

    public PublishException(String message) {
        super(message);
    }

    public PublishException(String message, Throwable cause) {
        super(message, cause);
    }
}

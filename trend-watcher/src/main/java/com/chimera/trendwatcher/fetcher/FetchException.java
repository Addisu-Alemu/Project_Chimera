package com.chimera.trendwatcher.fetcher;

/**
 * Thrown when both the primary and backup endpoints for a platform fail.
 */
public class FetchException extends Exception {

    public FetchException(String message) {
        super(message);
    }

    public FetchException(String message, Throwable cause) {
        super(message, cause);
    }
}

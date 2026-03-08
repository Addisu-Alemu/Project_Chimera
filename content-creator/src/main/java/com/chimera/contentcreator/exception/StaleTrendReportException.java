package com.chimera.contentcreator.exception;

/**
 * Thrown when the received TrendReport is too old for safe content generation.
 *
 * Rule: if the input is stale, request a fresh TrendReport from PERCEIVE.
 *
 * Callers should catch this exception, trigger a fresh report from the
 * TrendWatcher service, and retry content creation.
 */
public class StaleTrendReportException extends RuntimeException {

    public StaleTrendReportException(String message) {
        super(message);
    }
}

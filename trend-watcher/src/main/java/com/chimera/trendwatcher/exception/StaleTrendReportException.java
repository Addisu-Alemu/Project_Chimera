package com.chimera.trendwatcher.exception;

public class StaleTrendReportException extends RuntimeException {
    public StaleTrendReportException(String reportId) {
        super("TrendReport " + reportId + " is stale (fetchedAt > 24h ago)");
    }
}

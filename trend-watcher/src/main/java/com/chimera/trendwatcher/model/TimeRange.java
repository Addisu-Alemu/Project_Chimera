package com.chimera.trendwatcher.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Analysis window for trend reports.
 * DAILY  = content from the last 24 hours
 * WEEKLY = content from the last 7 days
 */
public enum TimeRange {
    DAILY(Duration.ofHours(24)),
    WEEKLY(Duration.ofDays(7));

    private final Duration window;

    TimeRange(Duration window) {
        this.window = window;
    }

    /** Returns the earliest publishedAt timestamp that qualifies for this range. */
    public Instant cutoff() {
        return Instant.now().minus(window);
    }

    public Duration window() {
        return window;
    }
}

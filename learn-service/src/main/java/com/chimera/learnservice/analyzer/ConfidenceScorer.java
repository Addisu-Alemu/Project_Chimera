package com.chimera.learnservice.analyzer;

import org.springframework.stereotype.Service;

@Service
public class ConfidenceScorer {

    private static final long MAX_SHARES = 10_000;
    private static final long MAX_LIKES = 50_000;
    private static final long MAX_COMMENTS = 5_000;
    private static final long MAX_VIEWS = 500_000;

    public double score(EngagementSummary summary) {
        double normalizedShares   = normalize(summary.shares(),   MAX_SHARES);
        double normalizedLikes    = normalize(summary.likes(),    MAX_LIKES);
        double normalizedComments = normalize(summary.comments(), MAX_COMMENTS);
        double normalizedViews    = normalize(summary.views(),    MAX_VIEWS);
        double ctr                = Math.min(summary.clickThroughRate(), 1.0);

        double score = 0.30 * normalizedShares
                     + 0.25 * normalizedLikes
                     + 0.20 * normalizedComments
                     + 0.15 * normalizedViews
                     + 0.10 * ctr;

        return Math.max(0.0, Math.min(1.0, score));
    }

    private double normalize(long value, long max) {
        if (max <= 0) return 0.0;
        return Math.min((double) value / max, 1.0);
    }
}

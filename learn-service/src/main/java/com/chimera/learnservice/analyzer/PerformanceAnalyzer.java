package com.chimera.learnservice.analyzer;

import com.chimera.learnservice.model.ContentPerformance;
import com.chimera.learnservice.model.PerformanceRating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Computes the engagement score and performance rating for a content piece.
 *
 * Engagement score formula (normalised to [0.0, 1.0]):
 *
 *   raw = shares × 4.0 + likes × 2.0 + comments × 3.0 + views × 0.01
 *   score = min(1.0, raw / 1000.0)
 *
 * Weights reflect real-world value: shares &gt; comments &gt; likes &gt; views.
 *
 * Rating thresholds:
 *   EXCELLENT  ≥ 0.70
 *   GOOD       ≥ 0.50
 *   AVERAGE    ≥ 0.30
 *   POOR       ≥ 0.10
 *   NEGATIVE   &lt;  0.10  ← Rule: must flag and report negative performance
 */
public class PerformanceAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(PerformanceAnalyzer.class);

    private static final double SHARES_WEIGHT   = 4.0;
    private static final double LIKES_WEIGHT    = 2.0;
    private static final double COMMENTS_WEIGHT = 3.0;
    private static final double VIEWS_WEIGHT    = 0.01;
    private static final double NORMALISER      = 1000.0;

    /**
     * Re-computes the engagement score and rating from the current accumulated totals
     * and returns an updated {@link ContentPerformance} snapshot.
     */
    public ContentPerformance analyze(ContentPerformance performance) {
        double raw = (performance.totalShares()   * SHARES_WEIGHT)
                   + (performance.totalLikes()    * LIKES_WEIGHT)
                   + (performance.totalComments() * COMMENTS_WEIGHT)
                   + (performance.totalViews()    * VIEWS_WEIGHT);

        double score  = Math.min(1.0, raw / NORMALISER);
        PerformanceRating rating = rate(score);

        // Rule: negative performance must be flagged
        if (rating == PerformanceRating.NEGATIVE) {
            log.warn("NEGATIVE_PERFORMANCE: contentPieceId={} topic='{}' platform={} score={}",
                    performance.contentPieceId(), performance.topic(), performance.platform(),
                    String.format("%.3f", score));
        }

        return performance.withScoreAndRating(score, rating);
    }

    private PerformanceRating rate(double score) {
        if (score >= 0.70) return PerformanceRating.EXCELLENT;
        if (score >= 0.50) return PerformanceRating.GOOD;
        if (score >= 0.30) return PerformanceRating.AVERAGE;
        if (score >= 0.10) return PerformanceRating.POOR;
        return PerformanceRating.NEGATIVE;
    }
}

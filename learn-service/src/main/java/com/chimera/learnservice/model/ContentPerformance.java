package com.chimera.learnservice.model;

import com.chimera.contentcreator.model.ContentType;
import com.chimera.trendwatcher.model.Platform;

import java.time.Instant;

/**
 * Accumulated performance record for a single content piece on a single platform.
 *
 * Immutable snapshot — updated via {@link #merge(EngagementSignal)} which returns
 * a new instance with accumulated metric totals.
 *
 * Rule: must store all historical performance.
 * Every version of this record is persisted to the PERFORMANCE_AUDIT log.
 *
 * @param contentPieceId   ID of the ContentPiece (traceability back to CREATE)
 * @param topic            Trending topic this content addressed
 * @param platform         Platform where this performance was measured
 * @param contentType      Format of the content
 * @param totalLikes       Cumulative likes / reactions
 * @param totalShares      Cumulative shares / reposts
 * @param totalComments    Cumulative comments / replies
 * @param totalViews       Cumulative views / impressions
 * @param engagementScore  Normalised score [0.0 – 1.0], re-computed after each merge
 * @param rating           Categorical rating derived from engagementScore
 * @param signalCount      Number of signals that contributed to this record
 * @param firstRecordedAt  Timestamp of the first signal received
 * @param lastUpdatedAt    Timestamp of the most recent signal
 */
public record ContentPerformance(
        String contentPieceId,
        String topic,
        Platform platform,
        ContentType contentType,
        long totalLikes,
        long totalShares,
        long totalComments,
        long totalViews,
        double engagementScore,
        PerformanceRating rating,
        int signalCount,
        Instant firstRecordedAt,
        Instant lastUpdatedAt
) {
    /**
     * Factory — creates the first performance record from an initial signal.
     * Score and rating are set to defaults; caller must invoke the analyzer to populate them.
     */
    public static ContentPerformance fromSignal(EngagementSignal signal) {
        return new ContentPerformance(
                signal.contentPieceId(),
                signal.topic(),
                signal.platform(),
                signal.contentType(),
                signal.likes(),
                signal.shares(),
                signal.comments(),
                signal.views(),
                0.0,
                PerformanceRating.AVERAGE,
                1,
                signal.receivedAt(),
                signal.receivedAt()
        );
    }

    /**
     * Returns a new instance with the signal's metrics added to the running totals.
     * The caller is responsible for re-computing score and rating after merging.
     */
    public ContentPerformance merge(EngagementSignal signal) {
        return new ContentPerformance(
                contentPieceId,
                topic,
                platform,
                contentType,
                totalLikes    + signal.likes(),
                totalShares   + signal.shares(),
                totalComments + signal.comments(),
                totalViews    + signal.views(),
                engagementScore,   // re-computed by PerformanceAnalyzer after merge
                rating,
                signalCount + 1,
                firstRecordedAt,
                signal.receivedAt()
        );
    }

    /** Returns a copy with updated score and rating after analysis. */
    public ContentPerformance withScoreAndRating(double score, PerformanceRating newRating) {
        return new ContentPerformance(
                contentPieceId, topic, platform, contentType,
                totalLikes, totalShares, totalComments, totalViews,
                score, newRating, signalCount, firstRecordedAt, lastUpdatedAt
        );
    }

    public boolean isNegative() {
        return rating == PerformanceRating.NEGATIVE;
    }

    public boolean isPoorOrNegative() {
        return rating == PerformanceRating.POOR || rating == PerformanceRating.NEGATIVE;
    }
}

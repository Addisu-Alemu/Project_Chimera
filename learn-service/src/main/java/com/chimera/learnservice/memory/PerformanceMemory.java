package com.chimera.learnservice.memory;

import com.chimera.contentcreator.model.ContentType;
import com.chimera.learnservice.model.ContentPerformance;
import com.chimera.learnservice.model.PerformanceRating;
import com.chimera.trendwatcher.model.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Thread-safe, in-memory store for all content performance data.
 *
 * Rules enforced:
 * - Must track which contents perform best per platform.
 * - Must store all historical performance — every update appended to history.
 * - Negative performance is surfaced via {@link #getNegativePerformers()}.
 *
 * Production note: back this with a time-series database (InfluxDB, TimescaleDB)
 * or an append-only event store to survive restarts.
 */
public class PerformanceMemory {

    private static final Logger log       = LoggerFactory.getLogger(PerformanceMemory.class);
    private static final Logger auditLog  = LoggerFactory.getLogger("PERFORMANCE_AUDIT");

    /** Latest accumulated performance per (contentPieceId + ":" + platform). */
    private final ConcurrentHashMap<String, ContentPerformance> latest = new ConcurrentHashMap<>();

    /** Full history — every snapshot ever recorded, in insertion order. Rule: store all historical. */
    private final CopyOnWriteArrayList<ContentPerformance> history = new CopyOnWriteArrayList<>();

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    /**
     * Records a performance snapshot.
     * Replaces the latest entry for the (contentPieceId, platform) key
     * and appends to the immutable history list.
     *
     * Rule: store all historical performance.
     */
    public void record(ContentPerformance performance) {
        String key = performance.contentPieceId() + ":" + performance.platform();
        latest.put(key, performance);
        history.add(performance);  // Never removed — full audit trail

        auditLog.info("contentPieceId={} topic='{}' platform={} type={} score={} rating={}" +
                        " likes={} shares={} comments={} views={} signals={}",
                performance.contentPieceId(), performance.topic(), performance.platform(),
                performance.contentType(), String.format("%.3f", performance.engagementScore()), performance.rating(),
                performance.totalLikes(), performance.totalShares(),
                performance.totalComments(), performance.totalViews(), performance.signalCount());
    }

    /**
     * Returns the latest accumulated performance for a (contentPieceId, platform) pair,
     * or {@code null} if no data exists yet.
     */
    public ContentPerformance getLatest(String contentPieceId, Platform platform) {
        return latest.get(contentPieceId + ":" + platform);
    }

    // -------------------------------------------------------------------------
    // Read — per-platform ranking
    // -------------------------------------------------------------------------

    /**
     * Rule: must track which contents perform best per platform.
     * Returns the top {@code limit} performers for the given platform, sorted by score descending.
     */
    public List<ContentPerformance> getTopPerformersByPlatform(Platform platform, int limit) {
        return latest.values().stream()
                .filter(p -> p.platform() == platform)
                .sorted(Comparator.comparingDouble(ContentPerformance::engagementScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /** Returns top performers for ALL platforms (top {@code limit} per platform). */
    public Map<Platform, List<ContentPerformance>> getTopPerformersByAllPlatforms(int limitPerPlatform) {
        Map<Platform, List<ContentPerformance>> result = new EnumMap<>(Platform.class);
        for (Platform p : Platform.values()) {
            List<ContentPerformance> top = getTopPerformersByPlatform(p, limitPerPlatform);
            if (!top.isEmpty()) result.put(p, top);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Read — topic rankings
    // -------------------------------------------------------------------------

    /** Returns distinct topics that have at least one EXCELLENT or GOOD rated piece. */
    public List<String> getTopPerformingTopics() {
        return latest.values().stream()
                .filter(p -> p.rating() == PerformanceRating.EXCELLENT || p.rating() == PerformanceRating.GOOD)
                .map(ContentPerformance::topic)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Rule: negative performance must be flagged.
     * Returns distinct topics that have at least one POOR or NEGATIVE rated piece.
     */
    public List<String> getUnderperformingTopics() {
        return latest.values().stream()
                .filter(ContentPerformance::isPoorOrNegative)
                .map(ContentPerformance::topic)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /** Returns all content pieces with a NEGATIVE rating. */
    public List<ContentPerformance> getNegativePerformers() {
        return latest.values().stream()
                .filter(ContentPerformance::isNegative)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Read — aggregate metrics for FeedbackData
    // -------------------------------------------------------------------------

    /** Rolling average engagement score across all tracked content. */
    public double getOverallEngagementScore() {
        return latest.values().stream()
                .mapToDouble(ContentPerformance::engagementScore)
                .average()
                .orElse(0.7); // default neutral when no data yet
    }

    /** Average engagement score per ContentType — informs CREATE's GenerationParameters. */
    public Map<ContentType, Double> getEngagementByContentType() {
        return latest.values().stream()
                .filter(p -> p.contentType() != null)
                .collect(Collectors.groupingBy(
                        ContentPerformance::contentType,
                        Collectors.averagingDouble(ContentPerformance::engagementScore)
                ));
    }

    // -------------------------------------------------------------------------
    // Read — full history
    // -------------------------------------------------------------------------

    /** Returns an unmodifiable snapshot of the complete performance history. */
    public List<ContentPerformance> getHistory() {
        return List.copyOf(history);
    }

    public int totalTracked() {
        return latest.size();
    }

    public int totalHistory() {
        return history.size();
    }
}

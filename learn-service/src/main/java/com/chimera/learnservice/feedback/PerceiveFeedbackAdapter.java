package com.chimera.learnservice.feedback;

import com.chimera.learnservice.memory.PerformanceMemory;
import com.chimera.learnservice.model.ContentPerformance;
import com.chimera.trendwatcher.model.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LEARN → PERCEIVE connection.
 *
 * Exposes high-performing topic intelligence back to the PERCEIVE (TrendWatcher) service
 * so that future trend reports can weight topics that historically drove real engagement.
 *
 * PERCEIVE integration path (future iteration):
 * - Inject this adapter into {@code TrendWatcherService} via a {@code PerceivedFeedback} parameter
 * - {@code TrendAggregator} calls {@link #getHighPerformingTopics()} to boost topic rankings
 * - Topics in {@link #getUnderperformingTopics()} are de-weighted or excluded from the report
 *
 * Until PERCEIVE is updated to consume this, the adapter is queryable directly
 * and its state is logged so the data is observable.
 */
public class PerceiveFeedbackAdapter {

    private static final Logger log = LoggerFactory.getLogger(PerceiveFeedbackAdapter.class);

    private final PerformanceMemory memory;

    public PerceiveFeedbackAdapter(PerformanceMemory memory) {
        this.memory = memory;
    }

    /**
     * Topics with EXCELLENT or GOOD performance — PERCEIVE should prioritise these
     * when filtering and ranking trending topics in future reports.
     */
    public List<String> getHighPerformingTopics() {
        return memory.getTopPerformingTopics();
    }

    /**
     * Topics with POOR or NEGATIVE performance — PERCEIVE should de-weight or skip these
     * even if they are technically trending, avoiding content that the audience rejects.
     */
    public List<String> getUnderperformingTopics() {
        return memory.getUnderperformingTopics();
    }

    /**
     * Best-performing content per platform — PERCEIVE can use the platform distribution
     * of high-performing content to adjust which platforms to prioritise in fetching.
     */
    public Map<Platform, List<String>> getBestTopicsByPlatform() {
        Map<Platform, List<ContentPerformance>> byPlatform =
                memory.getTopPerformersByAllPlatforms(10);

        return byPlatform.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(ContentPerformance::topic)
                                .distinct()
                                .collect(Collectors.toList())
                ));
    }

    /** Logs the current PERCEIVE feedback state for observability. */
    public void logState() {
        log.info("PERCEIVE_FEEDBACK: highPerforming={} underperforming={}",
                getHighPerformingTopics(), getUnderperformingTopics());
        getBestTopicsByPlatform().forEach((platform, topics) ->
                log.info("PERCEIVE_FEEDBACK: platform={} topTopics={}", platform, topics));
    }
}

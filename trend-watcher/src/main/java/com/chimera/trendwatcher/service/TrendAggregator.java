package com.chimera.trendwatcher.service;

import com.chimera.trendwatcher.cache.TrendSignalStore;
import com.chimera.trendwatcher.model.Platform;
import com.chimera.trendwatcher.model.TrendReport;
import com.chimera.trendwatcher.model.TrendSignal;
import com.chimera.trendwatcher.model.TrendTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TrendAggregator {

    private static final Logger log = LoggerFactory.getLogger(TrendAggregator.class);

    private final TrendSignalStore trendSignalStore;

    public TrendAggregator(TrendSignalStore trendSignalStore) {
        this.trendSignalStore = trendSignalStore;
    }

    public TrendReport aggregate(UUID agentId, List<Platform> platforms, List<TrendTopic> filteredTopics) {
        log.info("Aggregating {} filtered topics for agent={}", filteredTopics.size(), agentId);

        TrendSignal signal = trendSignalStore.getLatest(agentId);
        List<TrendTopic> weighted = applySignalWeights(filteredTopics, signal);

        Map<String, Double> categoryWeights = weighted.stream()
                .collect(Collectors.toMap(
                        TrendTopic::name,
                        TrendTopic::engagementScore,
                        Double::max
                ));

        TrendReport report = new TrendReport(
                UUID.randomUUID(),
                agentId,
                Instant.now(),
                platforms,
                weighted,
                categoryWeights
        );

        log.info("TrendReport built: id={} topics={}", report.id(), weighted.size());
        return report;
    }

    private List<TrendTopic> applySignalWeights(List<TrendTopic> topics, TrendSignal signal) {
        if (signal == null || signal.categoryWeights() == null) {
            return topics;
        }

        Map<String, Double> weights = signal.categoryWeights();
        List<TrendTopic> reweighted = topics.stream()
                .map(t -> {
                    double multiplier = weights.getOrDefault(t.name(), 1.0);
                    double newScore = Math.min(t.engagementScore() * multiplier, 1.0);
                    return new TrendTopic(t.name(), t.hashtags(), newScore, t.safetyPassed());
                })
                .sorted(Comparator.comparingDouble(TrendTopic::engagementScore).reversed())
                .limit(20)
                .collect(Collectors.toList());

        log.info("Applied TrendSignal weights from agentId={}: re-ranked {} topics", signal.agentId(), reweighted.size());
        return reweighted;
    }
}

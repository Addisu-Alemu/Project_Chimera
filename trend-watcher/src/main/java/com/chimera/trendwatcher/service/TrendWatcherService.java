package com.chimera.trendwatcher.service;

import com.chimera.trendwatcher.fetcher.SocialMediaFetcher;
import com.chimera.trendwatcher.filter.ContentSafetyFilter;
import com.chimera.trendwatcher.model.Platform;
import com.chimera.trendwatcher.model.TrendReport;
import com.chimera.trendwatcher.model.TrendTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class TrendWatcherService {

    private static final Logger log = LoggerFactory.getLogger(TrendWatcherService.class);

    private final List<SocialMediaFetcher> fetchers;
    private final ContentSafetyFilter safetyFilter;
    private final TrendAggregator aggregator;

    public TrendWatcherService(List<SocialMediaFetcher> fetchers,
                                ContentSafetyFilter safetyFilter,
                                TrendAggregator aggregator) {
        this.fetchers = List.copyOf(fetchers);
        this.safetyFilter = safetyFilter;
        this.aggregator = aggregator;
    }

    public TrendReport generateReport(UUID agentId) {
        log.info("Generating trend report for agent={}", agentId);

        List<TrendTopic> allTopics = fetchAllPlatformsConcurrently(agentId);
        List<TrendTopic> safe = allTopics.stream()
                .filter(t -> safetyFilter.passes(t.name()))
                .toList();

        log.info("Safety filter: {}/{} topics passed", safe.size(), allTopics.size());

        List<Platform> platforms = fetchers.stream().map(SocialMediaFetcher::platform).toList();
        return aggregator.aggregate(agentId, platforms, safe);
    }

    private List<TrendTopic> fetchAllPlatformsConcurrently(UUID agentId) {
        List<TrendTopic> combined = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<TrendTopic>>> futures = fetchers.stream()
                    .map(fetcher -> executor.submit(() -> fetcher.fetch(agentId)))
                    .toList();

            for (Future<List<TrendTopic>> future : futures) {
                try {
                    combined.addAll(future.get());
                } catch (ExecutionException e) {
                    log.error("Platform fetch failed: {}", e.getCause().getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Fetch interrupted", e);
                }
            }
        }

        log.info("Fetched {} topics from {} platforms", combined.size(), fetchers.size());
        return combined;
    }
}

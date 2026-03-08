package com.chimera.trendwatcher.service;

import com.chimera.trendwatcher.model.RawContent;
import com.chimera.trendwatcher.model.TimeRange;
import com.chimera.trendwatcher.model.TopCategory;
import com.chimera.trendwatcher.model.TrendReport;
import com.chimera.trendwatcher.model.TrendingTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates validated {@link RawContent} records into a {@link TrendReport}.
 *
 * Trending topics are derived from hashtags, ranked by aggregate share count.
 * Top categories are ranked by aggregate view count.
 */
public class TrendAggregator {

    private static final Logger log = LoggerFactory.getLogger(TrendAggregator.class);

    private static final int TOP_TOPICS     = 20;
    private static final int TOP_CATEGORIES = 10;

    /**
     * Produces a {@link TrendReport} from a validated, filtered content list.
     */
    public TrendReport aggregate(List<RawContent> content, TimeRange range) {
        log.info("Aggregating {} validated content items for range={}", content.size(), range);

        List<TrendingTopic> topics      = buildTrendingTopics(content);
        List<TopCategory>   categories  = buildTopCategories(content);

        TrendReport report = new TrendReport(topics, categories, range, Instant.now());
        log.info("Report ready: {} trending topics, {} top categories", topics.size(), categories.size());
        return report;
    }

    // ---------------------------------------------------------------------------
    // Private — topic aggregation
    // ---------------------------------------------------------------------------

    private List<TrendingTopic> buildTrendingTopics(List<RawContent> content) {
        // Map: hashtag → { totalShares, platforms set }
        record TopicAcc(long shares, java.util.Set<com.chimera.trendwatcher.model.Platform> platforms) {}

        Map<String, long[]> sharesByTag   = new LinkedHashMap<>();
        Map<String, java.util.Set<com.chimera.trendwatcher.model.Platform>> platformsByTag =
                new LinkedHashMap<>();

        for (RawContent c : content) {
            for (String tag : c.hashtags()) {
                String key = tag.toLowerCase().startsWith("#") ? tag.toLowerCase() : "#" + tag.toLowerCase();
                sharesByTag.merge(key, new long[]{c.shareCount()}, (a, b) -> new long[]{a[0] + b[0]});
                platformsByTag.computeIfAbsent(key, k -> new java.util.HashSet<>()).add(c.platform());
            }
        }

        return sharesByTag.entrySet().stream()
                .sorted(Map.Entry.<String, long[]>comparingByValue(
                        Comparator.comparingLong(a -> -a[0])))
                .limit(TOP_TOPICS)
                .map(e -> new TrendingTopic(
                        e.getKey(),
                        e.getValue()[0],
                        new ArrayList<>(platformsByTag.get(e.getKey()))))
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------------------
    // Private — category aggregation
    // ---------------------------------------------------------------------------

    private List<TopCategory> buildTopCategories(List<RawContent> content) {
        record CategoryStats(long totalViews, long count) {}

        Map<String, long[]> statsByCategory = new LinkedHashMap<>(); // [totalViews, count]

        for (RawContent c : content) {
            String cat = (c.category() == null || c.category().isBlank()) ? "Uncategorized" : c.category();
            statsByCategory.merge(
                    cat,
                    new long[]{c.viewCount(), 1},
                    (a, b) -> new long[]{a[0] + b[0], a[1] + b[1]}
            );
        }

        return statsByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, long[]>comparingByValue(
                        Comparator.comparingLong(a -> -a[0])))
                .limit(TOP_CATEGORIES)
                .map(e -> new TopCategory(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .collect(Collectors.toList());
    }
}

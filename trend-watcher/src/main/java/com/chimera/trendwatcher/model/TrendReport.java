package com.chimera.trendwatcher.model;

import java.time.Instant;
import java.util.List;

/**
 * Output model of the TrendWatcher service.
 *
 * Spec outputs:
 * - trending topics (ranked by share count)
 * - top viewed categories (ranked by view count)
 * - timestamp of report generation
 *
 * @param trendingTopics  Ranked list of trending topics / hashtags
 * @param topCategories   Ranked list of content categories by total views
 * @param timeRange       The analysis window this report covers
 * @param generatedAt     UTC timestamp when this report was produced
 */
public record TrendReport(
        List<TrendingTopic> trendingTopics,
        List<TopCategory> topCategories,
        TimeRange timeRange,
        Instant generatedAt
) {}

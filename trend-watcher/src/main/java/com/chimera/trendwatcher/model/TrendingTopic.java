package com.chimera.trendwatcher.model;

import java.util.List;

/**
 * A topic or hashtag trending across one or more platforms.
 *
 * @param topic       The topic label or hashtag (e.g. "#AI", "World Cup")
 * @param totalShares Aggregate share/repost count across all platforms
 * @param platforms   Platforms on which this topic appeared
 */
public record TrendingTopic(
        String topic,
        long totalShares,
        List<Platform> platforms
) {}

package com.chimera.trendwatcher.model;

import java.time.Instant;
import java.util.List;

/**
 * Raw content ingested from a social media platform before validation and aggregation.
 *
 * @param id            Platform-native content identifier
 * @param text          Full text / caption of the post
 * @param sourceUrl     Direct URL to the post
 * @param sourceHandle  Author handle (e.g. @username)
 * @param platform      Origin platform
 * @param shareCount    Number of shares/retweets/reposts
 * @param viewCount     Number of views/impressions
 * @param category      Self-reported or inferred content category (e.g. "Sports", "Tech")
 * @param hashtags      Hashtags extracted from the post
 * @param publishedAt   When the content was originally posted
 * @param fetchedAt     When this service fetched the content (used for staleness check)
 */
public record RawContent(
        String id,
        String text,
        String sourceUrl,
        String sourceHandle,
        Platform platform,
        long shareCount,
        long viewCount,
        String category,
        List<String> hashtags,
        Instant publishedAt,
        Instant fetchedAt
) {}

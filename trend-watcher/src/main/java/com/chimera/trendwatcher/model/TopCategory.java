package com.chimera.trendwatcher.model;

/**
 * A content category ranked by total views within the report window.
 *
 * @param name         Category name (e.g. "Sports", "Technology", "Entertainment")
 * @param totalViews   Aggregate view/impression count for this category
 * @param contentCount Number of individual pieces of content in this category
 */
public record TopCategory(
        String name,
        long totalViews,
        long contentCount
) {}

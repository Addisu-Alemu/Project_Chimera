package com.chimera.learnservice.analyzer;

public record EngagementSummary(
        long likes,
        long shares,
        long comments,
        long views,
        long clicks
) {
    public double clickThroughRate() {
        return views > 0 ? (double) clicks / views : 0.0;
    }
}

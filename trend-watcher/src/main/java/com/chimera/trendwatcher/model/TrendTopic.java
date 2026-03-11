package com.chimera.trendwatcher.model;

import java.util.List;

public record TrendTopic(
        String name,
        List<String> hashtags,
        double engagementScore,
        boolean safetyPassed
) {}

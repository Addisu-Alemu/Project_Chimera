package com.chimera.trendwatcher.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TrendReport(
        UUID id,
        UUID agentId,
        Instant fetchedAt,
        List<Platform> platforms,
        List<TrendTopic> topics,
        Map<String, Double> categoryWeights
) {}

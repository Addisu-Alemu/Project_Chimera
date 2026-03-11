package com.chimera.contentcreator.client.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TrendReportDto(
        UUID id,
        UUID agentId,
        Instant fetchedAt,
        List<String> platforms,
        List<TrendTopicDto> topics,
        Map<String, Double> categoryWeights
) {}

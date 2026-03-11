package com.chimera.contentcreator.client.dto;

import java.util.List;

public record TrendTopicDto(
        String name,
        List<String> hashtags,
        double engagementScore,
        boolean safetyPassed
) {}

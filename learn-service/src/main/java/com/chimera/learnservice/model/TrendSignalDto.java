package com.chimera.learnservice.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TrendSignalDto(
        UUID id,
        UUID agentId,
        UUID sourceFeedbackReportId,
        Map<String, Double> categoryWeights,
        Instant issuedAt
) {}

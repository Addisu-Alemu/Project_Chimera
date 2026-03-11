package com.chimera.learnservice.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FeedbackReportDto(
        UUID id,
        UUID agentId,
        UUID contentBundleId,
        BigDecimal confidenceScore,
        long likes,
        long shares,
        long comments,
        long views,
        BigDecimal clickThroughRate,
        ReviewStatus reviewStatus,
        Instant generatedAt
) {}

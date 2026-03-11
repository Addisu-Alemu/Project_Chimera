package com.chimera.contentcreator.client.dto;

import java.util.UUID;

public record FeedbackReportDto(
        UUID id,
        UUID agentId,
        UUID contentBundleId,
        double confidenceScore,
        String reviewStatus
) {}

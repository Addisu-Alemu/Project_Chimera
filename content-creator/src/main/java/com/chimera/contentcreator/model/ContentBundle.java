package com.chimera.contentcreator.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContentBundle(
        UUID id,
        UUID agentId,
        UUID trendReportId,
        String caption,
        List<String> hashtags,
        String videoDescription,
        Instant safetyPassedAt,
        Instant generatedAt
) {}

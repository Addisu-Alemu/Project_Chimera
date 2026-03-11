package com.chimera.actservice.client.dto;

import java.time.Instant;
import java.util.UUID;

public record EngagementSignalDto(
        UUID agentId,
        UUID postResultId,
        String signalType,
        long value,
        Instant recordedAt
) {}

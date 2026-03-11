package com.chimera.actservice.model;

import java.time.Instant;
import java.util.UUID;

public record AudienceInteraction(
        UUID id,
        UUID agentId,
        UUID postResultId,
        String platform,
        String interactionType,
        String content,
        Instant receivedAt
) {}

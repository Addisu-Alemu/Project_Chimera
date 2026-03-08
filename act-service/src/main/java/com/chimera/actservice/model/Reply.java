package com.chimera.actservice.model;

import com.chimera.trendwatcher.model.Platform;

import java.time.Instant;

/**
 * A generated reply to an audience interaction.
 * Sent within the configured response window and forwarded to the LEARN service.
 *
 * @param id            Unique reply identifier
 * @param interactionId ID of the AudienceInteraction this reply addresses
 * @param body          Text content of the reply
 * @param platform      Platform on which the reply is sent
 * @param generatedAt   UTC timestamp when the reply was generated
 * @param sentAt        UTC timestamp when the reply was dispatched to the platform
 */
public record Reply(
        String id,
        String interactionId,
        String body,
        Platform platform,
        Instant generatedAt,
        Instant sentAt
) {}

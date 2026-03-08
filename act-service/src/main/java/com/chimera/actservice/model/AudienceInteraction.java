package com.chimera.actservice.model;

import com.chimera.trendwatcher.model.Platform;

import java.time.Instant;

/**
 * A single engagement event received from an audience member on a published post.
 *
 * @param id              Unique interaction identifier
 * @param userId          Platform-native user ID of the audience member
 * @param username        Display handle of the audience member
 * @param platform        Platform where the interaction occurred
 * @param type            Nature of the engagement
 * @param content         Text body (comment, DM, or mention text; null for reactions)
 * @param referencePostId Platform post ID this interaction refers to (traceability)
 * @param receivedAt      UTC timestamp when the interaction was received — used to enforce response window
 */
public record AudienceInteraction(
        String id,
        String userId,
        String username,
        Platform platform,
        InteractionType type,
        String content,
        String referencePostId,
        Instant receivedAt
) {}

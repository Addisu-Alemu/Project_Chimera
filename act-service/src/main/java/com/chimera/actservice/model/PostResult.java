package com.chimera.actservice.model;

import com.chimera.trendwatcher.model.Platform;

import java.time.Instant;
import java.util.UUID;

/**
 * Outcome of a single content publishing attempt.
 *
 * Rule: all posts must be traceable to the source spec.
 * {@code contentPieceId} links every result back to the originating ContentPiece
 * produced by the CREATE service.
 *
 * @param id              Unique result identifier
 * @param contentPieceId  ID of the ContentPiece that was published (traceable to CREATE spec)
 * @param platform        Target platform for this attempt
 * @param status          Outcome of the publish operation
 * @param platformPostId  Platform-assigned post ID (null if not published successfully)
 * @param postedAt        UTC timestamp of the successful post (null if failed/paused)
 * @param attemptCount    Number of attempts made before this outcome
 */
public record PostResult(
        String id,
        String contentPieceId,
        Platform platform,
        PostStatus status,
        String platformPostId,
        Instant postedAt,
        int attemptCount
) {
    public static PostResult published(String contentPieceId, Platform platform,
                                       String platformPostId, int attempts) {
        return new PostResult(UUID.randomUUID().toString(), contentPieceId, platform,
                PostStatus.PUBLISHED, platformPostId, Instant.now(), attempts);
    }

    public static PostResult failed(String contentPieceId, Platform platform, int attempts) {
        return new PostResult(UUID.randomUUID().toString(), contentPieceId, platform,
                PostStatus.FAILED, null, null, attempts);
    }

    public static PostResult paused(String contentPieceId, Platform platform) {
        return new PostResult(UUID.randomUUID().toString(), contentPieceId, platform,
                PostStatus.PAUSED, null, null, 0);
    }
}

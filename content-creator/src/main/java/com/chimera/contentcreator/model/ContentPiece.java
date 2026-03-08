package com.chimera.contentcreator.model;

import com.chimera.trendwatcher.model.Platform;

import java.time.Instant;
import java.util.List;

/**
 * A single piece of generated content — output model of the CREATE service.
 *
 * Spec rules baked in:
 * - {@code generatedAt} is always set (timestamp rule)
 * - {@code sourceReferences} carries verified-source attribution (no false information rule)
 * - Only pieces that pass {@link com.chimera.contentcreator.filter.ContentSafetyFilter}
 *   reach the ACT service
 *
 * @param id               Unique content identifier
 * @param body             The actual generated text (post / caption / description)
 * @param contentType      Format of this piece
 * @param topic            The trending topic this piece is about
 * @param sourceReferences Verified sources this content references (from PERCEIVE)
 * @param hashtags         Hashtags included in the body
 * @param targetPlatforms  Platforms this piece is intended for
 * @param generatedAt      UTC timestamp — REQUIRED by spec
 */
public record ContentPiece(
        String id,
        String body,
        ContentType contentType,
        String topic,
        List<String> sourceReferences,
        List<String> hashtags,
        List<Platform> targetPlatforms,
        Instant generatedAt
) {}

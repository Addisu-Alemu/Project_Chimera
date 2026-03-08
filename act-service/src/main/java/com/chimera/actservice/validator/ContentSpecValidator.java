package com.chimera.actservice.validator;

import com.chimera.actservice.exception.InvalidContentSpecException;
import com.chimera.contentcreator.model.ContentPiece;

import java.time.Duration;
import java.time.Instant;

/**
 * Validates that a {@link ContentPiece} carries a legitimate spec from the CREATE service
 * before the ACT service is allowed to publish it.
 *
 * Rule: never post content without a valid spec from CREATE.
 *
 * Checks:
 * - Required fields are present (id, body, contentType, sourceReferences, generatedAt, targetPlatforms)
 * - Content is not older than MAX_CONTENT_AGE (stale content must not be published)
 * - Body is non-blank (no empty posts)
 * - Source references are present (traceability requirement)
 */
public class ContentSpecValidator {

    /** Maximum age of a ContentPiece before it is considered too stale to publish. */
    private static final Duration MAX_CONTENT_AGE = Duration.ofHours(24);

    /**
     * Validates the piece. Throws {@link InvalidContentSpecException} on any violation.
     */
    public void validate(ContentPiece piece) {
        if (piece == null) {
            throw new InvalidContentSpecException("ContentPiece is null — no spec received from CREATE");
        }
        if (piece.id() == null || piece.id().isBlank()) {
            throw new InvalidContentSpecException("ContentPiece missing id — cannot trace to source spec");
        }
        if (piece.body() == null || piece.body().isBlank()) {
            throw new InvalidContentSpecException("ContentPiece id=" + piece.id() + " has empty body");
        }
        if (piece.contentType() == null) {
            throw new InvalidContentSpecException("ContentPiece id=" + piece.id() + " has no contentType");
        }
        if (piece.sourceReferences() == null || piece.sourceReferences().isEmpty()) {
            throw new InvalidContentSpecException(
                    "ContentPiece id=" + piece.id() + " has no source references — traceability violated");
        }
        if (piece.generatedAt() == null) {
            throw new InvalidContentSpecException("ContentPiece id=" + piece.id() + " has no timestamp");
        }
        if (piece.targetPlatforms() == null || piece.targetPlatforms().isEmpty()) {
            throw new InvalidContentSpecException("ContentPiece id=" + piece.id() + " has no target platforms");
        }

        // Staleness check
        if (piece.generatedAt().isBefore(Instant.now().minus(MAX_CONTENT_AGE))) {
            throw new InvalidContentSpecException(
                    "ContentPiece id=" + piece.id() + " is stale (generatedAt=" + piece.generatedAt() + ")");
        }
    }
}

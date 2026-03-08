package com.chimera.actservice.publisher;

import com.chimera.actservice.exception.PublishException;
import com.chimera.actservice.model.PostResult;
import com.chimera.contentcreator.model.ContentPiece;
import com.chimera.trendwatcher.model.Platform;

/**
 * Contract for all platform-specific content publishers.
 *
 * Implementations must:
 * - Try the primary endpoint first
 * - Fall back to the backup endpoint if primary is unavailable
 * - Throw {@link PublishException} only when both endpoints fail
 * - Implement {@link #isHealthy()} for the orchestrator's recovery loop
 */
public interface ContentPublisher {

    Platform platform();

    /**
     * Publishes a single content piece to the platform.
     * Returns a {@link PostResult} on success.
     * Throws {@link PublishException} on failure (triggering retry in the orchestrator).
     */
    PostResult publish(ContentPiece piece) throws PublishException;

    /**
     * Returns {@code true} if the platform API is reachable.
     * Used by the health-check loop to detect recovery after a PAUSED state.
     */
    boolean isHealthy();
}

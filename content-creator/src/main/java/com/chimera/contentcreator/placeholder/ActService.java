package com.chimera.contentcreator.placeholder;

import com.chimera.contentcreator.model.ContentPiece;

/**
 * PLACEHOLDER — will be connected to the ACT service in a future iteration.
 *
 * Receives safety-approved {@link ContentPiece} instances from the CREATE service
 * and is responsible for publishing / scheduling them on the appropriate platforms.
 *
 * The CREATE service calls {@link #publish(ContentPiece)} only after a piece has
 * passed the {@link com.chimera.contentcreator.filter.ContentSafetyFilter}.
 */
public interface ActService {

    /**
     * Accepts a safety-checked content piece for downstream publishing.
     *
     * @param piece A fully-formed, safety-approved content piece.
     */
    void publish(ContentPiece piece);

    // -------------------------------------------------------------------------
    // Factory — no-op stub used until ACT service is implemented
    // -------------------------------------------------------------------------

    static ActService noOp() {
        return piece -> {
            // Intentional no-op — ACT service not yet connected
            org.slf4j.LoggerFactory.getLogger(ActService.class)
                    .info("ACT [stub]: would publish id={} type={} topic={}",
                            piece.id(), piece.contentType(), piece.topic());
        };
    }
}

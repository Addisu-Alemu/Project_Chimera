package com.chimera.actservice.placeholder;

import com.chimera.actservice.model.AudienceInteraction;
import com.chimera.actservice.model.PostResult;
import com.chimera.actservice.model.Reply;

import java.util.List;

/**
 * PLACEHOLDER — will be connected to the LEARN service in a future iteration.
 *
 * The ACT service forwards two streams of data to LEARN:
 * 1. Interaction + reply pairs — for sentiment and engagement analysis.
 * 2. Post results + interaction lists — for per-content engagement metrics.
 *
 * LEARN uses this data to update {@link com.chimera.contentcreator.placeholder.FeedbackData}
 * which is consumed by the CREATE service to adjust generation parameters.
 *
 * A no-op default is available via {@link #noOp()}.
 */
public interface LearnService {

    /**
     * Submits a resolved audience interaction and the reply sent in response.
     * LEARN uses this to analyse tone, sentiment, and response effectiveness.
     */
    void submitInteractionData(AudienceInteraction interaction, Reply reply);

    /**
     * Submits the publishing outcome and related interactions for a content piece.
     * LEARN uses this to build per-topic and per-format engagement metrics.
     */
    void submitEngagementMetrics(PostResult result, List<AudienceInteraction> interactions);

    // -------------------------------------------------------------------------
    // Factory — no-op stub until LEARN service is implemented
    // -------------------------------------------------------------------------

    static LearnService noOp() {
        return new LearnService() {
            private final org.slf4j.Logger log =
                    org.slf4j.LoggerFactory.getLogger(LearnService.class);

            @Override
            public void submitInteractionData(AudienceInteraction interaction, Reply reply) {
                log.info("LEARN [stub]: interaction id={} replied='{}'",
                        interaction.id(), reply.body());
            }

            @Override
            public void submitEngagementMetrics(PostResult result, List<AudienceInteraction> interactions) {
                log.info("LEARN [stub]: postResult id={} status={} interactions={}",
                        result.id(), result.status(), interactions.size());
            }
        };
    }
}

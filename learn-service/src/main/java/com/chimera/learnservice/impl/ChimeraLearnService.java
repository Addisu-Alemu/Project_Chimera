package com.chimera.learnservice.impl;

import com.chimera.actservice.model.AudienceInteraction;
import com.chimera.actservice.model.InteractionType;
import com.chimera.actservice.model.PostResult;
import com.chimera.actservice.model.PostStatus;
import com.chimera.actservice.model.Reply;
import com.chimera.actservice.placeholder.LearnService;
import com.chimera.learnservice.model.EngagementSignal;
import com.chimera.learnservice.model.SignalType;
import com.chimera.learnservice.signal.SignalIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Concrete implementation of {@link LearnService} — the placeholder left in the act-service module.
 *
 * This is the ACT → LEARN connection. The ACT service calls:
 * - {@link #submitInteractionData}     after each audience reply is generated
 * - {@link #submitEngagementMetrics}   after a post result + interactions are aggregated
 *
 * Both methods run on the caller's virtual thread and convert ACT data into
 * {@link EngagementSignal}s for the ingestion pipeline.
 */
public class ChimeraLearnService implements LearnService {

    private static final Logger log = LoggerFactory.getLogger(ChimeraLearnService.class);

    private final SignalIngestionService ingestionService;

    public ChimeraLearnService(SignalIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    // -------------------------------------------------------------------------
    // LearnService implementation
    // -------------------------------------------------------------------------

    /**
     * Receives a single audience interaction and the reply sent to it.
     * Converts interaction type to metric deltas (comment → +1 comment, reaction → +1 like, etc.)
     */
    @Override
    public void submitInteractionData(AudienceInteraction interaction, Reply reply) {
        log.debug("LEARN: interaction received id={} type={} platform={}",
                interaction.id(), interaction.type(), interaction.platform());

        long likes    = interaction.type() == InteractionType.REACTION       ? 1 : 0;
        long comments = interaction.type() == InteractionType.COMMENT        ? 1 : 0;
        long shares   = interaction.type() == InteractionType.MENTION        ? 1 : 0;
        long dms      = interaction.type() == InteractionType.DIRECT_MESSAGE ? 1 : 0;

        EngagementSignal signal = new EngagementSignal(
                UUID.randomUUID().toString(),
                interaction.referencePostId(),
                extractTopic(interaction.content()),
                interaction.platform(),
                null,          // ContentType unknown from a single interaction
                likes,
                shares,
                comments + dms,
                0,             // views not available from individual interaction
                SignalType.INTERACTION,
                true,
                interaction.receivedAt()
        );

        ingestionService.ingest(signal);
    }

    /**
     * Receives an aggregate PostResult and all interactions for that post.
     * Builds a comprehensive engagement signal with full metric breakdown.
     */
    @Override
    public void submitEngagementMetrics(PostResult result, List<AudienceInteraction> interactions) {
        log.debug("LEARN: engagement metrics received postId={} status={} interactions={}",
                result.id(), result.status(), interactions.size());

        // Derive metrics from the interaction list
        long likes    = interactions.stream().filter(i -> i.type() == InteractionType.REACTION).count();
        long comments = interactions.stream().filter(i -> i.type() == InteractionType.COMMENT
                                                       || i.type() == InteractionType.DIRECT_MESSAGE).count();
        long shares   = interactions.stream().filter(i -> i.type() == InteractionType.MENTION).count();

        // A published post gets a base view count of 1 (placeholder for real API view counts)
        long views = result.status() == PostStatus.PUBLISHED ? 1 : 0;

        // Transaction outcome: PUBLISHED = positive signal
        boolean transactionPositive = result.status() == PostStatus.PUBLISHED;

        EngagementSignal signal = new EngagementSignal(
                UUID.randomUUID().toString(),
                result.contentPieceId(),
                "",            // topic not available from PostResult — enriched via memory lookup
                result.platform(),
                null,          // ContentType not available from PostResult
                likes,
                shares,
                comments,
                views,
                SignalType.ENGAGEMENT_METRIC,
                transactionPositive,
                result.postedAt() != null ? result.postedAt() : java.time.Instant.now()
        );

        ingestionService.ingest(signal);
    }

    // -------------------------------------------------------------------------

    /** Simple topic extractor: finds the first hashtag in text, or returns empty string. */
    private String extractTopic(String text) {
        if (text == null) return "";
        for (String word : text.split("\\s+")) {
            if (word.startsWith("#")) return word;
        }
        return "";
    }
}

package com.chimera.contentcreator.service;

import com.chimera.contentcreator.exception.StaleTrendReportException;
import com.chimera.contentcreator.filter.ContentSafetyFilter;
import com.chimera.contentcreator.generator.ContentGenerator;
import com.chimera.contentcreator.model.ContentPiece;
import com.chimera.contentcreator.model.ContentType;
import com.chimera.contentcreator.model.GenerationParameters;
import com.chimera.contentcreator.model.Tone;
import com.chimera.contentcreator.placeholder.ActService;
import com.chimera.contentcreator.placeholder.FeedbackData;
import com.chimera.trendwatcher.model.TrendReport;
import com.chimera.trendwatcher.model.TrendingTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Core orchestrator of the CREATE service.
 *
 * Spec rules enforced here:
 *
 * 1. Content must have a timestamp         — {@link ContentPiece#generatedAt()} always set in generator.
 * 2. Must reference verified sources       — sourced from TrendReport (PERCEIVE-validated).
 * 3. Must pass safety filter before ACT    — {@link #generateWithSafetyRetry} enforces this.
 * 4. If TrendReport is stale → throw       — {@link #validateFreshness} throws {@link StaleTrendReportException}.
 * 5. Safety filter fails → discard & retry — up to {@value MAX_SAFETY_RETRIES} attempts per piece.
 * 6. Bad audience feedback → adjust params — {@link #computeParameters} reads {@link FeedbackData}.
 *
 * All per-topic generation runs on Java 21 virtual threads.
 */
public class ContentCreatorService {

    private static final Logger log = LoggerFactory.getLogger(ContentCreatorService.class);

    /** TrendReport older than this is considered stale — caller must fetch a fresh one. */
    private static final Duration REPORT_MAX_AGE = Duration.ofHours(6);

    /** Safety-filter retry budget per (topic, contentType) pair before discarding. */
    private static final int MAX_SAFETY_RETRIES = 3;

    private final ContentGenerator  generator;
    private final ContentSafetyFilter safetyFilter;
    private final ActService         actService;

    public ContentCreatorService(ContentGenerator generator,
                                 ContentSafetyFilter safetyFilter,
                                 ActService actService) {
        this.generator   = generator;
        this.safetyFilter = safetyFilter;
        this.actService  = actService;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Main entry point.
     *
     * Validates the TrendReport, derives generation parameters from LEARN feedback,
     * generates content for every trending topic on virtual threads,
     * filters through safety, and hands approved pieces to ACT.
     *
     * @param report       TrendReport from PERCEIVE — must not be stale.
     * @param feedbackData Audience feedback from LEARN (pass {@link FeedbackData#neutral()}
     *                     until LEARN is connected).
     * @return Immutable list of safety-approved {@link ContentPiece} instances.
     * @throws StaleTrendReportException if the report is older than {@value} hours.
     */
    public List<ContentPiece> createContent(TrendReport report, FeedbackData feedbackData) {
        validateFreshness(report);

        GenerationParameters params = computeParameters(feedbackData);
        log.info("CREATE: generating content | range={} topics={} tone={} emoji={}",
                report.timeRange(), report.trendingTopics().size(), params.tone(), params.emojiEnabled());

        List<ContentPiece> approved = generateAllTopicsConcurrently(report, params);

        log.info("CREATE: {} content pieces approved and sent to ACT", approved.size());
        return approved;
    }

    // -------------------------------------------------------------------------
    // Step 1 — Staleness gate
    // -------------------------------------------------------------------------

    /**
     * Rule: if the input is stale, request a fresh TrendReport.
     * Callers must catch {@link StaleTrendReportException} and re-trigger PERCEIVE.
     */
    private void validateFreshness(TrendReport report) {
        Instant cutoff = Instant.now().minus(REPORT_MAX_AGE);
        if (report.generatedAt().isBefore(cutoff)) {
            throw new StaleTrendReportException(
                    "TrendReport is stale: generatedAt=" + report.generatedAt()
                    + ", max age=" + REPORT_MAX_AGE + ". Request a fresh report from PERCEIVE.");
        }
    }

    // -------------------------------------------------------------------------
    // Step 2 — Derive generation parameters from LEARN feedback
    // -------------------------------------------------------------------------

    /**
     * Rule: if feedback from the audience is bad, adjust generation parameters.
     */
    private GenerationParameters computeParameters(FeedbackData feedback) {
        GenerationParameters base = GenerationParameters.defaults();

        if (feedback == null) return base;

        double score = feedback.engagementScore();

        if (score < 0.3) {
            log.warn("LEARN: poor engagement score={:.2f} — switching to HUMOROUS, enabling emojis, raising creativity", score);
            return base.withTone(Tone.HUMOROUS).withEmojiEnabled(true).withCreativity(0.8);
        }
        if (score < 0.5) {
            log.info("LEARN: below-average engagement score={:.2f} — switching to CASUAL with emojis", score);
            return base.withTone(Tone.CASUAL).withEmojiEnabled(true);
        }

        return base;
    }

    // -------------------------------------------------------------------------
    // Step 3 — Concurrent generation with virtual threads
    // -------------------------------------------------------------------------

    private List<ContentPiece> generateAllTopicsConcurrently(TrendReport report,
                                                              GenerationParameters params) {
        List<ContentPiece> result = new ArrayList<>();

        // Java 21 virtual threads — one per topic, lightweight and non-blocking
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<List<ContentPiece>>> futures = report.trendingTopics().stream()
                    .map(topic -> executor.submit(() -> generateForTopic(topic, report, params)))
                    .toList();

            for (Future<List<ContentPiece>> future : futures) {
                try {
                    result.addAll(future.get());
                } catch (ExecutionException e) {
                    log.error("CREATE: topic generation task failed: {}", e.getCause().getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("CREATE: generation interrupted", e);
                }
            }
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Step 4 — Per-topic generation: all content types + safety gate
    // -------------------------------------------------------------------------

    private List<ContentPiece> generateForTopic(TrendingTopic topic,
                                                 TrendReport report,
                                                 GenerationParameters params) {
        List<ContentPiece> pieces = new ArrayList<>();

        for (ContentType type : ContentType.values()) {
            generateWithSafetyRetry(topic, report, type, params).ifPresent(piece -> {
                actService.publish(piece);   // Rule: only safety-approved pieces reach ACT
                pieces.add(piece);
            });
        }

        log.info("CREATE: topic='{}' → {}/{} content types generated",
                topic.topic(), pieces.size(), ContentType.values().length);
        return pieces;
    }

    // -------------------------------------------------------------------------
    // Step 5 — Safety retry loop
    // -------------------------------------------------------------------------

    /**
     * Rule: if content fails the safety filter → discard and regenerate.
     * After {@value MAX_SAFETY_RETRIES} failed attempts, the piece is discarded entirely.
     */
    private Optional<ContentPiece> generateWithSafetyRetry(TrendingTopic topic,
                                                             TrendReport report,
                                                             ContentType type,
                                                             GenerationParameters params) {
        for (int attempt = 1; attempt <= MAX_SAFETY_RETRIES; attempt++) {
            ContentPiece piece = generator.generate(topic, report, type, params);

            if (safetyFilter.isSafe(piece.body())) {
                return Optional.of(piece);
            }

            log.warn("SAFETY [CREATE]: piece failed filter — topic='{}' type={} attempt={}/{}",
                    topic.topic(), type, attempt, MAX_SAFETY_RETRIES);
        }

        log.error("SAFETY [CREATE]: all {} attempts failed — discarding topic='{}' type={}",
                MAX_SAFETY_RETRIES, topic.topic(), type);
        return Optional.empty();
    }
}

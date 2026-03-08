package com.chimera.learnservice;

import com.chimera.actservice.model.AudienceInteraction;
import com.chimera.actservice.model.InteractionType;
import com.chimera.actservice.model.PostResult;
import com.chimera.actservice.model.PostStatus;
import com.chimera.actservice.model.Reply;
import com.chimera.learnservice.alert.LearnAlertService;
import com.chimera.learnservice.analyzer.PerformanceAnalyzer;
import com.chimera.learnservice.connector.FeedbackDispatcher;
import com.chimera.learnservice.feedback.CreateFeedbackAdapter;
import com.chimera.learnservice.feedback.PerceiveFeedbackAdapter;
import com.chimera.learnservice.impl.ChimeraLearnService;
import com.chimera.learnservice.memory.PerformanceMemory;
import com.chimera.learnservice.service.LearnOrchestrator;
import com.chimera.learnservice.service.ReportBuilder;
import com.chimera.learnservice.signal.SignalIngestionService;
import com.chimera.learnservice.signal.SignalValidator;
import com.chimera.trendwatcher.model.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Entry point for the LEARN standalone service.
 *
 * Demonstrates the full closed loop:
 *
 *   ACT → ChimeraLearnService (LearnService impl)
 *              ↓
 *         SignalIngestionService (validate → accumulate → score → store)
 *              ↓
 *         PerformanceMemory (historical store)
 *              ↓
 *         ReportBuilder → FeedbackReport
 *              ↓
 *   ┌──── FeedbackDispatcher ────────────────────┐
 *   │                                            │
 *   ▼                                            ▼
 * CreateFeedbackAdapter (FeedbackData impl)   PerceiveFeedbackAdapter
 *       ↓                                           ↓
 *   CREATE service                            PERCEIVE service
 * (adjusts GenerationParameters)         (weights trending topics)
 *
 * In production:
 * - Replace stub signals with live ACT output by injecting ChimeraLearnService
 *   into ActServiceApplication (replace LearnService.noOp()).
 * - Replace CreateFeedbackAdapter.neutral() in ContentCreatorService with
 *   the live CreateFeedbackAdapter backed by PerformanceMemory.
 */
public class LearnServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(LearnServiceApplication.class);

    public static void main(String[] args) throws InterruptedException {
        log.info("=== Project Chimera — LEARN service starting ===");
        log.info("    This service CLOSES THE LOOP: ACT → LEARN → CREATE + PERCEIVE");

        // -----------------------------------------------------------------------
        // Component assembly
        // -----------------------------------------------------------------------
        LearnAlertService    alertService   = new LearnAlertService();
        PerformanceMemory    memory         = new PerformanceMemory();
        SignalValidator      validator      = new SignalValidator(alertService);
        PerformanceAnalyzer  analyzer       = new PerformanceAnalyzer();
        SignalIngestionService ingestion    = new SignalIngestionService(validator, analyzer, memory, alertService);

        // Feedback adapters — implement the placeholders left in CREATE and PERCEIVE
        CreateFeedbackAdapter   createAdapter  = new CreateFeedbackAdapter(memory);
        PerceiveFeedbackAdapter perceiveAdapter = new PerceiveFeedbackAdapter(memory);

        FeedbackDispatcher dispatcher = new FeedbackDispatcher(perceiveAdapter, alertService);
        ReportBuilder      builder    = new ReportBuilder(memory);
        ChimeraLearnService learnSvc  = new ChimeraLearnService(ingestion);
        LearnOrchestrator  orchestrator = new LearnOrchestrator(learnSvc, builder, dispatcher);

        orchestrator.start();

        // -----------------------------------------------------------------------
        // Demo — simulate signals arriving from ACT
        // -----------------------------------------------------------------------
        log.info("--- DEMO: simulating engagement signals from ACT ---");

        // High-performing content: lots of shares/comments
        simulateEngagement(orchestrator,
                "piece-ai-post",     "#AI",           Platform.TWITTER,   600, 150, 80, 12000);
        simulateEngagement(orchestrator,
                "piece-ai-caption",  "#AI",           Platform.TIKTOK,    800, 200, 60, 50000);
        simulateEngagement(orchestrator,
                "piece-wc-post",     "#WorldCup2026", Platform.INSTAGRAM, 500, 300, 120, 80000);

        // Average content
        simulateEngagement(orchestrator,
                "piece-climate-vid", "#ClimateAction",Platform.TWITTER,   80,  20,  15, 3000);

        // Poor-performing content (will trigger NEGATIVE alert)
        simulateEngagement(orchestrator,
                "piece-weak-post",   "#OldTrend",     Platform.INSTAGRAM, 2,   1,   0,  100);

        // Corrupt signal (missing contentPieceId — will be rejected)
        simulateCorruptSignal(orchestrator);

        // Individual interaction events (as ACT would submit)
        simulateInteraction(orchestrator, "piece-ai-post", Platform.TWITTER,
                InteractionType.COMMENT, "piece-ai-post");
        simulateInteraction(orchestrator, "piece-ai-post", Platform.TWITTER,
                InteractionType.REACTION, "piece-ai-post");
        simulateInteraction(orchestrator, "piece-wc-post", Platform.INSTAGRAM,
                InteractionType.MENTION, "piece-wc-post");

        // Force report generation
        orchestrator.generateAndDispatch("demo-complete");

        // -----------------------------------------------------------------------
        // Show what CREATE and PERCEIVE would receive
        // -----------------------------------------------------------------------
        Thread.sleep(500); // let async threads settle

        log.info("--- DEMO: CREATE feedback (via CreateFeedbackAdapter) ---");
        log.info("  overallEngagementScore  : {}", String.format("%.3f", createAdapter.engagementScore()));
        log.info("  engagementByType        : {}", createAdapter.engagementByType());
        log.info("  underperformingTopics   : {}", createAdapter.underperformingTopics());

        log.info("--- DEMO: PERCEIVE feedback (via PerceiveFeedbackAdapter) ---");
        log.info("  highPerformingTopics    : {}", perceiveAdapter.getHighPerformingTopics());
        log.info("  underperformingTopics   : {}", perceiveAdapter.getUnderperformingTopics());
        log.info("  bestTopicsByPlatform    : {}", perceiveAdapter.getBestTopicsByPlatform());

        log.info("--- DEMO: PerformanceMemory state ---");
        log.info("  total tracked           : {}", memory.totalTracked());
        log.info("  total history entries   : {}", memory.totalHistory());

        orchestrator.stop();
        log.info("=== LEARN service demo complete — loop is CLOSED ===");
    }

    // -------------------------------------------------------------------------
    // Simulation helpers
    // -------------------------------------------------------------------------

    private static void simulateEngagement(LearnOrchestrator orchestrator,
                                            String contentPieceId, String topic,
                                            Platform platform,
                                            long likes, long shares, long comments, long views) {
        PostResult result = new PostResult(
                UUID.randomUUID().toString(),
                contentPieceId,
                platform,
                PostStatus.PUBLISHED,
                "platform-post-" + UUID.randomUUID(),
                Instant.now(),
                1
        );

        // Build stub interactions matching the counts
        List<AudienceInteraction> interactions = buildInteractions(
                contentPieceId, platform, likes, shares, comments);

        orchestrator.onEngagementMetrics(result, interactions);
    }

    private static void simulateCorruptSignal(LearnOrchestrator orchestrator) {
        // Submit a PostResult with null contentPieceId (will be caught by validator)
        PostResult corruptResult = new PostResult(
                UUID.randomUUID().toString(),
                null,              // ← corrupt: missing contentPieceId
                Platform.TWITTER,
                PostStatus.PUBLISHED,
                null,
                Instant.now(),
                1
        );
        orchestrator.onEngagementMetrics(corruptResult, List.of());
    }

    private static void simulateInteraction(LearnOrchestrator orchestrator,
                                             String contentPieceId, Platform platform,
                                             InteractionType type, String postId) {
        AudienceInteraction interaction = new AudienceInteraction(
                UUID.randomUUID().toString(),
                "user-" + UUID.randomUUID().toString().substring(0, 8),
                "@demo_user",
                platform,
                type,
                type == InteractionType.COMMENT ? "Great content!" :
                type == InteractionType.MENTION  ? "@chimera love this!" : null,
                postId,
                Instant.now()
        );
        Reply reply = new Reply(
                UUID.randomUUID().toString(),
                interaction.id(),
                "Thanks for engaging!",
                platform,
                Instant.now(),
                Instant.now()
        );
        orchestrator.onInteractionData(interaction, reply);
    }

    private static List<AudienceInteraction> buildInteractions(String postId, Platform platform,
                                                                 long likes, long shares, long comments) {
        // Build simplified interaction list: likes → REACTION, shares → MENTION, comments → COMMENT
        var list = new java.util.ArrayList<AudienceInteraction>();
        for (long i = 0; i < Math.min(likes, 100); i++) {
            list.add(new AudienceInteraction(UUID.randomUUID().toString(),
                    "u" + i, "@user" + i, platform, InteractionType.REACTION, null, postId, Instant.now()));
        }
        for (long i = 0; i < Math.min(shares, 100); i++) {
            list.add(new AudienceInteraction(UUID.randomUUID().toString(),
                    "u" + i, "@user" + i, platform, InteractionType.MENTION, "@chimera", postId, Instant.now()));
        }
        for (long i = 0; i < Math.min(comments, 100); i++) {
            list.add(new AudienceInteraction(UUID.randomUUID().toString(),
                    "u" + i, "@user" + i, platform, InteractionType.COMMENT, "Nice post!", postId, Instant.now()));
        }
        return list;
    }
}

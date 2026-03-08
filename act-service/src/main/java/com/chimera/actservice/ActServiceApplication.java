package com.chimera.actservice;

import com.chimera.actservice.alert.HumanAlertService;
import com.chimera.actservice.exception.SuspiciousTransactionException;
import com.chimera.actservice.interaction.InteractionHandler;
import com.chimera.actservice.interaction.InteractionQueue;
import com.chimera.actservice.model.AudienceInteraction;
import com.chimera.actservice.model.InteractionType;
import com.chimera.actservice.model.PostResult;
import com.chimera.actservice.model.Transaction;
import com.chimera.actservice.model.TransactionStatus;
import com.chimera.actservice.model.TransactionType;
import com.chimera.actservice.placeholder.LearnService;
import com.chimera.actservice.publisher.ContentPublisher;
import com.chimera.actservice.publisher.InstagramPublisher;
import com.chimera.actservice.publisher.TikTokPublisher;
import com.chimera.actservice.publisher.TwitterPublisher;
import com.chimera.actservice.service.ActOrchestrator;
import com.chimera.actservice.transaction.TransactionLogger;
import com.chimera.actservice.transaction.TransactionManager;
import com.chimera.actservice.validator.ContentSpecValidator;
import com.chimera.contentcreator.model.ContentPiece;
import com.chimera.contentcreator.model.ContentType;
import com.chimera.trendwatcher.model.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entry point for the ACT standalone service.
 *
 * Wires all components and demonstrates:
 * - Content publishing with spec validation
 * - Interaction handling (queue, response window, LEARN forwarding)
 * - Transaction processing (logging, alert threshold, suspicious freeze)
 *
 * In production:
 * - Replace stub ContentPiece with live output from ContentCreatorService.
 * - Replace platform credentials with env vars / secrets manager.
 * - Replace LearnService.noOp() with the real LEARN service implementation.
 */
public class ActServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(ActServiceApplication.class);

    public static void main(String[] args) throws InterruptedException {
        log.info("=== Project Chimera — ACT service starting ===");

        // -----------------------------------------------------------------------
        // Infrastructure
        // -----------------------------------------------------------------------
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // -----------------------------------------------------------------------
        // Credentials (load from env vars in production)
        // -----------------------------------------------------------------------
        String twitterBearer  = System.getenv().getOrDefault("TWITTER_BEARER_TOKEN",  "REPLACE_ME");
        String tiktokToken    = System.getenv().getOrDefault("TIKTOK_ACCESS_TOKEN",   "REPLACE_ME");
        String instagramToken = System.getenv().getOrDefault("INSTAGRAM_ACCESS_TOKEN","REPLACE_ME");

        // -----------------------------------------------------------------------
        // Publishers
        // -----------------------------------------------------------------------
        Map<Platform, ContentPublisher> publishers = Map.of(
                Platform.TWITTER,   new TwitterPublisher(twitterBearer, httpClient),
                Platform.TIKTOK,    new TikTokPublisher(tiktokToken, httpClient),
                Platform.INSTAGRAM, new InstagramPublisher(instagramToken, httpClient)
        );

        // -----------------------------------------------------------------------
        // Supporting components
        // -----------------------------------------------------------------------
        HumanAlertService  alertService   = new HumanAlertService();
        TransactionLogger  txLogger       = new TransactionLogger();
        TransactionManager txManager      = new TransactionManager(txLogger, alertService);
        LearnService       learnService   = LearnService.noOp();
        InteractionQueue   interactionQ   = new InteractionQueue(alertService);
        InteractionHandler interactionH   = new InteractionHandler(interactionQ, learnService, alertService);

        // -----------------------------------------------------------------------
        // Orchestrator assembly
        // -----------------------------------------------------------------------
        ActOrchestrator orchestrator = new ActOrchestrator(
                publishers,
                new ContentSpecValidator(),
                interactionH,
                txManager,
                alertService
        );
        orchestrator.start();

        // -----------------------------------------------------------------------
        // Demo 1 — Publish content from CREATE (stub ContentPiece)
        // -----------------------------------------------------------------------
        ContentPiece piece = buildSampleContentPiece();
        log.info("--- DEMO: publishing content ---");
        List<PostResult> results = orchestrator.publishContent(piece);
        results.forEach(r -> log.info("PostResult: platform={} status={} contentPieceId={}",
                r.platform(), r.status(), r.contentPieceId()));

        // -----------------------------------------------------------------------
        // Demo 2 — Audience interactions
        // -----------------------------------------------------------------------
        log.info("--- DEMO: handling audience interactions ---");
        orchestrator.handleInteraction(buildInteraction("user-001", "alice", Platform.TWITTER,
                InteractionType.COMMENT, "Great post about #AI!", piece.id()));
        orchestrator.handleInteraction(buildInteraction("user-002", "bob", Platform.TIKTOK,
                InteractionType.MENTION, "@chimera love this content!", piece.id()));
        orchestrator.handleInteraction(buildInteraction("user-003", "carol", Platform.INSTAGRAM,
                InteractionType.DIRECT_MESSAGE, "Can you collaborate?", piece.id()));

        // -----------------------------------------------------------------------
        // Demo 3 — Financial transactions
        // -----------------------------------------------------------------------
        log.info("--- DEMO: processing transactions ---");

        // Normal transaction
        runTransaction(orchestrator, "user-001", TransactionType.PAYMENT,
                new BigDecimal("49.99"), "Monthly subscription");

        // High-value transaction (triggers human alert)
        runTransaction(orchestrator, "user-002", TransactionType.SPONSORSHIP,
                new BigDecimal("5000.00"), "Sponsorship deal");

        // Suspicious transaction (triggers freeze)
        runTransaction(orchestrator, "user-003", TransactionType.WITHDRAWAL,
                new BigDecimal("15000.00"), "Large withdrawal — should be frozen");

        // Wait briefly so interaction virtual threads can complete
        Thread.sleep(2000);

        log.info("=== ACT service demo complete ===");
    }

    // -------------------------------------------------------------------------
    // Sample data helpers
    // -------------------------------------------------------------------------

    private static ContentPiece buildSampleContentPiece() {
        return new ContentPiece(
                UUID.randomUUID().toString(),
                "TRENDING: #AI | Twitter/X, TikTok | 250.0K shares\nSource verified by PERCEIVE\n#AI #trending #chimera",
                ContentType.POST,
                "#AI",
                List.of("Twitter/X, TikTok (verified by PERCEIVE, report: 2026-03-08 14:31 UTC)"),
                List.of("#AI", "#trending", "#chimera"),
                List.of(Platform.TWITTER, Platform.TIKTOK, Platform.INSTAGRAM),
                Instant.now()
        );
    }

    private static AudienceInteraction buildInteraction(String userId, String username,
                                                         Platform platform, InteractionType type,
                                                         String content, String postId) {
        return new AudienceInteraction(
                UUID.randomUUID().toString(),
                userId, username, platform, type, content, postId, Instant.now()
        );
    }

    private static void runTransaction(ActOrchestrator orchestrator,
                                        String userId, TransactionType type,
                                        BigDecimal amount, String description) {
        Transaction tx = new Transaction(
                UUID.randomUUID().toString(),
                userId, type, amount, "USD", description,
                Instant.now(), TransactionStatus.PENDING
        );
        try {
            Transaction result = orchestrator.processTransaction(tx);
            log.info("TX DONE: id={} status={} amount={}", result.id(), result.status(), result.amount());
        } catch (SuspiciousTransactionException e) {
            log.error("TX FROZEN: {}", e.getMessage());
        }
    }
}

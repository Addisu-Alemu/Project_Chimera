package com.chimera.actservice.service;

import com.chimera.actservice.alert.HumanAlertService;
import com.chimera.actservice.exception.InvalidContentSpecException;
import com.chimera.actservice.exception.PublishException;
import com.chimera.actservice.exception.SuspiciousTransactionException;
import com.chimera.actservice.interaction.InteractionHandler;
import com.chimera.actservice.model.AudienceInteraction;
import com.chimera.actservice.model.PlatformState;
import com.chimera.actservice.model.PostResult;
import com.chimera.actservice.model.Transaction;
import com.chimera.actservice.publisher.ContentPublisher;
import com.chimera.actservice.transaction.TransactionManager;
import com.chimera.actservice.validator.ContentSpecValidator;
import com.chimera.contentcreator.model.ContentPiece;
import com.chimera.trendwatcher.model.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Core orchestrator of the ACT service.
 *
 * Responsibilities:
 * 1. Publish content — validate spec, publish to all target platforms with retry, health-check loop.
 * 2. Manage interactions — enqueue audience interactions for ordered, windowed processing.
 * 3. Handle transactions — log, alert, freeze, and complete financial transactions.
 *
 * Spec rules enforced:
 *
 * PUBLISHING
 * - Never post without valid spec from CREATE          → {@link ContentSpecValidator}
 * - All posts traceable to source spec                 → {@link PostResult#contentPieceId()}
 * - Post fails → retry 3× → alert human               → {@link #publishWithRetry}
 * - Platform API down → pause, log state, resume       → {@link #startHealthCheckLoop}
 *
 * INTERACTIONS
 * - Respond within defined time window                 → {@link InteractionHandler}
 * - High volume → queue and process in order           → {@link com.chimera.actservice.interaction.InteractionQueue}
 *
 * TRANSACTIONS
 * - Log every financial transaction                    → {@link TransactionManager} + {@link com.chimera.actservice.transaction.TransactionLogger}
 * - Alert human above threshold                        → {@link HumanAlertService#alertHighTransactionAmount}
 * - Suspicious → freeze and alert human               → {@link HumanAlertService#alertSuspiciousTransaction}
 *
 * All platform publishing uses Java 21 virtual threads.
 */
public class ActOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ActOrchestrator.class);

    private static final int      MAX_PUBLISH_RETRIES    = 3;
    private static final Duration HEALTH_CHECK_INTERVAL  = Duration.ofSeconds(60);

    private final Map<Platform, ContentPublisher> publishers;
    private final Map<Platform, PlatformState>    platformStates = new ConcurrentHashMap<>();
    private final ContentSpecValidator            validator;
    private final InteractionHandler              interactionHandler;
    private final TransactionManager             transactionManager;
    private final HumanAlertService              humanAlertService;

    public ActOrchestrator(Map<Platform, ContentPublisher> publishers,
                           ContentSpecValidator validator,
                           InteractionHandler interactionHandler,
                           TransactionManager transactionManager,
                           HumanAlertService humanAlertService) {
        this.publishers         = Map.copyOf(publishers);
        this.validator          = validator;
        this.interactionHandler = interactionHandler;
        this.transactionManager = transactionManager;
        this.humanAlertService  = humanAlertService;

        // Initialise all platforms as ACTIVE
        publishers.keySet().forEach(p -> platformStates.put(p, PlatformState.ACTIVE));
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /** Starts background loops: interaction processing and platform health checks. */
    public void start() {
        interactionHandler.start();
        startHealthCheckLoop();
        log.info("ActOrchestrator started with {} publishers", publishers.size());
    }

    // -------------------------------------------------------------------------
    // 1. Content publishing
    // -------------------------------------------------------------------------

    /**
     * Publishes a ContentPiece to all its target platforms concurrently using virtual threads.
     *
     * @throws InvalidContentSpecException if the piece fails spec validation.
     */
    public List<PostResult> publishContent(ContentPiece piece) {
        // Rule: never post without valid spec from CREATE
        validator.validate(piece);

        log.info("ACT: publishing contentPieceId={} type={} to platforms={}",
                piece.id(), piece.contentType(), piece.targetPlatforms());

        List<PostResult> results = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<PostResult>> futures = piece.targetPlatforms().stream()
                    .filter(publishers::containsKey)
                    .map(platform -> executor.submit(() -> publishWithRetry(piece, platform)))
                    .toList();

            for (Future<PostResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (ExecutionException e) {
                    log.error("ACT: publish task failed: {}", e.getCause().getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("ACT: publish interrupted", e);
                }
            }
        }

        long published = results.stream().filter(r -> r.status() == com.chimera.actservice.model.PostStatus.PUBLISHED).count();
        log.info("ACT: contentPieceId={} published to {}/{} platforms", piece.id(), published, results.size());
        return results;
    }

    // -------------------------------------------------------------------------
    // Retry logic — Rule: post fails → retry 3× → alert human
    // -------------------------------------------------------------------------

    private PostResult publishWithRetry(ContentPiece piece, Platform platform) {
        // Rule: if platform is PAUSED → skip and log state
        PlatformState state = platformStates.getOrDefault(platform, PlatformState.ACTIVE);
        if (state == PlatformState.PAUSED) {
            log.warn("ACT: platform={} is PAUSED — skipping contentPieceId={}, state logged",
                    platform, piece.id());
            return PostResult.paused(piece.id(), platform);
        }

        ContentPublisher publisher = publishers.get(platform);

        for (int attempt = 1; attempt <= MAX_PUBLISH_RETRIES; attempt++) {
            try {
                PostResult result = publisher.publish(piece);
                // Success — ensure platform is back to ACTIVE (recovery scenario)
                platformStates.put(platform, PlatformState.ACTIVE);
                log.info("ACT: PUBLISHED contentPieceId={} platform={} platformPostId={} attempt={}/{}",
                        piece.id(), platform, result.platformPostId(), attempt, MAX_PUBLISH_RETRIES);
                return result;
            } catch (PublishException e) {
                log.warn("ACT: publish attempt {}/{} failed for platform={}: {}",
                        attempt, MAX_PUBLISH_RETRIES, platform, e.getMessage());
            }
        }

        // All retries exhausted
        // Rule: platform API down → pause publishing and log state
        if (!publisher.isHealthy()) {
            platformStates.put(platform, PlatformState.PAUSED);
            humanAlertService.alertPlatformDown(platform);
            log.error("ACT: platform={} confirmed DOWN — publishing PAUSED, state logged. Resuming on recovery.", platform);
        }

        // Rule: post fails after MAX_RETRIES → alert human
        humanAlertService.alertPostFailure(piece, platform, MAX_PUBLISH_RETRIES);
        return PostResult.failed(piece.id(), platform, MAX_PUBLISH_RETRIES);
    }

    // -------------------------------------------------------------------------
    // Platform health-check loop — Rule: resume when API recovers
    // -------------------------------------------------------------------------

    private void startHealthCheckLoop() {
        Thread.ofVirtual()
                .name("platform-health-check")
                .start(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Thread.sleep(HEALTH_CHECK_INTERVAL);
                            recoverPausedPlatforms();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                });
    }

    private void recoverPausedPlatforms() {
        platformStates.forEach((platform, state) -> {
            if (state == PlatformState.PAUSED) {
                ContentPublisher publisher = publishers.get(platform);
                if (publisher != null && publisher.isHealthy()) {
                    platformStates.put(platform, PlatformState.ACTIVE);
                    log.info("ACT: platform={} has RECOVERED — publishing resumed", platform);
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // 2. Audience interaction management
    // -------------------------------------------------------------------------

    /**
     * Accepts an incoming audience interaction.
     * Rule: high volume → queued and processed in order within the response window.
     */
    public void handleInteraction(AudienceInteraction interaction) {
        interactionHandler.enqueue(interaction);
    }

    // -------------------------------------------------------------------------
    // 3. Financial transaction processing
    // -------------------------------------------------------------------------

    /**
     * Processes a financial transaction through all spec rules.
     * Rule: log every tx; alert on high value; freeze and alert on suspicious.
     *
     * @throws SuspiciousTransactionException if the transaction is frozen.
     */
    public Transaction processTransaction(Transaction transaction) {
        log.info("ACT: processing transaction id={} type={} amount={} {}",
                transaction.id(), transaction.type(), transaction.amount(), transaction.currency());
        return transactionManager.process(transaction);
    }
}

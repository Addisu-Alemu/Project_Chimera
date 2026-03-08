package com.chimera.actservice.interaction;

import com.chimera.actservice.alert.HumanAlertService;
import com.chimera.actservice.model.AudienceInteraction;
import com.chimera.actservice.model.Reply;
import com.chimera.actservice.placeholder.LearnService;
import com.chimera.trendwatcher.model.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Processes audience interactions from the {@link InteractionQueue} in FIFO order.
 *
 * Rules enforced:
 * - Must respond to audiences within the defined response window ({@value RESPONSE_WINDOW_MINUTES} min).
 * - Interaction data is forwarded to the LEARN service after each reply.
 *
 * Processing runs on a Java 21 virtual thread. Each individual interaction is also
 * dispatched to its own virtual thread to maximise throughput without blocking the loop.
 */
public class InteractionHandler {

    private static final Logger log = LoggerFactory.getLogger(InteractionHandler.class);

    /** Maximum time allowed between receiving an interaction and sending a reply. */
    private static final int RESPONSE_WINDOW_MINUTES = 30;
    private static final Duration RESPONSE_WINDOW = Duration.ofMinutes(RESPONSE_WINDOW_MINUTES);

    private final InteractionQueue   queue;
    private final LearnService       learnService;
    private final HumanAlertService  alertService;
    private final AtomicBoolean      running = new AtomicBoolean(false);

    public InteractionHandler(InteractionQueue queue,
                              LearnService learnService,
                              HumanAlertService alertService) {
        this.queue        = queue;
        this.learnService = learnService;
        this.alertService = alertService;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /** Starts the background processing loop on a named virtual thread. */
    public void start() {
        if (running.compareAndSet(false, true)) {
            Thread.ofVirtual()
                    .name("interaction-processor-loop")
                    .start(this::processLoop);
            log.info("InteractionHandler started — response window={}min", RESPONSE_WINDOW_MINUTES);
        }
    }

    public void stop() {
        running.set(false);
    }

    // -------------------------------------------------------------------------
    // Queue drain loop — runs on a virtual thread
    // -------------------------------------------------------------------------

    private void processLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                AudienceInteraction interaction = queue.poll(1, TimeUnit.SECONDS);
                if (interaction != null) {
                    // Dispatch each interaction to its own virtual thread — ordered intake, parallel processing
                    Thread.ofVirtual()
                            .name("interaction-" + interaction.id())
                            .start(() -> processInteraction(interaction));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.info("InteractionHandler stopped");
    }

    // -------------------------------------------------------------------------
    // Per-interaction processing
    // -------------------------------------------------------------------------

    private void processInteraction(AudienceInteraction interaction) {
        // Rule: check response window
        Duration age = Duration.between(interaction.receivedAt(), Instant.now());
        if (age.compareTo(RESPONSE_WINDOW) > 0) {
            long minutesLate = age.toMinutes();
            alertService.alertResponseWindowBreached(interaction.id(), interaction.platform(), minutesLate);
            log.warn("RESPONSE_WINDOW_BREACH: interaction id={} is {}min old (window={}min)",
                    interaction.id(), minutesLate, RESPONSE_WINDOW_MINUTES);
        }

        Optional<Reply> reply = generateReply(interaction);

        reply.ifPresentOrElse(
                r -> {
                    log.info("REPLY: interaction id={} platform={} type={} → '{}'",
                            interaction.id(), interaction.platform(), interaction.type(), r.body());
                    learnService.submitInteractionData(interaction, r);  // Rule: forward to LEARN
                },
                () -> log.debug("No reply generated for REACTION interaction id={}", interaction.id())
        );
    }

    // -------------------------------------------------------------------------
    // Reply generation — template-based; wire in an LLM for richer responses
    // -------------------------------------------------------------------------

    private Optional<Reply> generateReply(AudienceInteraction interaction) {
        String body = switch (interaction.type()) {
            case COMMENT ->
                    "Thanks for your comment! We appreciate your engagement. Stay tuned for more trending content.";
            case DIRECT_MESSAGE ->
                    "Thanks for reaching out! We've received your message and will follow up shortly.";
            case MENTION ->
                    "Thanks for the mention! We love seeing the community engage with our content.";
            case REACTION ->
                    null; // reactions don't require a text reply
        };

        if (body == null) return Optional.empty();

        Instant now = Instant.now();
        return Optional.of(new Reply(
                UUID.randomUUID().toString(),
                interaction.id(),
                body,
                interaction.platform(),
                now,
                now
        ));
    }

    // -------------------------------------------------------------------------
    // Public entry point for the orchestrator
    // -------------------------------------------------------------------------

    /** Accepts an incoming interaction and places it in the ordered queue. */
    public void enqueue(AudienceInteraction interaction) {
        log.info("INTERACTION: received id={} type={} platform={} from @{}",
                interaction.id(), interaction.type(), interaction.platform(), interaction.username());
        queue.enqueue(interaction);
    }
}

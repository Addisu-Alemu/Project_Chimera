package com.chimera.actservice.interaction;

import com.chimera.actservice.alert.HumanAlertService;
import com.chimera.actservice.model.AudienceInteraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bounded, thread-safe queue for incoming audience interactions.
 *
 * Rule: if interaction volume is too high → queue and process in order.
 *
 * When the queue reaches capacity, new interactions are dropped and a human alert
 * is dispatched after {@value OVERFLOW_ALERT_BATCH} consecutive drops.
 */
public class InteractionQueue {

    private static final Logger log = LoggerFactory.getLogger(InteractionQueue.class);

    /** Maximum interactions held in memory before overflow. */
    private static final int MAX_CAPACITY = 1_000;

    /** Alert human after this many consecutive dropped interactions. */
    private static final int OVERFLOW_ALERT_BATCH = 10;

    private final LinkedBlockingQueue<AudienceInteraction> queue =
            new LinkedBlockingQueue<>(MAX_CAPACITY);

    private final HumanAlertService alertService;
    private final AtomicInteger     droppedCount = new AtomicInteger(0);

    public InteractionQueue(HumanAlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * Enqueues an interaction for ordered processing.
     * Returns {@code true} if enqueued; {@code false} if dropped due to overflow.
     */
    public boolean enqueue(AudienceInteraction interaction) {
        boolean accepted = queue.offer(interaction);
        if (!accepted) {
            int dropped = droppedCount.incrementAndGet();
            log.warn("QUEUE OVERFLOW: interaction id={} from {} dropped (dropped={})",
                    interaction.id(), interaction.platform(), dropped);

            if (dropped % OVERFLOW_ALERT_BATCH == 0) {
                alertService.alertQueueOverflow(dropped);
                droppedCount.set(0);
            }
        }
        return accepted;
    }

    /**
     * Polls for the next interaction, blocking up to {@code timeout} with the given unit.
     * Returns {@code null} if no interaction arrives within the timeout.
     */
    public AudienceInteraction poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    public int size() {
        return queue.size();
    }
}

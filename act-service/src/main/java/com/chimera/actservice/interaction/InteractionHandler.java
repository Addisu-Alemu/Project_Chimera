package com.chimera.actservice.interaction;

import com.chimera.actservice.model.AudienceInteraction;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class InteractionHandler {

    private static final Logger log = LoggerFactory.getLogger(InteractionHandler.class);

    private final ConcurrentLinkedQueue<AudienceInteraction> queue = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void startProcessing() {
        Thread.ofVirtual()
                .name("interaction-processor")
                .start(this::processLoop);
        log.info("InteractionHandler: background processor started");
    }

    public void enqueue(AudienceInteraction interaction) {
        queue.add(interaction);
        log.info("InteractionHandler: enqueued type={} agentId={} postResultId={}",
                interaction.interactionType(), interaction.agentId(), interaction.postResultId());
    }

    private void processLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            AudienceInteraction interaction = queue.poll();
            if (interaction != null) {
                log.info("Processing interaction type={} agentId={} platform={}",
                        interaction.interactionType(), interaction.agentId(), interaction.platform());
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}

package com.chimera.actservice.controller;

import com.chimera.actservice.interaction.InteractionHandler;
import com.chimera.actservice.model.AudienceInteraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
public class InteractionController {

    private static final Logger log = LoggerFactory.getLogger(InteractionController.class);

    private final InteractionHandler interactionHandler;

    public InteractionController(InteractionHandler interactionHandler) {
        this.interactionHandler = interactionHandler;
    }

    @PostMapping("/interactions")
    public ResponseEntity<Void> receiveInteraction(@RequestBody Map<String, String> body) {
        UUID agentId = UUID.fromString(body.get("agentId"));
        UUID postResultId = UUID.fromString(body.get("postResultId"));
        String platform = body.get("platform");
        String interactionType = body.get("interactionType");
        String content = body.getOrDefault("content", "");

        log.info("POST /interactions agentId={} type={} platform={}", agentId, interactionType, platform);

        AudienceInteraction interaction = new AudienceInteraction(
                UUID.randomUUID(), agentId, postResultId, platform, interactionType, content, Instant.now()
        );
        interactionHandler.enqueue(interaction);
        return ResponseEntity.accepted().build();
    }
}

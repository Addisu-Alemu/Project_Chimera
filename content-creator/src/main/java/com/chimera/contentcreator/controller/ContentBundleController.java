package com.chimera.contentcreator.controller;

import com.chimera.contentcreator.client.dto.FeedbackReportDto;
import com.chimera.contentcreator.model.ContentBundle;
import com.chimera.contentcreator.service.ContentCreatorService;
import com.chimera.contentcreator.service.GenerationParametersStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class ContentBundleController {

    private static final Logger log = LoggerFactory.getLogger(ContentBundleController.class);

    private final ContentCreatorService contentCreatorService;
    private final GenerationParametersStore parametersStore;
    private final Map<UUID, ContentBundle> bundleStore = new ConcurrentHashMap<>();

    public ContentBundleController(ContentCreatorService contentCreatorService,
                                    GenerationParametersStore parametersStore) {
        this.contentCreatorService = contentCreatorService;
        this.parametersStore = parametersStore;
    }

    @PostMapping("/content-bundles")
    public ResponseEntity<Map<String, Object>> createBundle(
            @RequestParam UUID agentId,
            @RequestParam UUID trendReportId) {
        log.info("POST /content-bundles agentId={} trendReportId={}", agentId, trendReportId);
        ContentBundle bundle = contentCreatorService.generate(agentId, trendReportId);
        bundleStore.put(bundle.id(), bundle);
        return ResponseEntity.accepted().body(Map.of("bundleId", bundle.id(), "status", "READY"));
    }

    @GetMapping("/content-bundles/{bundleId}")
    public ResponseEntity<ContentBundle> getBundle(@PathVariable UUID bundleId) {
        log.info("GET /content-bundles/{}", bundleId);
        ContentBundle bundle = bundleStore.get(bundleId);
        if (bundle == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(bundle);
    }

    @PostMapping("/feedback-reports")
    public ResponseEntity<Void> receiveFeedback(@RequestBody FeedbackReportDto feedback) {
        log.info("POST /feedback-reports agentId={} confidence={} status={}",
                feedback.agentId(), feedback.confidenceScore(), feedback.reviewStatus());

        if ("HELD_PENDING_REVIEW".equals(feedback.reviewStatus())) {
            log.warn("Rejecting HELD_PENDING_REVIEW feedback for agentId={}", feedback.agentId());
            return ResponseEntity.unprocessableEntity().build();
        }

        parametersStore.applyFeedback(feedback);
        return ResponseEntity.noContent().build();
    }
}

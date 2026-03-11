package com.chimera.actservice.controller;

import com.chimera.actservice.model.PostResult;
import com.chimera.actservice.repository.PostResultRepository;
import com.chimera.actservice.service.ActService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
public class PublishController {

    private static final Logger log = LoggerFactory.getLogger(PublishController.class);

    private final ActService actService;
    private final PostResultRepository postResultRepository;

    public PublishController(ActService actService, PostResultRepository postResultRepository) {
        this.actService = actService;
        this.postResultRepository = postResultRepository;
    }

    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publish(@RequestBody Map<String, String> body) {
        UUID agentId = UUID.fromString(body.get("agentId"));
        UUID contentBundleId = UUID.fromString(body.get("contentBundleId"));
        String platform = body.get("platform");
        log.info("POST /publish agentId={} bundleId={} platform={}", agentId, contentBundleId, platform);

        PostResult result = actService.publish(agentId, contentBundleId, platform);
        return ResponseEntity.accepted().body(
                Map.of("postResultId", result.getId(), "status", result.getStatus())
        );
    }

    @GetMapping("/post-results/{postResultId}")
    public ResponseEntity<PostResult> getPostResult(@PathVariable UUID postResultId) {
        log.info("GET /post-results/{}", postResultId);
        return postResultRepository.findById(postResultId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

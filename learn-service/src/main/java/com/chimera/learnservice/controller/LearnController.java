package com.chimera.learnservice.controller;

import com.chimera.learnservice.model.FeedbackReport;
import com.chimera.learnservice.repository.FeedbackReportRepository;
import com.chimera.learnservice.service.LearnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
public class LearnController {

    private static final Logger log = LoggerFactory.getLogger(LearnController.class);

    private final LearnService learnService;
    private final FeedbackReportRepository feedbackReportRepository;

    public LearnController(LearnService learnService, FeedbackReportRepository feedbackReportRepository) {
        this.learnService = learnService;
        this.feedbackReportRepository = feedbackReportRepository;
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(@RequestBody Map<String, String> body) {
        UUID agentId = UUID.fromString(body.get("agentId"));
        UUID postResultId = UUID.fromString(body.get("postResultId"));
        UUID contentBundleId = body.containsKey("contentBundleId")
                ? UUID.fromString(body.get("contentBundleId")) : UUID.randomUUID();

        log.info("POST /analyze agentId={} postResultId={}", agentId, postResultId);
        FeedbackReport report = learnService.analyze(agentId, postResultId, contentBundleId);

        return ResponseEntity.accepted().body(Map.of(
                "feedbackReportId", report.getId(),
                "reviewStatus", report.getReviewStatus()
        ));
    }

    @PostMapping("/engagement-signals")
    public ResponseEntity<Void> receiveSignal(@RequestBody Map<String, Object> body) {
        UUID agentId = UUID.fromString((String) body.get("agentId"));
        UUID postResultId = UUID.fromString((String) body.get("postResultId"));
        String signalType = (String) body.get("signalType");
        long value = body.containsKey("value") ? ((Number) body.get("value")).longValue() : 1L;

        log.info("POST /engagement-signals agentId={} postResultId={} type={}", agentId, postResultId, signalType);
        learnService.persistSignal(agentId, postResultId, signalType, value);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/feedback-reports/{reportId}")
    public ResponseEntity<FeedbackReport> getFeedbackReport(@PathVariable UUID reportId) {
        log.info("GET /feedback-reports/{}", reportId);
        return feedbackReportRepository.findById(reportId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/feedback-reports/{reportId}/approve")
    public ResponseEntity<Void> approveFeedbackReport(@PathVariable UUID reportId) {
        log.info("POST /feedback-reports/{}/approve", reportId);
        var opt = feedbackReportRepository.findById(reportId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var report = opt.get();
        report.setDispatchedAt(Instant.now());
        feedbackReportRepository.save(report);
        return ResponseEntity.<Void>accepted().build();
    }
}

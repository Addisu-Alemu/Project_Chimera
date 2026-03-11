package com.chimera.contentcreator.service;

import com.chimera.contentcreator.client.dto.FeedbackReportDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GenerationParametersStore {

    private static final Logger log = LoggerFactory.getLogger(GenerationParametersStore.class);

    private final Map<UUID, Double> confidenceByAgent = new ConcurrentHashMap<>();

    public void applyFeedback(FeedbackReportDto feedback) {
        if (!"AUTO_DISPATCHED".equals(feedback.reviewStatus()) &&
            !"HUMAN_APPROVED".equals(feedback.reviewStatus())) {
            log.warn("Rejecting feedback with status={} for agentId={}", feedback.reviewStatus(), feedback.agentId());
            return;
        }
        confidenceByAgent.put(feedback.agentId(), feedback.confidenceScore());
        log.info("GenerationParameters updated for agentId={} confidence={}", feedback.agentId(), feedback.confidenceScore());
    }

    public double getConfidenceScore(UUID agentId) {
        return confidenceByAgent.getOrDefault(agentId, 0.7);
    }
}

package com.chimera.actservice.client;

import com.chimera.actservice.client.dto.EngagementSignalDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

@Component
public class LearnServiceClient {

    private static final Logger log = LoggerFactory.getLogger(LearnServiceClient.class);

    private final WebClient webClient;

    public LearnServiceClient(@Value("${learn.service.url:http://localhost:8084}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public void sendEngagementSignal(EngagementSignalDto signal) {
        log.info("Sending engagement signal postResultId={} type={}", signal.postResultId(), signal.signalType());
        webClient.post()
                .uri("/engagement-signals")
                .bodyValue(signal)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                    r -> log.info("Engagement signal sent"),
                    e -> log.error("Failed to send engagement signal: {}", e.getMessage())
                );
    }

    public void triggerAnalysis(UUID agentId, UUID postResultId) {
        log.info("Triggering analysis agentId={} postResultId={}", agentId, postResultId);
        webClient.post()
                .uri("/analyze")
                .bodyValue(Map.of("agentId", agentId, "postResultId", postResultId))
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                    r -> log.info("Analysis triggered"),
                    e -> log.error("Failed to trigger analysis: {}", e.getMessage())
                );
    }
}

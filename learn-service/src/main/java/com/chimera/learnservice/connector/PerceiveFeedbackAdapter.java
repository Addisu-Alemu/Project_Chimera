package com.chimera.learnservice.connector;

import com.chimera.learnservice.model.TrendSignalDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PerceiveFeedbackAdapter {

    private static final Logger log = LoggerFactory.getLogger(PerceiveFeedbackAdapter.class);

    private final WebClient webClient;

    public PerceiveFeedbackAdapter(@Value("${trend.watcher.url:http://localhost:8081}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public void dispatch(TrendSignalDto signal) {
        log.info("Dispatching TrendSignal to PERCEIVE agentId={}", signal.agentId());
        webClient.post()
                .uri("/trend-signals")
                .bodyValue(signal)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                    r -> log.info("TrendSignal dispatched to trend-watcher"),
                    e -> log.error("Failed to dispatch TrendSignal: {}", e.getMessage())
                );
    }
}

package com.chimera.learnservice.connector;

import com.chimera.learnservice.model.FeedbackReport;
import com.chimera.learnservice.model.FeedbackReportDto;
import com.chimera.learnservice.model.ReviewStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

@Component
public class CreateFeedbackAdapter {

    private static final Logger log = LoggerFactory.getLogger(CreateFeedbackAdapter.class);

    private final WebClient webClient;

    public CreateFeedbackAdapter(@Value("${content.creator.url:http://localhost:8082}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public void dispatch(FeedbackReport report) {
        if (report.getReviewStatus() != ReviewStatus.AUTO_DISPATCHED) {
            log.warn("Skipping dispatch for report id={} status={}", report.getId(), report.getReviewStatus());
            return;
        }
        log.info("Dispatching FeedbackReport to CREATE id={}", report.getId());

        FeedbackReportDto dto = new FeedbackReportDto(
                report.getId(), report.getAgentId(), report.getContentBundleId(),
                report.getConfidenceScore(), report.getLikes(), report.getShares(),
                report.getComments(), report.getViews(), report.getClickThroughRate(),
                report.getReviewStatus(), report.getGeneratedAt()
        );

        report.setDispatchedAt(Instant.now());

        webClient.post()
                .uri("/feedback-reports")
                .bodyValue(dto)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                    r -> log.info("FeedbackReport dispatched to content-creator"),
                    e -> log.error("Failed to dispatch FeedbackReport: {}", e.getMessage())
                );
    }
}

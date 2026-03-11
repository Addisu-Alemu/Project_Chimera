package com.chimera.learnservice.service;

import com.chimera.learnservice.analyzer.EngagementSummary;
import com.chimera.learnservice.model.FeedbackReport;
import com.chimera.learnservice.model.ReviewStatus;
import com.chimera.learnservice.repository.FeedbackReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class ReportBuilder {

    private static final Logger log = LoggerFactory.getLogger(ReportBuilder.class);
    private static final double CONFIDENCE_THRESHOLD = 0.6;

    private final FeedbackReportRepository repository;

    public ReportBuilder(FeedbackReportRepository repository) {
        this.repository = repository;
    }

    public FeedbackReport build(UUID agentId, UUID contentBundleId,
                                 EngagementSummary summary, double confidenceScore) {
        ReviewStatus status = confidenceScore >= CONFIDENCE_THRESHOLD
                ? ReviewStatus.AUTO_DISPATCHED
                : ReviewStatus.HELD_PENDING_REVIEW;

        if (status == ReviewStatus.HELD_PENDING_REVIEW) {
            log.warn("FeedbackReport held for human review: confidenceScore={} agentId={}", confidenceScore, agentId);
        }

        FeedbackReport report = new FeedbackReport(
                UUID.randomUUID(),
                agentId,
                contentBundleId,
                BigDecimal.valueOf(confidenceScore).setScale(3, RoundingMode.HALF_UP),
                summary.likes(),
                summary.shares(),
                summary.comments(),
                summary.views(),
                BigDecimal.valueOf(summary.clickThroughRate()).setScale(4, RoundingMode.HALF_UP),
                status,
                Instant.now()
        );

        FeedbackReport saved = repository.save(report);
        log.info("FeedbackReport saved id={} confidence={} status={}", saved.getId(), confidenceScore, status);
        return saved;
    }
}

package com.chimera.learnservice.service;

import com.chimera.learnservice.alert.HumanAlertService;
import com.chimera.learnservice.analyzer.ConfidenceScorer;
import com.chimera.learnservice.analyzer.EngagementSummary;
import com.chimera.learnservice.analyzer.PerformanceAnalyzer;
import com.chimera.learnservice.connector.CreateFeedbackAdapter;
import com.chimera.learnservice.connector.PerceiveFeedbackAdapter;
import com.chimera.learnservice.model.AlertType;
import com.chimera.learnservice.model.EngagementSignal;
import com.chimera.learnservice.model.FeedbackReport;
import com.chimera.learnservice.model.ReviewStatus;
import com.chimera.learnservice.model.SignalType;
import com.chimera.learnservice.model.TrendSignalDto;
import com.chimera.learnservice.repository.EngagementSignalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class LearnService {

    private static final Logger log = LoggerFactory.getLogger(LearnService.class);
    private static final double CONFIDENCE_THRESHOLD = 0.6;

    private final EngagementSignalRepository signalRepository;
    private final PerformanceAnalyzer performanceAnalyzer;
    private final ConfidenceScorer confidenceScorer;
    private final ReportBuilder reportBuilder;
    private final PerceiveFeedbackAdapter perceiveAdapter;
    private final CreateFeedbackAdapter createAdapter;
    private final HumanAlertService humanAlertService;

    public LearnService(EngagementSignalRepository signalRepository,
                         PerformanceAnalyzer performanceAnalyzer,
                         ConfidenceScorer confidenceScorer,
                         ReportBuilder reportBuilder,
                         PerceiveFeedbackAdapter perceiveAdapter,
                         CreateFeedbackAdapter createAdapter,
                         HumanAlertService humanAlertService) {
        this.signalRepository = signalRepository;
        this.performanceAnalyzer = performanceAnalyzer;
        this.confidenceScorer = confidenceScorer;
        this.reportBuilder = reportBuilder;
        this.perceiveAdapter = perceiveAdapter;
        this.createAdapter = createAdapter;
        this.humanAlertService = humanAlertService;
    }

    @Transactional
    public UUID persistSignal(UUID agentId, UUID postResultId, String signalType, long value) {
        EngagementSignal signal = new EngagementSignal(
                UUID.randomUUID(), agentId, postResultId,
                SignalType.valueOf(signalType.toUpperCase()), value
        );
        return signalRepository.save(signal).getId();
    }

    @Transactional
    public FeedbackReport analyze(UUID agentId, UUID postResultId, UUID contentBundleId) {
        log.info("LearnService.analyze agentId={} postResultId={}", agentId, postResultId);

        EngagementSummary summary = performanceAnalyzer.analyze(postResultId);
        double confidence = confidenceScorer.score(summary);
        log.info("Confidence score={} for postResultId={}", confidence, postResultId);

        FeedbackReport report = reportBuilder.build(agentId, contentBundleId, summary, confidence);

        TrendSignalDto trendSignal = new TrendSignalDto(
                UUID.randomUUID(), agentId, report.getId(),
                Map.of("confidence", confidence),
                Instant.now()
        );
        perceiveAdapter.dispatch(trendSignal);

        if (report.getReviewStatus() == ReviewStatus.HELD_PENDING_REVIEW) {
            humanAlertService.raise(
                    agentId, AlertType.LOW_CONFIDENCE, report.getId(),
                    "/feedback-reports/" + report.getId(),
                    "0.6", String.valueOf(confidence)
            );
        } else {
            createAdapter.dispatch(report);
        }

        return report;
    }
}

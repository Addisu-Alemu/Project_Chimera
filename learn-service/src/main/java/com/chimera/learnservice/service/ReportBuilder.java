package com.chimera.learnservice.service;

import com.chimera.learnservice.memory.PerformanceMemory;
import com.chimera.learnservice.model.ContentPerformance;
import com.chimera.learnservice.model.FeedbackReport;
import com.chimera.trendwatcher.model.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Assembles a {@link FeedbackReport} from the current state of {@link PerformanceMemory}.
 *
 * Called by {@link LearnOrchestrator} on a scheduled cadence and after significant signals.
 */
public class ReportBuilder {

    private static final Logger log = LoggerFactory.getLogger(ReportBuilder.class);
    private static final int TOP_PER_PLATFORM = 5;

    private final PerformanceMemory memory;

    public ReportBuilder(PerformanceMemory memory) {
        this.memory = memory;
    }

    /**
     * Builds and returns a point-in-time {@link FeedbackReport} from live memory.
     */
    public FeedbackReport build() {
        Map<Platform, List<ContentPerformance>> topByPlatform =
                memory.getTopPerformersByAllPlatforms(TOP_PER_PLATFORM);

        List<String> topTopics          = memory.getTopPerformingTopics();
        List<String> underTopics        = memory.getUnderperformingTopics();
        List<ContentPerformance> negatives = memory.getNegativePerformers();
        List<String> negativeIds        = negatives.stream()
                .map(ContentPerformance::contentPieceId)
                .distinct()
                .collect(Collectors.toList());

        double overallScore = memory.getOverallEngagementScore();

        FeedbackReport report = new FeedbackReport(
                UUID.randomUUID().toString(),
                topByPlatform,
                topTopics,
                underTopics,
                negativeIds,
                overallScore,
                memory.getEngagementByContentType(),
                memory.totalTracked(),
                negatives.size(),
                Instant.now()
        );

        log.info("REPORT: id={} totalTracked={} overallScore={} topTopics={} underperforming={} negatives={}",
                report.id(), report.totalContentAnalyzed(), String.format("%.3f", report.overallEngagementScore()),
                report.topPerformingTopics().size(), report.underperformingTopics().size(),
                report.negativePerformanceCount());

        return report;
    }
}

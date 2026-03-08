package com.chimera.contentcreator;

import com.chimera.contentcreator.filter.ContentSafetyFilter;
import com.chimera.contentcreator.generator.TemplateContentGenerator;
import com.chimera.contentcreator.model.ContentPiece;
import com.chimera.contentcreator.placeholder.ActService;
import com.chimera.contentcreator.placeholder.FeedbackData;
import com.chimera.contentcreator.service.ContentCreatorService;
import com.chimera.trendwatcher.model.Platform;
import com.chimera.trendwatcher.model.TimeRange;
import com.chimera.trendwatcher.model.TopCategory;
import com.chimera.trendwatcher.model.TrendReport;
import com.chimera.trendwatcher.model.TrendingTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Entry point for the ContentCreator (CREATE) standalone service.
 *
 * Wires all components and demonstrates the full pipeline:
 *   PERCEIVE TrendReport → CREATE ContentPieces → ACT (stub)
 *
 * In production:
 * - Replace the stub TrendReport with a live call to the TrendWatcher service.
 * - Replace FeedbackData.neutral() with a live feed from the LEARN service.
 * - Replace ActService.noOp() with the real ACT service implementation.
 */
public class ContentCreatorApplication {

    private static final Logger log = LoggerFactory.getLogger(ContentCreatorApplication.class);

    public static void main(String[] args) {
        log.info("=== Project Chimera — ContentCreator (CREATE) starting ===");

        // -----------------------------------------------------------------------
        // Service assembly
        // -----------------------------------------------------------------------
        ContentCreatorService service = new ContentCreatorService(
                new TemplateContentGenerator(),
                new ContentSafetyFilter(),
                ActService.noOp()           // Replace with real ACT service when available
        );

        // -----------------------------------------------------------------------
        // Input: TrendReport from PERCEIVE
        // In production, call TrendWatcherService.generateReport(TimeRange.DAILY)
        // -----------------------------------------------------------------------
        TrendReport report = buildSampleReport();

        // -----------------------------------------------------------------------
        // Feedback: from LEARN service
        // In production, inject real FeedbackData from the LEARN service
        // -----------------------------------------------------------------------
        FeedbackData feedback = FeedbackData.neutral();

        // -----------------------------------------------------------------------
        // Run
        // -----------------------------------------------------------------------
        try {
            List<ContentPiece> pieces = service.createContent(report, feedback);
            log.info("=== CREATE complete: {} pieces ready for ACT ===", pieces.size());
            pieces.forEach(ContentCreatorApplication::printPiece);
        } catch (com.chimera.contentcreator.exception.StaleTrendReportException e) {
            log.error("TrendReport is stale — re-triggering PERCEIVE: {}", e.getMessage());
            // In production: call TrendWatcherService.generateReport() and retry
        }

        log.info("=== ContentCreator finished ===");
    }

    // ---------------------------------------------------------------------------
    // Sample data — replace with live PERCEIVE output in production
    // ---------------------------------------------------------------------------

    private static TrendReport buildSampleReport() {
        List<TrendingTopic> topics = List.of(
                new TrendingTopic("#AI", 250_000, List.of(Platform.TWITTER, Platform.TIKTOK)),
                new TrendingTopic("#ClimateAction", 180_000, List.of(Platform.INSTAGRAM, Platform.TWITTER)),
                new TrendingTopic("#WorldCup2026", 420_000,
                        List.of(Platform.TWITTER, Platform.TIKTOK, Platform.INSTAGRAM))
        );

        List<TopCategory> categories = List.of(
                new TopCategory("Technology",   5_000_000, 1200),
                new TopCategory("Sports",       8_000_000, 2100),
                new TopCategory("Environment",  3_200_000,  900)
        );

        return new TrendReport(topics, categories, TimeRange.DAILY, Instant.now());
    }

    // ---------------------------------------------------------------------------
    // Simple printer
    // ---------------------------------------------------------------------------

    private static void printPiece(ContentPiece piece) {
        log.info("──────────────────────────────────────────────────────");
        log.info("  id          : {}", piece.id());
        log.info("  type        : {}", piece.contentType());
        log.info("  topic       : {}", piece.topic());
        log.info("  platforms   : {}", piece.targetPlatforms());
        log.info("  generatedAt : {}", piece.generatedAt());
        log.info("  sources     : {}", piece.sourceReferences());
        log.info("  body:\n{}", piece.body());
        log.info("──────────────────────────────────────────────────────");
    }
}

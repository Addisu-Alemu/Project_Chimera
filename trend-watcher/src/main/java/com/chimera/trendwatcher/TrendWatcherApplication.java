package com.chimera.trendwatcher;

import com.chimera.trendwatcher.fetcher.InstagramFetcher;
import com.chimera.trendwatcher.fetcher.SocialMediaFetcher;
import com.chimera.trendwatcher.fetcher.TikTokFetcher;
import com.chimera.trendwatcher.fetcher.TwitterFetcher;
import com.chimera.trendwatcher.filter.ContentSafetyFilter;
import com.chimera.trendwatcher.model.Platform;
import com.chimera.trendwatcher.model.TimeRange;
import com.chimera.trendwatcher.model.TrendReport;
import com.chimera.trendwatcher.service.TrendAggregator;
import com.chimera.trendwatcher.service.TrendWatcherService;
import com.chimera.trendwatcher.verifier.SourceVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Entry point for the TrendWatcher standalone service.
 *
 * Wires all components together and runs both DAILY and WEEKLY trend reports.
 * In production, replace credential placeholders with environment variables
 * or a secrets manager, and schedule this via a cron/scheduler.
 */
public class TrendWatcherApplication {

    private static final Logger log = LoggerFactory.getLogger(TrendWatcherApplication.class);

    public static void main(String[] args) {
        log.info("=== Project Chimera — TrendWatcher starting ===");

        // -----------------------------------------------------------------------
        // Infrastructure
        // -----------------------------------------------------------------------
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // -----------------------------------------------------------------------
        // Credentials (load from env vars in production)
        // -----------------------------------------------------------------------
        String twitterBearer   = System.getenv().getOrDefault("TWITTER_BEARER_TOKEN", "REPLACE_ME");
        String tiktokToken     = System.getenv().getOrDefault("TIKTOK_ACCESS_TOKEN",  "REPLACE_ME");
        String instagramToken  = System.getenv().getOrDefault("INSTAGRAM_ACCESS_TOKEN","REPLACE_ME");

        // -----------------------------------------------------------------------
        // Fetchers (primary + backup fallback baked in per fetcher)
        // -----------------------------------------------------------------------
        List<SocialMediaFetcher> fetchers = List.of(
                new TwitterFetcher(twitterBearer,  httpClient),
                new TikTokFetcher(tiktokToken,     httpClient),
                new InstagramFetcher(instagramToken, httpClient)
        );

        // -----------------------------------------------------------------------
        // Validation components
        // -----------------------------------------------------------------------

        // Verified-source registry — seed with known verified handles per platform.
        // Use SourceVerifier.allowAll() to skip verification during development.
        SourceVerifier sourceVerifier = new SourceVerifier(Map.of(
                Platform.TWITTER,   Set.of("@bbc", "@cnn", "@reuters", "@apnews"),
                Platform.TIKTOK,    Set.of("@bbcnews", "@cnn", "@cbsnews"),
                Platform.INSTAGRAM, Set.of("@bbcnews", "@cnn", "@natgeo")
        ));

        ContentSafetyFilter safetyFilter = new ContentSafetyFilter();

        // -----------------------------------------------------------------------
        // Service assembly
        // -----------------------------------------------------------------------
        TrendWatcherService service = new TrendWatcherService(
                fetchers,
                safetyFilter,
                sourceVerifier,
                new TrendAggregator()
        );

        // -----------------------------------------------------------------------
        // Run reports
        // -----------------------------------------------------------------------
        for (TimeRange range : TimeRange.values()) {
            try {
                TrendReport report = service.generateReport(range);
                printReport(report);
            } catch (Exception e) {
                log.error("Failed to generate {} report: {}", range, e.getMessage(), e);
            }
        }

        log.info("=== TrendWatcher complete ===");
    }

    // ---------------------------------------------------------------------------
    // Simple console printer — replace with serialisation / downstream sink
    // ---------------------------------------------------------------------------

    private static void printReport(TrendReport report) {
        log.info("──────────────────────────────────────────────────────");
        log.info("TREND REPORT  range={}  generatedAt={}", report.timeRange(), report.generatedAt());
        log.info("  Trending Topics ({}):", report.trendingTopics().size());
        report.trendingTopics().forEach(t ->
                log.info("    {} | shares={} | platforms={}", t.topic(), t.totalShares(), t.platforms()));
        log.info("  Top Categories ({}):", report.topCategories().size());
        report.topCategories().forEach(c ->
                log.info("    {} | views={} | posts={}", c.name(), c.totalViews(), c.contentCount()));
        log.info("──────────────────────────────────────────────────────");
    }
}

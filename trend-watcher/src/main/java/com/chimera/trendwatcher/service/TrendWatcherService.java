package com.chimera.trendwatcher.service;

import com.chimera.trendwatcher.fetcher.FetchException;
import com.chimera.trendwatcher.fetcher.SocialMediaFetcher;
import com.chimera.trendwatcher.filter.ContentSafetyFilter;
import com.chimera.trendwatcher.model.RawContent;
import com.chimera.trendwatcher.model.TimeRange;
import com.chimera.trendwatcher.model.TrendReport;
import com.chimera.trendwatcher.verifier.SourceVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Core orchestrator of the TrendWatcher service.
 *
 * Spec rules enforced here:
 *
 * 1. News data must not be more than 24 hours old — checked via {@code fetchedAt}.
 * 2. Source must be verified — checked via {@link SourceVerifier}.
 * 3. Content must pass safety filter — checked via {@link ContentSafetyFilter}.
 * 4. If source is down → fall back to backup — handled inside each {@link SocialMediaFetcher}.
 * 5. If content is harmful → skip and log — enforced inside {@link ContentSafetyFilter}.
 * 6. If data is stale → discard and retry — implemented in {@link #fetchWithRetry}.
 *
 * All platform fetches run in parallel on Java 21 virtual threads.
 */
public class TrendWatcherService {

    private static final Logger log = LoggerFactory.getLogger(TrendWatcherService.class);

    /** Maximum age of a fetched payload before it is considered stale and retried. */
    private static final Duration STALE_THRESHOLD = Duration.ofHours(24);

    /** How many times to retry a fetcher that returns only stale data. */
    private static final int MAX_RETRIES = 3;

    private final List<SocialMediaFetcher> fetchers;
    private final ContentSafetyFilter      safetyFilter;
    private final SourceVerifier           sourceVerifier;
    private final TrendAggregator          aggregator;

    public TrendWatcherService(
            List<SocialMediaFetcher> fetchers,
            ContentSafetyFilter safetyFilter,
            SourceVerifier sourceVerifier,
            TrendAggregator aggregator) {
        this.fetchers       = List.copyOf(fetchers);
        this.safetyFilter   = safetyFilter;
        this.sourceVerifier = sourceVerifier;
        this.aggregator     = aggregator;
    }

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /**
     * Fetches content from all platforms concurrently using virtual threads,
     * validates it against all spec rules, and returns an aggregated trend report.
     *
     * @param range The time window (DAILY or WEEKLY) to analyse.
     * @return A fully-populated {@link TrendReport}.
     */
    public TrendReport generateReport(TimeRange range) {
        log.info("=== Generating {} trend report ===", range);

        List<RawContent> rawContent = fetchAllPlatformsConcurrently(range);
        List<RawContent> validated  = validate(rawContent);

        log.info("Validation: {}/{} items passed all rules", validated.size(), rawContent.size());
        return aggregator.aggregate(validated, range);
    }

    // ---------------------------------------------------------------------------
    // Step 1 — Concurrent fetch with virtual threads
    // ---------------------------------------------------------------------------

    private List<RawContent> fetchAllPlatformsConcurrently(TimeRange range) {
        List<RawContent> combined = new ArrayList<>();

        // Java 21: one virtual thread per platform fetch — lightweight, no thread pool tuning needed
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<List<RawContent>>> futures = fetchers.stream()
                    .map(fetcher -> executor.submit(() -> fetchWithRetry(fetcher, range)))
                    .toList();

            for (Future<List<RawContent>> future : futures) {
                try {
                    combined.addAll(future.get());
                } catch (ExecutionException e) {
                    log.error("Platform fetch task failed: {}", e.getCause().getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Fetch interrupted", e);
                }
            }
        }

        log.info("Fetched {} raw items from {} platforms", combined.size(), fetchers.size());
        return combined;
    }

    // ---------------------------------------------------------------------------
    // Step 2 — Retry loop: discard stale data and re-fetch
    // ---------------------------------------------------------------------------

    /**
     * Fetches from a single platform, retrying if the response contains only stale content.
     *
     * Rule: if data is stale → discard and retry.
     */
    private List<RawContent> fetchWithRetry(SocialMediaFetcher fetcher, TimeRange range) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                List<RawContent> items = fetcher.fetch(range);
                List<RawContent> fresh = items.stream()
                        .filter(this::isFresh)
                        .toList();

                if (!fresh.isEmpty()) {
                    log.info("[{}] Attempt {}: got {}/{} fresh items",
                            fetcher.platform(), attempt, fresh.size(), items.size());
                    return fresh;
                }

                log.warn("[{}] Attempt {}/{}: all {} items are stale — retrying",
                        fetcher.platform(), attempt, MAX_RETRIES, items.size());

            } catch (FetchException e) {
                log.error("[{}] Attempt {}/{}: fetch failed — {}",
                        fetcher.platform(), attempt, MAX_RETRIES, e.getMessage());
            }
        }

        log.error("[{}] Exhausted {} retries, returning empty list", fetcher.platform(), MAX_RETRIES);
        return List.of();
    }

    // ---------------------------------------------------------------------------
    // Step 3 — Validate: freshness + verified source + safety
    // ---------------------------------------------------------------------------

    /**
     * Applies all content-level spec rules in sequence.
     * Items that fail any rule are dropped (and logged where required by spec).
     */
    private List<RawContent> validate(List<RawContent> content) {
        return content.stream()
                .filter(this::isFresh)           // Rule 1: not older than 24 h
                .filter(this::isVerifiedSource)  // Rule 2: verified account
                .filter(this::isSafe)            // Rule 3: passes safety filter
                .toList();
    }

    /** Rule 1: fetchedAt must be within the last 24 hours. */
    private boolean isFresh(RawContent content) {
        boolean fresh = content.fetchedAt().isAfter(Instant.now().minus(STALE_THRESHOLD));
        if (!fresh) {
            log.debug("STALE: discarding id={} platform={} fetchedAt={}",
                    content.id(), content.platform(), content.fetchedAt());
        }
        return fresh;
    }

    /** Rule 2: source handle must be on the verified list for its platform. */
    private boolean isVerifiedSource(RawContent content) {
        boolean verified = sourceVerifier.isVerified(content.sourceHandle(), content.platform());
        if (!verified) {
            log.debug("UNVERIFIED: skipping id={} handle={} platform={}",
                    content.id(), content.sourceHandle(), content.platform());
        }
        return verified;
    }

    /**
     * Rule 3: content must pass the safety filter.
     * Logging of harmful content is handled inside {@link ContentSafetyFilter#isSafe}.
     */
    private boolean isSafe(RawContent content) {
        return safetyFilter.isSafe(content);
    }
}

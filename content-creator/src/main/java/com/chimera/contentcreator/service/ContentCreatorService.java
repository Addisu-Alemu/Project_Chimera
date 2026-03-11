package com.chimera.contentcreator.service;

import com.chimera.contentcreator.client.TrendWatcherClient;
import com.chimera.contentcreator.client.dto.TrendReportDto;
import com.chimera.contentcreator.exception.ContentSafetyException;
import com.chimera.contentcreator.exception.StaleTrendReportException;
import com.chimera.contentcreator.filter.ContentSafetyFilter;
import com.chimera.contentcreator.generator.TemplateContentGenerator;
import com.chimera.contentcreator.model.ContentBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class ContentCreatorService {

    private static final Logger log = LoggerFactory.getLogger(ContentCreatorService.class);
    private static final Duration REPORT_MAX_AGE = Duration.ofHours(24);
    private static final int MAX_SAFETY_RETRIES = 3;

    private final TrendWatcherClient trendWatcherClient;
    private final TemplateContentGenerator generator;
    private final ContentSafetyFilter safetyFilter;

    public ContentCreatorService(TrendWatcherClient trendWatcherClient,
                                  TemplateContentGenerator generator,
                                  ContentSafetyFilter safetyFilter) {
        this.trendWatcherClient = trendWatcherClient;
        this.generator = generator;
        this.safetyFilter = safetyFilter;
    }

    public ContentBundle generate(UUID agentId, UUID trendReportId) {
        log.info("ContentCreatorService.generate agentId={} trendReportId={}", agentId, trendReportId);

        TrendReportDto report = trendWatcherClient.getReport(trendReportId);
        validateFreshness(report);

        for (int attempt = 1; attempt <= MAX_SAFETY_RETRIES; attempt++) {
            ContentBundle bundle = generator.generate(agentId, report);
            if (safetyFilter.passes(bundle.caption()) && safetyFilter.passes(bundle.videoDescription())) {
                ContentBundle safe = new ContentBundle(
                        bundle.id(), bundle.agentId(), bundle.trendReportId(),
                        bundle.caption(), bundle.hashtags(), bundle.videoDescription(),
                        Instant.now(), bundle.generatedAt()
                );
                log.info("ContentBundle generated id={} attempt={}", safe.id(), attempt);
                return safe;
            }
            log.warn("Safety filter failed for attempt {}/{}", attempt, MAX_SAFETY_RETRIES);
        }

        log.error("All {} safety retries exhausted for agentId={} trendReportId={}", MAX_SAFETY_RETRIES, agentId, trendReportId);
        throw new ContentSafetyException("Content failed safety filter after " + MAX_SAFETY_RETRIES + " attempts");
    }

    private void validateFreshness(TrendReportDto report) {
        if (report.fetchedAt().isBefore(Instant.now().minus(REPORT_MAX_AGE))) {
            throw new StaleTrendReportException("TrendReport is stale: fetchedAt=" + report.fetchedAt());
        }
    }
}

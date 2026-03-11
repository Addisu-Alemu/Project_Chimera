package com.chimera.trendwatcher.service;

import com.chimera.trendwatcher.model.TrendReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TrendReportCacheService {

    private static final Logger log = LoggerFactory.getLogger(TrendReportCacheService.class);

    private final TrendWatcherService trendWatcherService;

    public TrendReportCacheService(TrendWatcherService trendWatcherService) {
        this.trendWatcherService = trendWatcherService;
    }

    @Cacheable(value = "trend-reports", key = "#agentId")
    public TrendReport getOrCreate(UUID agentId) {
        log.info("Cache miss for agentId={}, generating new report", agentId);
        return trendWatcherService.generateReport(agentId);
    }
}

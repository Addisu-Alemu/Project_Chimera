package com.chimera.trendwatcher.controller;

import com.chimera.trendwatcher.cache.TrendSignalStore;
import com.chimera.trendwatcher.model.TrendReport;
import com.chimera.trendwatcher.model.TrendSignal;
import com.chimera.trendwatcher.service.TrendReportCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping
public class TrendReportController {

    private static final Logger log = LoggerFactory.getLogger(TrendReportController.class);

    private final TrendReportCacheService cacheService;
    private final TrendSignalStore trendSignalStore;
    private final Map<UUID, TrendReport> reportStore = new ConcurrentHashMap<>();

    public TrendReportController(TrendReportCacheService cacheService, TrendSignalStore trendSignalStore) {
        this.cacheService = cacheService;
        this.trendSignalStore = trendSignalStore;
    }

    @PostMapping("/trend-reports")
    public ResponseEntity<TrendReport> createReport(@RequestParam UUID agentId) {
        log.info("POST /trend-reports agentId={}", agentId);
        TrendReport report = cacheService.getOrCreate(agentId);
        reportStore.put(report.id(), report);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/trend-reports/{reportId}")
    public ResponseEntity<TrendReport> getReport(@PathVariable UUID reportId) {
        log.info("GET /trend-reports/{}", reportId);
        TrendReport report = reportStore.get(reportId);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(report);
    }

    @PostMapping("/trend-signals")
    public ResponseEntity<Void> receiveTrendSignal(@RequestBody TrendSignal signal) {
        log.info("POST /trend-signals agentId={} weights={}", signal.agentId(), signal.categoryWeights());
        if (signal.agentId() == null || signal.categoryWeights() == null || signal.categoryWeights().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        trendSignalStore.store(signal);
        return ResponseEntity.noContent().build();
    }
}

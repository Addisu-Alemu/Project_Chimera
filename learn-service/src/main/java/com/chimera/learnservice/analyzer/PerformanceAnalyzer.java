package com.chimera.learnservice.analyzer;

import com.chimera.learnservice.model.EngagementSignal;
import com.chimera.learnservice.model.SignalType;
import com.chimera.learnservice.repository.EngagementSignalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PerformanceAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(PerformanceAnalyzer.class);

    private final EngagementSignalRepository repository;

    public PerformanceAnalyzer(EngagementSignalRepository repository) {
        this.repository = repository;
    }

    public EngagementSummary analyze(UUID postResultId) {
        List<EngagementSignal> signals = repository.findByPostResultId(postResultId);
        log.info("Analyzing {} signals for postResultId={}", signals.size(), postResultId);

        long likes = sum(signals, SignalType.LIKE);
        long shares = sum(signals, SignalType.SHARE);
        long comments = sum(signals, SignalType.COMMENT);
        long views = sum(signals, SignalType.VIEW);
        long clicks = sum(signals, SignalType.CLICK);

        return new EngagementSummary(likes, shares, comments, views, clicks);
    }

    private long sum(List<EngagementSignal> signals, SignalType type) {
        return signals.stream()
                .filter(s -> s.getSignalType() == type)
                .mapToLong(EngagementSignal::getValue)
                .sum();
    }
}

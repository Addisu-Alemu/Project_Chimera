package com.chimera.learnservice.signal;

import com.chimera.learnservice.analyzer.PerformanceAnalyzer;
import com.chimera.learnservice.alert.LearnAlertService;
import com.chimera.learnservice.memory.PerformanceMemory;
import com.chimera.learnservice.model.ContentPerformance;
import com.chimera.learnservice.model.EngagementSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates, accumulates, and stores incoming engagement signals from the ACT service.
 *
 * Pipeline per signal:
 *   validate → accumulate → analyze (score + rate) → record to memory → flag negatives
 *
 * Rule: if the signal is bad or corrupt → flag to human intervention and skip.
 * Rule: negative performance must be flagged and reported back.
 */
public class SignalIngestionService {

    private static final Logger log = LoggerFactory.getLogger(SignalIngestionService.class);

    private final SignalValidator    validator;
    private final PerformanceAnalyzer analyzer;
    private final PerformanceMemory  memory;
    private final LearnAlertService  alertService;

    public SignalIngestionService(SignalValidator validator,
                                  PerformanceAnalyzer analyzer,
                                  PerformanceMemory memory,
                                  LearnAlertService alertService) {
        this.validator    = validator;
        this.analyzer     = analyzer;
        this.memory       = memory;
        this.alertService = alertService;
    }

    /**
     * Ingests a single engagement signal.
     * Returns the updated {@link ContentPerformance} on success, or {@code null} if the signal was rejected.
     */
    public ContentPerformance ingest(EngagementSignal signal) {
        // Rule: bad/corrupt signal → flag and skip
        if (!validator.isValid(signal)) {
            return null;
        }

        log.debug("INGEST: signalId={} contentPieceId={} platform={} type={}",
                signal.id(), signal.contentPieceId(), signal.platform(), signal.signalType());

        // Accumulate signal into existing performance record (or create new one)
        ContentPerformance existing = memory.getLatest(signal.contentPieceId(), signal.platform());
        ContentPerformance accumulated = (existing == null)
                ? ContentPerformance.fromSignal(signal)
                : existing.merge(signal);

        // Re-compute score and rating after accumulation
        ContentPerformance scored = analyzer.analyze(accumulated);

        // Rule: store all historical performance
        memory.record(scored);

        // Rule: flag negative performance
        if (scored.isNegative()) {
            alertService.alertNegativePerformance(scored);
        }

        log.info("INGEST: contentPieceId={} platform={} score={} rating={}",
                scored.contentPieceId(), scored.platform(),
                String.format("%.3f", scored.engagementScore()), scored.rating());

        return scored;
    }
}

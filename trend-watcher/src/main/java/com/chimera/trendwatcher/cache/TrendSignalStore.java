package com.chimera.trendwatcher.cache;

import com.chimera.trendwatcher.model.TrendSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TrendSignalStore {

    private static final Logger log = LoggerFactory.getLogger(TrendSignalStore.class);

    @CachePut(value = "trend-signals", key = "#signal.agentId()")
    public TrendSignal store(TrendSignal signal) {
        log.info("Storing TrendSignal agentId={} weights={}", signal.agentId(), signal.categoryWeights());
        return signal;
    }

    @Cacheable(value = "trend-signals", key = "#agentId", unless = "#result == null")
    public TrendSignal getLatest(UUID agentId) {
        log.debug("Cache miss for TrendSignal agentId={}", agentId);
        return null;
    }
}

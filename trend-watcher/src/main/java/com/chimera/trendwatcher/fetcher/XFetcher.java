package com.chimera.trendwatcher.fetcher;

import com.chimera.trendwatcher.model.Platform;
import com.chimera.trendwatcher.model.TrendTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class XFetcher implements SocialMediaFetcher {

    private static final Logger log = LoggerFactory.getLogger(XFetcher.class);

    @Value("${x.bearer-token:REPLACE_ME}")
    private String bearerToken;

    @Override
    public Platform platform() {
        return Platform.X;
    }

    @Override
    public List<TrendTopic> fetch(UUID agentId) {
        log.info("XFetcher: fetching trends for agent={}", agentId);
        return List.of(
                new TrendTopic("Tech Layoffs", List.of("#tech", "#jobs", "#AI"), 0.95, false),
                new TrendTopic("Climate Action", List.of("#climate", "#sustainability", "#green"), 0.89, false),
                new TrendTopic("Crypto Rally", List.of("#crypto", "#bitcoin", "#web3"), 0.84, false),
                new TrendTopic("Remote Work Tips", List.of("#remotework", "#productivity", "#WFH"), 0.78, false),
                new TrendTopic("Mental Health Day", List.of("#mentalhealth", "#selfcare", "#mindfulness"), 0.72, false)
        );
    }
}

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
public class TikTokFetcher implements SocialMediaFetcher {

    private static final Logger log = LoggerFactory.getLogger(TikTokFetcher.class);

    @Value("${tiktok.access-token:REPLACE_ME}")
    private String accessToken;

    @Override
    public Platform platform() {
        return Platform.TIKTOK;
    }

    @Override
    public List<TrendTopic> fetch(UUID agentId) {
        log.info("TikTokFetcher: fetching trends for agent={}", agentId);
        return List.of(
                new TrendTopic("Dance Challenge", List.of("#dance", "#viral", "#fyp"), 0.97, false),
                new TrendTopic("Life Hacks", List.of("#lifehack", "#tips", "#tiktok"), 0.91, false),
                new TrendTopic("Food Recipes", List.of("#food", "#recipe", "#cooking"), 0.87, false),
                new TrendTopic("Fitness Motivation", List.of("#fitness", "#gym", "#health"), 0.81, false),
                new TrendTopic("Travel Vlog", List.of("#travel", "#adventure", "#explore"), 0.76, false)
        );
    }
}

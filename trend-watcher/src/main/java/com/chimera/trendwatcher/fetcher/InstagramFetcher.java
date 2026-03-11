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
public class InstagramFetcher implements SocialMediaFetcher {

    private static final Logger log = LoggerFactory.getLogger(InstagramFetcher.class);

    @Value("${instagram.access-token:REPLACE_ME}")
    private String accessToken;

    @Override
    public Platform platform() {
        return Platform.INSTAGRAM;
    }

    @Override
    public List<TrendTopic> fetch(UUID agentId) {
        log.info("InstagramFetcher: fetching trends for agent={}", agentId);
        return List.of(
                new TrendTopic("Fashion Week", List.of("#fashion", "#style", "#ootd"), 0.93, false),
                new TrendTopic("Beauty Trends", List.of("#beauty", "#makeup", "#skincare"), 0.88, false),
                new TrendTopic("Home Decor", List.of("#homedecor", "#interior", "#design"), 0.83, false),
                new TrendTopic("Pet Moments", List.of("#pets", "#dogs", "#cats"), 0.79, false),
                new TrendTopic("Wellness Journey", List.of("#wellness", "#mindset", "#selfcare"), 0.74, false)
        );
    }
}

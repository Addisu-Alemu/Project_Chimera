package com.chimera.trendwatcher.fetcher;

import com.chimera.trendwatcher.model.Platform;
import com.chimera.trendwatcher.model.TrendTopic;

import java.util.List;
import java.util.UUID;

public interface SocialMediaFetcher {

    Platform platform();

    List<TrendTopic> fetch(UUID agentId);
}

package com.chimera.trendwatcher.fetcher;

import com.chimera.trendwatcher.model.Platform;
import com.chimera.trendwatcher.model.RawContent;
import com.chimera.trendwatcher.model.TimeRange;

import java.util.List;

/**
 * Contract for all platform-specific content fetchers.
 *
 * Implementations must:
 * - Try the primary endpoint first
 * - Fall back to the backup endpoint if the primary is unavailable
 * - Throw {@link FetchException} only when both endpoints fail
 */
public interface SocialMediaFetcher {

    Platform platform();

    List<RawContent> fetch(TimeRange range) throws FetchException;
}

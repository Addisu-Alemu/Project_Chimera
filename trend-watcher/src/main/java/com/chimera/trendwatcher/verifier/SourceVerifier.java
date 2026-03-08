package com.chimera.trendwatcher.verifier;

import com.chimera.trendwatcher.model.Platform;

import java.util.Map;
import java.util.Set;

/**
 * Checks whether a content source (author handle) is a verified account on its platform.
 *
 * Rule: the source of the news must be from a verified source.
 *
 * In production, this should query the platform's verified-account API.
 * The default implementation uses a configurable allowlist.
 */
public class SourceVerifier {

    /**
     * Pre-seeded verified handles per platform (lowercase).
     * Keys are Platform enum values; values are sets of verified handles.
     */
    private final Map<Platform, Set<String>> verifiedHandles;

    public SourceVerifier(Map<Platform, Set<String>> verifiedHandles) {
        this.verifiedHandles = verifiedHandles;
    }

    /**
     * Returns {@code true} if {@code handle} is considered verified on {@code platform}.
     */
    public boolean isVerified(String handle, Platform platform) {
        Set<String> handles = verifiedHandles.getOrDefault(platform, Set.of());
        return handles.contains(handle.toLowerCase());
    }

    /**
     * Factory: creates a verifier that allows all sources.
     * Use during development or when source-verification is delegated upstream.
     */
    public static SourceVerifier allowAll() {
        return new SourceVerifier(Map.of()) {
            @Override
            public boolean isVerified(String handle, Platform platform) {
                return true;
            }
        };
    }
}

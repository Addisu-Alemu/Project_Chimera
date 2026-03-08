package com.chimera.contentcreator.placeholder;

import com.chimera.contentcreator.model.ContentType;

import java.util.Map;

/**
 * PLACEHOLDER — will be connected to the LEARN service in a future iteration.
 *
 * Carries audience engagement signals that the CREATE service uses to adjust
 * its {@link com.chimera.contentcreator.model.GenerationParameters}.
 *
 * Rule: if feedback from the audience is bad, adjust generation parameters.
 *
 * Implementations will be provided by the LEARN service. A no-op default is
 * available via {@link #neutral()}.
 */
public interface FeedbackData {

    /**
     * Overall engagement score in range [0.0, 1.0].
     * < 0.3  → poor  (switch to HUMOROUS, enable emojis, raise creativity)
     * < 0.5  → below average (switch to CASUAL, enable emojis)
     * ≥ 0.5  → acceptable (keep defaults)
     */
    double engagementScore();

    /**
     * Per-content-type engagement breakdown.
     * The CREATE service may use this to favour better-performing formats.
     */
    Map<ContentType, Double> engagementByType();

    /**
     * Topics that under-performed in the previous cycle.
     * The CREATE service may deprioritise or reframe these topics.
     */
    java.util.List<String> underperformingTopics();

    // -------------------------------------------------------------------------
    // Factory — neutral baseline used when LEARN is not yet connected
    // -------------------------------------------------------------------------

    static FeedbackData neutral() {
        return new FeedbackData() {
            @Override public double engagementScore()                    { return 0.7; }
            @Override public Map<ContentType, Double> engagementByType() { return Map.of(); }
            @Override public java.util.List<String> underperformingTopics() { return java.util.List.of(); }
        };
    }
}

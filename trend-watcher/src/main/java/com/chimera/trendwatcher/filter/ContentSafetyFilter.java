package com.chimera.trendwatcher.filter;

import com.chimera.trendwatcher.model.RawContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * Evaluates whether a piece of raw content is safe for inclusion in a trend report.
 *
 * The default implementation uses a keyword blocklist. Replace or extend this class
 * with an AI-based content moderation API for production use.
 *
 * Rule: if content is harmful → skip and log it.
 */
public class ContentSafetyFilter {

    private static final Logger log = LoggerFactory.getLogger(ContentSafetyFilter.class);

    // Blocklist of harmful keyword patterns (lowercase). Extend as needed.
    private static final List<String> BLOCKED_KEYWORDS = List.of(
            "violence", "hate speech", "self-harm", "terrorism",
            "child abuse", "explicit", "gore", "harassment"
    );

    /**
     * Returns {@code true} if the content is safe to include.
     * Logs a warning and returns {@code false} if content is flagged.
     */
    public boolean isSafe(RawContent content) {
        String lowerText = content.text().toLowerCase(Locale.ROOT);

        for (String keyword : BLOCKED_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                log.warn(
                        "SAFETY_FILTER: Skipping harmful content [id={}, platform={}, keyword='{}']",
                        content.id(), content.platform(), keyword
                );
                return false;
            }
        }
        return true;
    }
}

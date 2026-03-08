package com.chimera.contentcreator.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * Evaluates whether a generated content body is safe to pass to the ACT service.
 *
 * Rule: content must pass the safety filter before being forwarded to ACT.
 * Rule: if a piece fails the filter, discard it and regenerate.
 *
 * The default implementation uses a keyword blocklist. Replace with an
 * AI-based moderation API for production use.
 */
public class ContentSafetyFilter {

    private static final Logger log = LoggerFactory.getLogger(ContentSafetyFilter.class);

    private static final List<String> BLOCKED_KEYWORDS = List.of(
            "violence", "hate speech", "self-harm", "terrorism",
            "child abuse", "explicit", "gore", "harassment",
            "misinformation", "fake news"
    );

    /**
     * Returns {@code true} if the generated body is safe for publishing.
     * Logs a warning at WARN level when content is flagged (traceability for discards).
     */
    public boolean isSafe(String body) {
        String lower = body.toLowerCase(Locale.ROOT);
        for (String keyword : BLOCKED_KEYWORDS) {
            if (lower.contains(keyword)) {
                log.warn("SAFETY_FILTER [CREATE]: blocked keyword='{}' detected in generated content", keyword);
                return false;
            }
        }
        return true;
    }
}

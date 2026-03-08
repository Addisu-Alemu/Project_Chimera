package com.chimera.contentcreator.model;

import java.util.Map;

/**
 * Tunable parameters that control how content is generated.
 *
 * These are adjusted at runtime when the LEARN service reports poor audience engagement:
 * - Low engagement  → HUMOROUS tone, emojis enabled, higher creativity
 * - Below-average   → CASUAL tone, emojis enabled
 * - Good engagement → keep defaults
 *
 * @param tone             Writing voice applied to all generated pieces
 * @param maxLengthByType  Hard character limits per content type
 * @param includeHashtags  Whether to append hashtags to each piece
 * @param emojiEnabled     Whether emojis are included in the body
 * @param creativityLevel  0.0 (conservative/factual) → 1.0 (creative/experimental)
 */
public record GenerationParameters(
        Tone tone,
        Map<ContentType, Integer> maxLengthByType,
        boolean includeHashtags,
        boolean emojiEnabled,
        double creativityLevel
) {

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    public static GenerationParameters defaults() {
        return new GenerationParameters(
                Tone.INFORMATIVE,
                Map.of(
                        ContentType.POST,              280,
                        ContentType.CAPTION,           500,
                        ContentType.VIDEO_DESCRIPTION, 2000
                ),
                true,
                false,
                0.5
        );
    }

    // -------------------------------------------------------------------------
    // Wither helpers (records don't auto-generate these in Java 21)
    // -------------------------------------------------------------------------

    public GenerationParameters withTone(Tone tone) {
        return new GenerationParameters(tone, maxLengthByType, includeHashtags, emojiEnabled, creativityLevel);
    }

    public GenerationParameters withEmojiEnabled(boolean enabled) {
        return new GenerationParameters(tone, maxLengthByType, includeHashtags, enabled, creativityLevel);
    }

    public GenerationParameters withCreativity(double level) {
        return new GenerationParameters(tone, maxLengthByType, includeHashtags, emojiEnabled,
                Math.max(0.0, Math.min(1.0, level)));
    }

    public int maxLength(ContentType type) {
        return maxLengthByType.getOrDefault(type, 500);
    }
}

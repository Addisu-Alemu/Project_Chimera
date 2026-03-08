package com.chimera.contentcreator.generator;

import com.chimera.contentcreator.model.ContentPiece;
import com.chimera.contentcreator.model.ContentType;
import com.chimera.contentcreator.model.GenerationParameters;
import com.chimera.trendwatcher.model.TrendReport;
import com.chimera.trendwatcher.model.TrendingTopic;

/**
 * Strategy interface for content generation.
 *
 * Two implementations are planned:
 * - {@link TemplateContentGenerator} — deterministic, no external API (current)
 * - LlmContentGenerator             — calls Claude API for richer output (future)
 *
 * Swap implementations in {@link com.chimera.contentcreator.ContentCreatorApplication}
 * without touching service logic.
 */
public interface ContentGenerator {

    /**
     * Generates a single {@link ContentPiece} for the given topic and format.
     *
     * @param topic  The trending topic to write about
     * @param report The full TrendReport for context (time range, categories, timestamp)
     * @param type   The desired output format
     * @param params Current generation parameters (tone, length, emoji, creativity)
     * @return A fully-formed ContentPiece with timestamp and source references
     */
    ContentPiece generate(TrendingTopic topic,
                          TrendReport report,
                          ContentType type,
                          GenerationParameters params);
}

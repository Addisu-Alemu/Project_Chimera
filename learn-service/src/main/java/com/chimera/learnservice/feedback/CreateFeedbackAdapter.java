package com.chimera.learnservice.feedback;

import com.chimera.contentcreator.model.ContentType;
import com.chimera.contentcreator.placeholder.FeedbackData;
import com.chimera.learnservice.memory.PerformanceMemory;

import java.util.List;
import java.util.Map;

/**
 * LEARN → CREATE connection.
 *
 * Implements {@link FeedbackData} (the placeholder left in the content-creator module)
 * using live data from {@link PerformanceMemory}.
 *
 * The CREATE service injects this adapter in place of {@code FeedbackData.neutral()},
 * enabling real-time adjustment of {@link com.chimera.contentcreator.model.GenerationParameters}
 * based on observed audience performance.
 *
 * This is a pull-based connection: CREATE calls these methods when generating
 * new content; LEARN keeps PerformanceMemory up-to-date in real time.
 */
public class CreateFeedbackAdapter implements FeedbackData {

    private final PerformanceMemory memory;

    public CreateFeedbackAdapter(PerformanceMemory memory) {
        this.memory = memory;
    }

    /**
     * Overall rolling engagement score across all tracked content.
     * CREATE uses this to decide whether to raise creativity and change tone.
     *
     * Thresholds (from ContentCreatorService):
     *   < 0.3 → HUMOROUS + emojis + high creativity
     *   < 0.5 → CASUAL + emojis
     *   ≥ 0.5 → keep defaults (INFORMATIVE)
     */
    @Override
    public double engagementScore() {
        return memory.getOverallEngagementScore();
    }

    /**
     * Per-ContentType engagement breakdown.
     * CREATE uses this to favour better-performing formats (POST vs CAPTION vs VIDEO_DESCRIPTION).
     */
    @Override
    public Map<ContentType, Double> engagementByType() {
        return memory.getEngagementByContentType();
    }

    /**
     * Topics with POOR or NEGATIVE performance ratings.
     * CREATE deprioritises or reframes these topics when generating new content.
     *
     * Rule: negative performance must be flagged and reported back.
     */
    @Override
    public List<String> underperformingTopics() {
        return memory.getUnderperformingTopics();
    }
}

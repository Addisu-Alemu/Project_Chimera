package com.chimera.contentcreator.generator;

import com.chimera.contentcreator.model.ContentPiece;
import com.chimera.contentcreator.model.ContentType;
import com.chimera.contentcreator.model.GenerationParameters;
import com.chimera.contentcreator.model.Tone;
import com.chimera.trendwatcher.model.Platform;
import com.chimera.trendwatcher.model.TrendReport;
import com.chimera.trendwatcher.model.TrendingTopic;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Template-based content generator — deterministic, no external API.
 *
 * Fills structured templates with data from the TrendReport:
 * - Trending topic name and platforms
 * - Verified-source attribution (inherited from PERCEIVE validation)
 * - Share counts, time range, timestamps
 * - Tone and emoji variants controlled by {@link GenerationParameters}
 *
 * Replace or extend with an LlmContentGenerator (Claude API) via the
 * {@link ContentGenerator} interface when richer copy is needed.
 */
public class TemplateContentGenerator implements ContentGenerator {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    @Override
    public ContentPiece generate(TrendingTopic topic,
                                 TrendReport report,
                                 ContentType type,
                                 GenerationParameters params) {

        String platformStr   = formatPlatforms(topic.platforms());
        String sourceRef     = buildSourceRef(topic, report);
        String hashtags      = buildHashtags(topic, params);
        String reportedAt    = FMT.format(report.generatedAt());
        String createdAt     = FMT.format(Instant.now());
        String sharesFmt     = formatShares(topic.totalShares());

        String body = switch (type) {
            case POST              -> buildPost(topic, platformStr, sharesFmt, sourceRef, hashtags, params, createdAt);
            case CAPTION           -> buildCaption(topic, platformStr, sharesFmt, sourceRef, hashtags, params, createdAt);
            case VIDEO_DESCRIPTION -> buildVideoDescription(topic, platformStr, sharesFmt, sourceRef,
                                                            hashtags, params, reportedAt, createdAt,
                                                            report.timeRange().name());
        };

        // Enforce max-length hard cap per spec
        int maxLen = params.maxLength(type);
        if (body.length() > maxLen) {
            body = body.substring(0, maxLen - 3) + "...";
        }

        return new ContentPiece(
                UUID.randomUUID().toString(),
                body,
                type,
                topic.topic(),
                List.of(sourceRef),
                extractHashtagList(topic),
                topic.platforms(),
                Instant.now()   // Rule: content must always have a timestamp
        );
    }

    // -------------------------------------------------------------------------
    // Template builders
    // -------------------------------------------------------------------------

    private String buildPost(TrendingTopic topic, String platforms, String shares,
                             String sourceRef, String hashtags,
                             GenerationParameters params, String timestamp) {
        String fire  = params.emojiEnabled() ? "🔥 " : "";
        String chart = params.emojiEnabled() ? " 📊" : "";

        return switch (params.tone()) {
            case CASUAL        -> fire + topic.topic() + " is all over " + platforms + " right now!\n"
                                  + shares + " shares and climbing." + chart + "\n"
                                  + "Source: " + sourceRef + "\n"
                                  + hashtags + "\n[" + timestamp + "]";

            case HUMOROUS      -> fire + "Everyone's talking about " + topic.topic() + " on " + platforms
                                  + " and honestly? Same. " + shares + " shares can't be wrong!\n"
                                  + "Source: " + sourceRef + "\n"
                                  + hashtags + "\n[" + timestamp + "]";

            case PROFESSIONAL  -> topic.topic() + " is currently trending across " + platforms
                                  + " with " + shares + " shares.\n"
                                  + "Verified source: " + sourceRef + "\n"
                                  + hashtags + "\n[" + timestamp + "]";

            case INFORMATIVE   -> "TRENDING: " + topic.topic() + " | " + platforms
                                  + " | " + shares + " shares\n"
                                  + "Source verified: " + sourceRef + "\n"
                                  + hashtags + "\n[" + timestamp + "]";
        };
    }

    private String buildCaption(TrendingTopic topic, String platforms, String shares,
                                String sourceRef, String hashtags,
                                GenerationParameters params, String timestamp) {
        String hook   = params.emojiEnabled() ? "🚨 " : "";
        String bullet = params.emojiEnabled() ? "📌 " : "• ";

        return switch (params.tone()) {
            case CASUAL       -> hook + topic.topic() + " — you need to see this 👀\n\n"
                                 + bullet + shares + " shares across " + platforms + "\n"
                                 + bullet + "Source-verified by Project Chimera\n\n"
                                 + "🔗 " + sourceRef + "\n"
                                 + "📅 " + timestamp + "\n\n" + hashtags;

            case HUMOROUS     -> hook + "Not to be dramatic but " + topic.topic()
                                 + " just broke the internet on " + platforms + " 😂\n\n"
                                 + bullet + shares + " shares (and counting)\n"
                                 + bullet + "Source: " + sourceRef + "\n"
                                 + "📅 " + timestamp + "\n\n" + hashtags;

            case PROFESSIONAL -> topic.topic() + " — Trending Analysis\n\n"
                                 + bullet + "Platforms: " + platforms + "\n"
                                 + bullet + "Shares: " + shares + "\n"
                                 + bullet + "Verified source: " + sourceRef + "\n\n"
                                 + "Report generated: " + timestamp + "\n\n" + hashtags;

            case INFORMATIVE  -> "TRENDING NOW: " + topic.topic() + "\n\n"
                                 + "What's happening:\n"
                                 + bullet + shares + " shares on " + platforms + "\n"
                                 + bullet + "Source: " + sourceRef + "\n\n"
                                 + "📅 " + timestamp + "\n\n" + hashtags;
        };
    }

    private String buildVideoDescription(TrendingTopic topic, String platforms, String shares,
                                         String sourceRef, String hashtags,
                                         GenerationParameters params, String reportedAt,
                                         String createdAt, String timeRange) {
        return "TRENDING NOW: " + topic.topic() + "\n\n"
             + topic.topic() + " is the most talked-about topic on " + platforms
             + ", accumulating " + shares + " shares in the last " + timeRange.toLowerCase() + ".\n\n"
             + "WHAT'S HAPPENING:\n"
             + "This video covers everything you need to know about " + topic.topic()
             + " — a trend currently dominating " + platforms + ". "
             + "All information is sourced from verified accounts and validated by "
             + "Project Chimera's PERCEIVE service.\n\n"
             + "VERIFIED SOURCES:\n"
             + sourceRef + "\n\n"
             + "REPORT INFO:\n"
             + "• Trend report generated : " + reportedAt + "\n"
             + "• Content created        : " + createdAt + "\n"
             + "• Analysis window        : " + timeRange + "\n\n"
             + hashtags + "\n\n"
             + "---\n"
             + "Content by Project Chimera | Source-verified | Safety-checked";
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String formatPlatforms(List<Platform> platforms) {
        return platforms.stream()
                .map(p -> switch (p) {
                    case TWITTER  -> "Twitter/X";
                    case TIKTOK   -> "TikTok";
                    case INSTAGRAM -> "Instagram";
                })
                .collect(Collectors.joining(", "));
    }

    /**
     * Source reference string — attributes the trend to the verified platforms
     * as confirmed by the PERCEIVE (TrendWatcher) service.
     */
    private String buildSourceRef(TrendingTopic topic, TrendReport report) {
        return formatPlatforms(topic.platforms())
                + " (verified by PERCEIVE, report: " + FMT.format(report.generatedAt()) + ")";
    }

    private String buildHashtags(TrendingTopic topic, GenerationParameters params) {
        if (!params.includeHashtags()) return "";
        return extractHashtagList(topic).stream()
                .limit(5)
                .collect(Collectors.joining(" "));
    }

    private List<String> extractHashtagList(TrendingTopic topic) {
        // The topic name may itself be a hashtag; also append platform-specific tags
        String normalised = topic.topic().startsWith("#") ? topic.topic() : "#" + topic.topic();
        return List.of(normalised, "#trending", "#chimera");
    }

    private String formatShares(long shares) {
        if (shares >= 1_000_000) return String.format("%.1fM", shares / 1_000_000.0);
        if (shares >= 1_000)     return String.format("%.1fK", shares / 1_000.0);
        return String.valueOf(shares);
    }
}

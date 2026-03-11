package com.chimera.contentcreator.generator;

import com.chimera.contentcreator.client.dto.TrendReportDto;
import com.chimera.contentcreator.client.dto.TrendTopicDto;
import com.chimera.contentcreator.model.ContentBundle;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class TemplateContentGenerator {

    private static final int CAPTION_MAX_LENGTH = 2200;
    private static final int VIDEO_DESC_MAX_LENGTH = 500;
    private static final int MAX_HASHTAGS = 15;

    public ContentBundle generate(UUID agentId, TrendReportDto report) {
        List<TrendTopicDto> top3 = report.topics().stream().limit(3).toList();

        String caption = buildCaption(top3);
        List<String> hashtags = buildHashtags(top3);
        String videoDescription = buildVideoDescription(top3);

        return new ContentBundle(
                UUID.randomUUID(),
                agentId,
                report.id(),
                truncate(caption, CAPTION_MAX_LENGTH),
                hashtags,
                truncate(videoDescription, VIDEO_DESC_MAX_LENGTH),
                null,
                Instant.now()
        );
    }

    private String buildCaption(List<TrendTopicDto> topics) {
        String topicNames = topics.stream()
                .map(TrendTopicDto::name)
                .collect(Collectors.joining(", "));
        return "Trending now: " + topicNames + ". Stay ahead of the curve with the latest insights!";
    }

    private List<String> buildHashtags(List<TrendTopicDto> topics) {
        return topics.stream()
                .flatMap(t -> t.hashtags().stream())
                .distinct()
                .limit(MAX_HASHTAGS)
                .collect(Collectors.toList());
    }

    private String buildVideoDescription(List<TrendTopicDto> topics) {
        String topicNames = topics.stream()
                .map(TrendTopicDto::name)
                .collect(Collectors.joining(", "));
        return "In this video we cover the top trending topics: " + topicNames
                + ". All content sourced from verified social platforms and safety-checked.";
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}

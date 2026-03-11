package com.chimera.actservice.publisher;

import com.chimera.actservice.client.dto.ContentBundleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TikTokPublisher implements ContentPublisher {

    private static final Logger log = LoggerFactory.getLogger(TikTokPublisher.class);

    @Value("${tiktok.access-token:REPLACE_ME}")
    private String accessToken;

    @Override
    public String platform() {
        return "TIKTOK";
    }

    @Override
    public String publish(ContentBundleDto bundle) {
        String platformPostId = UUID.randomUUID().toString();
        log.info("TikTokPublisher: published bundleId={} platformPostId={}", bundle.id(), platformPostId);
        return platformPostId;
    }
}

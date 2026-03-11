package com.chimera.actservice.client;

import com.chimera.actservice.client.dto.ContentBundleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Component
public class ContentCreatorClient {

    private static final Logger log = LoggerFactory.getLogger(ContentCreatorClient.class);

    private final WebClient webClient;

    public ContentCreatorClient(@Value("${content.creator.url:http://localhost:8082}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public ContentBundleDto getBundle(UUID bundleId) {
        log.info("Fetching ContentBundle bundleId={}", bundleId);
        return webClient.get()
                .uri("/content-bundles/{bundleId}", bundleId)
                .retrieve()
                .bodyToMono(ContentBundleDto.class)
                .block();
    }
}

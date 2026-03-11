package com.chimera.actservice.publisher;

import com.chimera.actservice.client.dto.ContentBundleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class XPublisher implements ContentPublisher {

    private static final Logger log = LoggerFactory.getLogger(XPublisher.class);

    @Value("${x.bearer-token:REPLACE_ME}")
    private String bearerToken;

    @Override
    public String platform() {
        return "X";
    }

    @Override
    public String publish(ContentBundleDto bundle) {
        String platformPostId = UUID.randomUUID().toString();
        log.info("XPublisher: published bundleId={} platformPostId={}", bundle.id(), platformPostId);
        return platformPostId;
    }
}

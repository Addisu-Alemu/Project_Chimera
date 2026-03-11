package com.chimera.actservice.validator;

import com.chimera.actservice.client.dto.ContentBundleDto;
import com.chimera.actservice.exception.InvalidContentSpecException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ContentSpecValidator {

    private static final Logger log = LoggerFactory.getLogger(ContentSpecValidator.class);

    public void validate(ContentBundleDto bundle) {
        if (bundle == null) {
            throw new InvalidContentSpecException("ContentBundle is null — no spec received from CREATE");
        }
        if (bundle.trendReportId() == null) {
            log.warn("ContentBundle id={} missing trendReportId — traceability violated", bundle.id());
            throw new InvalidContentSpecException("ContentBundle missing trendReportId: id=" + bundle.id());
        }
        if (bundle.safetyPassedAt() == null) {
            log.warn("ContentBundle id={} has no safetyPassedAt — safety check not confirmed", bundle.id());
            throw new InvalidContentSpecException("ContentBundle missing safetyPassedAt: id=" + bundle.id());
        }
    }
}

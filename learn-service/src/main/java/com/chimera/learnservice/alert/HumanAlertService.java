package com.chimera.learnservice.alert;

import com.chimera.learnservice.model.AlertType;
import com.chimera.learnservice.model.HumanAlert;
import com.chimera.learnservice.repository.HumanAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Service
public class HumanAlertService {

    private static final Logger log = LoggerFactory.getLogger(HumanAlertService.class);

    private final HumanAlertRepository alertRepository;
    private final RestTemplate restTemplate;
    private final String webhookUrl;

    public HumanAlertService(HumanAlertRepository alertRepository,
                              @Value("${alert.webhook.url:}") String webhookUrl) {
        this.alertRepository = alertRepository;
        this.restTemplate = new RestTemplate();
        this.webhookUrl = webhookUrl;
    }

    public HumanAlert raise(UUID agentId, AlertType type, UUID triggeringRecordId,
                             String triggeringRecordLink, String thresholdValue, String actualValue) {
        HumanAlert alert = new HumanAlert(
                UUID.randomUUID(), agentId, type, triggeringRecordId,
                triggeringRecordLink, thresholdValue, actualValue
        );
        HumanAlert saved = alertRepository.save(alert);
        log.error("HUMAN ALERT: type={} agentId={} triggeringRecordId={} threshold={} actual={}",
                type, agentId, triggeringRecordId, thresholdValue, actualValue);
        dispatchWebhook(saved);
        return saved;
    }

    private void dispatchWebhook(HumanAlert alert) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("HUMAN ALERT: No webhook URL configured for alertId={}", alert.getId());
            return;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "alertId", alert.getId(),
                    "type", alert.getType(),
                    "agentId", alert.getAgentId(),
                    "triggeringRecordId", alert.getTriggeringRecordId()
            );
            restTemplate.postForEntity(webhookUrl, payload, Void.class);
        } catch (Exception e) {
            log.error("Failed to dispatch webhook for alertId={}: {}", alert.getId(), e.getMessage());
        }
    }
}

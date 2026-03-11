package com.chimera.actservice.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "human_alerts")
@EntityListeners(AuditingEntityListener.class)
public class HumanAlert {

    @Id
    private UUID id;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AlertType type;

    @Column(name = "triggering_record_id", nullable = false)
    private UUID triggeringRecordId;

    @Column(name = "triggering_record_link")
    private String triggeringRecordLink;

    @Column(name = "threshold_value")
    private String thresholdValue;

    @Column(name = "actual_value")
    private String actualValue;

    @CreatedDate
    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolving_operator_id")
    private String resolvingOperatorId;

    protected HumanAlert() {}

    public HumanAlert(UUID id, UUID agentId, AlertType type, UUID triggeringRecordId,
                      String triggeringRecordLink, String thresholdValue, String actualValue) {
        this.id = id;
        this.agentId = agentId;
        this.type = type;
        this.triggeringRecordId = triggeringRecordId;
        this.triggeringRecordLink = triggeringRecordLink;
        this.thresholdValue = thresholdValue;
        this.actualValue = actualValue;
    }

    public UUID getId() { return id; }
    public UUID getAgentId() { return agentId; }
    public AlertType getType() { return type; }
    public UUID getTriggeringRecordId() { return triggeringRecordId; }
    public String getTriggeringRecordLink() { return triggeringRecordLink; }
    public String getThresholdValue() { return thresholdValue; }
    public String getActualValue() { return actualValue; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getResolvingOperatorId() { return resolvingOperatorId; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public void setResolvingOperatorId(String id) { this.resolvingOperatorId = id; }
}

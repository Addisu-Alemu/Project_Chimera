package com.chimera.learnservice.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "engagement_signals")
@EntityListeners(AuditingEntityListener.class)
public class EngagementSignal {

    @Id
    private UUID id;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(name = "post_result_id", nullable = false)
    private UUID postResultId;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false)
    private SignalType signalType;

    @Column(name = "value", nullable = false)
    private long value;

    @CreatedDate
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected EngagementSignal() {}

    public EngagementSignal(UUID id, UUID agentId, UUID postResultId, SignalType signalType, long value) {
        this.id = id;
        this.agentId = agentId;
        this.postResultId = postResultId;
        this.signalType = signalType;
        this.value = value;
    }

    public UUID getId() { return id; }
    public UUID getAgentId() { return agentId; }
    public UUID getPostResultId() { return postResultId; }
    public SignalType getSignalType() { return signalType; }
    public long getValue() { return value; }
    public Instant getRecordedAt() { return recordedAt; }
}

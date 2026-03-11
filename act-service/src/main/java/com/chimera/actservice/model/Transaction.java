package com.chimera.actservice.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    private UUID id;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "platform")
    private String platform;

    @Column(name = "content_bundle_id")
    private UUID contentBundleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "actor")
    private String actor;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "approver_id")
    private String approverId;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Transaction() {}

    public Transaction(UUID id, UUID agentId, TransactionType type, BigDecimal amount,
                       String currency, String platform, UUID contentBundleId,
                       TransactionStatus status, String actor) {
        this.id = id;
        this.agentId = agentId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.platform = platform;
        this.contentBundleId = contentBundleId;
        this.status = status;
        this.actor = actor;
    }

    public UUID getId() { return id; }
    public UUID getAgentId() { return agentId; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getPlatform() { return platform; }
    public UUID getContentBundleId() { return contentBundleId; }
    public TransactionStatus getStatus() { return status; }
    public String getActor() { return actor; }
    public Instant getCreatedAt() { return createdAt; }
    public String getApproverId() { return approverId; }
    public Instant getCompletedAt() { return completedAt; }
    public void setApproverId(String approverId) { this.approverId = approverId; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}

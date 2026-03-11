package com.chimera.actservice.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "post_results")
@EntityListeners(AuditingEntityListener.class)
public class PostResult {

    @Id
    private UUID id;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(name = "content_bundle_id", nullable = false)
    private UUID contentBundleId;

    @Column(name = "platform", nullable = false)
    private String platform;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PostStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "platform_post_id")
    private String platformPostId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PostResult() {}

    public PostResult(UUID id, UUID agentId, UUID contentBundleId, String platform,
                      Instant publishedAt, PostStatus status, int attemptCount,
                      String failureReason, String platformPostId) {
        this.id = id;
        this.agentId = agentId;
        this.contentBundleId = contentBundleId;
        this.platform = platform;
        this.publishedAt = publishedAt;
        this.status = status;
        this.attemptCount = attemptCount;
        this.failureReason = failureReason;
        this.platformPostId = platformPostId;
    }

    public UUID getId() { return id; }
    public UUID getAgentId() { return agentId; }
    public UUID getContentBundleId() { return contentBundleId; }
    public String getPlatform() { return platform; }
    public Instant getPublishedAt() { return publishedAt; }
    public PostStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public String getFailureReason() { return failureReason; }
    public String getPlatformPostId() { return platformPostId; }
    public Instant getCreatedAt() { return createdAt; }
}

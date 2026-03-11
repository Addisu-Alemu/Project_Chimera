package com.chimera.learnservice.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feedback_reports")
public class FeedbackReport {

    @Id
    private UUID id;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(name = "content_bundle_id", nullable = false)
    private UUID contentBundleId;

    @Column(name = "confidence_score", nullable = false, precision = 4, scale = 3)
    private BigDecimal confidenceScore;

    @Column(name = "likes")
    private long likes;

    @Column(name = "shares")
    private long shares;

    @Column(name = "comments")
    private long comments;

    @Column(name = "views")
    private long views;

    @Column(name = "click_through_rate", precision = 5, scale = 4)
    private BigDecimal clickThroughRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    private ReviewStatus reviewStatus;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    protected FeedbackReport() {}

    public FeedbackReport(UUID id, UUID agentId, UUID contentBundleId, BigDecimal confidenceScore,
                          long likes, long shares, long comments, long views,
                          BigDecimal clickThroughRate, ReviewStatus reviewStatus, Instant generatedAt) {
        this.id = id;
        this.agentId = agentId;
        this.contentBundleId = contentBundleId;
        this.confidenceScore = confidenceScore;
        this.likes = likes;
        this.shares = shares;
        this.comments = comments;
        this.views = views;
        this.clickThroughRate = clickThroughRate;
        this.reviewStatus = reviewStatus;
        this.generatedAt = generatedAt;
    }

    public UUID getId() { return id; }
    public UUID getAgentId() { return agentId; }
    public UUID getContentBundleId() { return contentBundleId; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public long getLikes() { return likes; }
    public long getShares() { return shares; }
    public long getComments() { return comments; }
    public long getViews() { return views; }
    public BigDecimal getClickThroughRate() { return clickThroughRate; }
    public ReviewStatus getReviewStatus() { return reviewStatus; }
    public Instant getGeneratedAt() { return generatedAt; }
    public Instant getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(Instant dispatchedAt) { this.dispatchedAt = dispatchedAt; }
}

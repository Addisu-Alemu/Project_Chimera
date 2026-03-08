package com.chimera.learnservice.model;

/** Classifies the origin of a raw engagement signal received from the ACT service. */
public enum SignalType {

    /** A single audience interaction event (comment, like, mention, DM). */
    INTERACTION,

    /** Aggregate engagement metrics for a published post (views, shares, total reactions). */
    ENGAGEMENT_METRIC,

    /** Financial transaction outcome associated with a piece of content. */
    TRANSACTION_OUTCOME
}

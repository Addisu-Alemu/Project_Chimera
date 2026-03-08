package com.chimera.learnservice.model;

/**
 * Categorical rating of a content piece's performance on a given platform.
 *
 * Derived from engagement score (0.0 – 1.0):
 *   EXCELLENT  ≥ 0.70  — share widely, use as template
 *   GOOD       ≥ 0.50  — performing above average
 *   AVERAGE    ≥ 0.30  — acceptable, continue monitoring
 *   POOR       ≥ 0.10  — below expectations, flag for review
 *   NEGATIVE   <  0.10  — must be flagged and reported back to CREATE
 */
public enum PerformanceRating {
    EXCELLENT,
    GOOD,
    AVERAGE,
    POOR,
    NEGATIVE
}

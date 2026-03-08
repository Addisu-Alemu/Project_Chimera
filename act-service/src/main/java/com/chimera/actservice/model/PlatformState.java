package com.chimera.actservice.model;

/**
 * Operational state of a social media platform's publishing endpoint.
 * Tracked per-platform by the ActOrchestrator.
 *
 * Transitions:
 *   ACTIVE    → PAUSED     (3 consecutive publish failures or health check fails)
 *   PAUSED    → RECOVERING (health check passes)
 *   RECOVERING → ACTIVE    (first successful publish after recovery)
 */
public enum PlatformState {
    ACTIVE,
    PAUSED,
    RECOVERING
}

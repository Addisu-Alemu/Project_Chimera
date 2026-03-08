package com.chimera.actservice.model;

/** Outcome of a content publishing attempt. */
public enum PostStatus {
    PUBLISHED,
    RETRYING,
    FAILED,

    /** Platform API is down — publishing paused, state logged, will resume on recovery. */
    PAUSED
}

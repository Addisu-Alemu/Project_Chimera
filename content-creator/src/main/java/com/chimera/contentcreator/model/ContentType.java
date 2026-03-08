package com.chimera.contentcreator.model;

/**
 * The format of a generated content piece.
 */
public enum ContentType {

    /** Short-form social media post (≤ 280 chars — Twitter-style). */
    POST,

    /** Image or video caption (≤ 500 chars). */
    CAPTION,

    /** Full video description with sources and timestamps (≤ 2 000 chars). */
    VIDEO_DESCRIPTION
}

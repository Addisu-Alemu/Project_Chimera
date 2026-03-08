package com.chimera.learnservice.model;

import com.chimera.contentcreator.model.ContentType;
import com.chimera.trendwatcher.model.Platform;

import java.time.Instant;

/**
 * Raw engagement signal ingested from the ACT service.
 *
 * Built by {@link com.chimera.learnservice.impl.ChimeraLearnService} from:
 * - {@link com.chimera.actservice.model.AudienceInteraction} + Reply pairs
 * - {@link com.chimera.actservice.model.PostResult} + interaction lists
 *
 * Validated by {@link com.chimera.learnservice.signal.SignalValidator} before processing.
 *
 * @param id              Unique signal identifier
 * @param contentPieceId  ID of the ContentPiece this signal relates to (traceability)
 * @param topic           Trending topic the content addressed
 * @param platform        Platform where the engagement occurred
 * @param contentType     Format of the content (POST, CAPTION, VIDEO_DESCRIPTION)
 * @param likes           Number of likes / reactions in this signal window
 * @param shares          Number of shares / reposts
 * @param comments        Number of comments / replies
 * @param views           Number of views / impressions
 * @param signalType      Origin of the signal
 * @param transactionPositive For TRANSACTION_OUTCOME signals: whether the outcome was positive
 * @param receivedAt      UTC timestamp when LEARN received this signal
 */
public record EngagementSignal(
        String id,
        String contentPieceId,
        String topic,
        Platform platform,
        ContentType contentType,
        long likes,
        long shares,
        long comments,
        long views,
        SignalType signalType,
        boolean transactionPositive,
        Instant receivedAt
) {}

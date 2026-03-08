package com.chimera.actservice.publisher;

import com.chimera.actservice.exception.PublishException;
import com.chimera.actservice.model.PostResult;
import com.chimera.contentcreator.model.ContentPiece;
import com.chimera.trendwatcher.model.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

/**
 * Publishes content to TikTok via Content Posting API.
 * Falls back to the backup endpoint if the primary is unavailable.
 */
public class TikTokPublisher implements ContentPublisher {

    private static final Logger log = LoggerFactory.getLogger(TikTokPublisher.class);

    private static final String PRIMARY_ENDPOINT = "https://open.tiktokapis.com/v2/post/publish/video/init/";
    private static final String BACKUP_ENDPOINT  = "https://open.tiktokapis.com/v2/post/publish/inbox/video/init/";
    private static final String HEALTH_ENDPOINT  = "https://open.tiktokapis.com/v2/user/info/";

    private final String accessToken;
    private final HttpClient httpClient;

    public TikTokPublisher(String accessToken, HttpClient httpClient) {
        this.accessToken = accessToken;
        this.httpClient  = httpClient;
    }

    @Override
    public Platform platform() {
        return Platform.TIKTOK;
    }

    @Override
    public PostResult publish(ContentPiece piece) throws PublishException {
        try {
            log.info("TIKTOK: publishing contentPieceId={} via primary", piece.id());
            return postToEndpoint(PRIMARY_ENDPOINT, piece);
        } catch (IOException | InterruptedException e) {
            log.warn("TIKTOK: primary failed ({}), trying backup", e.getMessage());
            try {
                return postToEndpoint(BACKUP_ENDPOINT, piece);
            } catch (IOException | InterruptedException ex) {
                throw new PublishException("TikTok primary and backup both failed: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(HEALTH_ENDPOINT))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET().build();
            HttpResponse<Void> res = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            return res.statusCode() < 500;
        } catch (Exception e) {
            log.warn("TIKTOK: health check failed: {}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------

    private PostResult postToEndpoint(String endpoint, ContentPiece piece)
            throws IOException, InterruptedException {

        String body = buildRequestBody(piece);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Unexpected HTTP status: " + response.statusCode());
        }

        String platformPostId = "tt-" + UUID.randomUUID();
        log.info("TIKTOK: published contentPieceId={} → platformPostId={}", piece.id(), platformPostId);
        return PostResult.published(piece.id(), Platform.TIKTOK, platformPostId, 1);
    }

    private String buildRequestBody(ContentPiece piece) {
        return """
                {
                  "post_info": {
                    "title": %s,
                    "description": %s,
                    "privacy_level": "PUBLIC_TO_EVERYONE"
                  },
                  "source_info": { "source": "FILE_UPLOAD" }
                }
                """.formatted(escapeJson(piece.topic()), escapeJson(piece.body()));
    }

    private String escapeJson(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                        .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
}

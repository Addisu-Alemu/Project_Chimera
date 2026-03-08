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
 * Publishes content to Twitter (X) via API v2.
 * Falls back to the backup endpoint if the primary is unavailable.
 */
public class TwitterPublisher implements ContentPublisher {

    private static final Logger log = LoggerFactory.getLogger(TwitterPublisher.class);

    private static final String PRIMARY_ENDPOINT = "https://api.twitter.com/2/tweets";
    private static final String BACKUP_ENDPOINT  = "https://api.twitter.com/2/tweets/media";
    private static final String HEALTH_ENDPOINT  = "https://api.twitter.com/2/openapi.json";

    private final String bearerToken;
    private final HttpClient httpClient;

    public TwitterPublisher(String bearerToken, HttpClient httpClient) {
        this.bearerToken = bearerToken;
        this.httpClient  = httpClient;
    }

    @Override
    public Platform platform() {
        return Platform.TWITTER;
    }

    @Override
    public PostResult publish(ContentPiece piece) throws PublishException {
        try {
            log.info("TWITTER: publishing contentPieceId={} via primary", piece.id());
            return postToEndpoint(PRIMARY_ENDPOINT, piece);
        } catch (IOException | InterruptedException e) {
            log.warn("TWITTER: primary failed ({}), trying backup", e.getMessage());
            try {
                return postToEndpoint(BACKUP_ENDPOINT, piece);
            } catch (IOException | InterruptedException ex) {
                throw new PublishException("Twitter primary and backup both failed: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(HEALTH_ENDPOINT))
                    .header("Authorization", "Bearer " + bearerToken)
                    .GET().build();
            HttpResponse<Void> res = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            return res.statusCode() < 500;
        } catch (Exception e) {
            log.warn("TWITTER: health check failed: {}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------

    private PostResult postToEndpoint(String endpoint, ContentPiece piece)
            throws IOException, InterruptedException {

        String body = buildRequestBody(piece);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + bearerToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201 && response.statusCode() != 200) {
            throw new IOException("Unexpected HTTP status: " + response.statusCode());
        }

        // TODO: parse real post ID from response JSON
        String platformPostId = "tw-" + UUID.randomUUID();
        log.info("TWITTER: published contentPieceId={} → platformPostId={}", piece.id(), platformPostId);
        return PostResult.published(piece.id(), Platform.TWITTER, platformPostId, 1);
    }

    private String buildRequestBody(ContentPiece piece) {
        // Truncate body to Twitter's 280-char limit if needed
        String text = piece.body().length() > 280
                ? piece.body().substring(0, 277) + "..."
                : piece.body();
        return "{\"text\": " + escapeJson(text) + "}";
    }

    private String escapeJson(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                        .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
}

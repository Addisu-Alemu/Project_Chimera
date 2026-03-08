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
 * Publishes content to Instagram via Graph API.
 * Falls back to the backup endpoint if the primary is unavailable.
 */
public class InstagramPublisher implements ContentPublisher {

    private static final Logger log = LoggerFactory.getLogger(InstagramPublisher.class);

    private static final String PRIMARY_ENDPOINT = "https://graph.instagram.com/v18.0/me/media";
    private static final String BACKUP_ENDPOINT  = "https://graph.facebook.com/v18.0/me/media";
    private static final String HEALTH_ENDPOINT  = "https://graph.instagram.com/v18.0/me";

    private final String accessToken;
    private final HttpClient httpClient;

    public InstagramPublisher(String accessToken, HttpClient httpClient) {
        this.accessToken = accessToken;
        this.httpClient  = httpClient;
    }

    @Override
    public Platform platform() {
        return Platform.INSTAGRAM;
    }

    @Override
    public PostResult publish(ContentPiece piece) throws PublishException {
        try {
            log.info("INSTAGRAM: publishing contentPieceId={} via primary", piece.id());
            return postToEndpoint(PRIMARY_ENDPOINT, piece);
        } catch (IOException | InterruptedException e) {
            log.warn("INSTAGRAM: primary failed ({}), trying backup", e.getMessage());
            try {
                return postToEndpoint(BACKUP_ENDPOINT, piece);
            } catch (IOException | InterruptedException ex) {
                throw new PublishException("Instagram primary and backup both failed: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(HEALTH_ENDPOINT + "?access_token=" + accessToken))
                    .GET().build();
            HttpResponse<Void> res = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            return res.statusCode() < 500;
        } catch (Exception e) {
            log.warn("INSTAGRAM: health check failed: {}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------

    private PostResult postToEndpoint(String endpoint, ContentPiece piece)
            throws IOException, InterruptedException {

        String query = "caption=" + encode(piece.body())
                + "&media_type=IMAGE"
                + "&access_token=" + accessToken;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(query))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Unexpected HTTP status: " + response.statusCode());
        }

        String platformPostId = "ig-" + UUID.randomUUID();
        log.info("INSTAGRAM: published contentPieceId={} → platformPostId={}", piece.id(), platformPostId);
        return PostResult.published(piece.id(), Platform.INSTAGRAM, platformPostId, 1);
    }

    private String encode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}

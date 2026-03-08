package com.chimera.trendwatcher.fetcher;

import com.chimera.trendwatcher.model.Platform;
import com.chimera.trendwatcher.model.RawContent;
import com.chimera.trendwatcher.model.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;

/**
 * Fetches most-shared content from Instagram Graph API.
 *
 * Rule: If the primary endpoint is down, falls back to the backup endpoint.
 */
public class InstagramFetcher implements SocialMediaFetcher {

    private static final Logger log = LoggerFactory.getLogger(InstagramFetcher.class);

    private static final String PRIMARY_ENDPOINT = "https://graph.instagram.com/v18.0/me/media";
    private static final String BACKUP_ENDPOINT  = "https://graph.facebook.com/v18.0/me/media";

    private final String accessToken;
    private final HttpClient httpClient;

    public InstagramFetcher(String accessToken, HttpClient httpClient) {
        this.accessToken = accessToken;
        this.httpClient  = httpClient;
    }

    @Override
    public Platform platform() {
        return Platform.INSTAGRAM;
    }

    @Override
    public List<RawContent> fetch(TimeRange range) throws FetchException {
        try {
            log.info("INSTAGRAM: fetching {} trending content via primary endpoint", range);
            return fetchFromEndpoint(PRIMARY_ENDPOINT, range);
        } catch (IOException | InterruptedException e) {
            log.warn("INSTAGRAM: primary endpoint unavailable ({}), switching to backup", e.getMessage());
            try {
                return fetchFromEndpoint(BACKUP_ENDPOINT, range);
            } catch (IOException | InterruptedException ex) {
                throw new FetchException(
                        "Instagram primary and backup both failed: " + ex.getMessage(), ex);
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    private List<RawContent> fetchFromEndpoint(String endpoint, TimeRange range)
            throws IOException, InterruptedException {

        String query = buildQuery(range);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "?" + query))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Unexpected HTTP status: " + response.statusCode());
        }

        return parseResponse(response.body());
    }

    private String buildQuery(TimeRange range) {
        Instant cutoff = range.cutoff();
        return "fields=id,caption,media_type,share_count,insights,hashtags,timestamp"
                + "&since=" + cutoff.getEpochSecond()
                + "&access_token=" + accessToken
                + "&limit=100";
    }

    /**
     * Parses the Instagram Graph API JSON response into {@link RawContent} records.
     *
     * TODO: Replace with a real JSON parser once added to pom.xml.
     */
    private List<RawContent> parseResponse(String json) {
        log.debug("INSTAGRAM: parsing response ({} chars)", json.length());
        return List.of();
    }
}

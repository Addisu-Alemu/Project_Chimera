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
 * Fetches most-shared content from TikTok Research API.
 *
 * Rule: If the primary endpoint is down, falls back to the backup endpoint.
 */
public class TikTokFetcher implements SocialMediaFetcher {

    private static final Logger log = LoggerFactory.getLogger(TikTokFetcher.class);

    private static final String PRIMARY_ENDPOINT = "https://open.tiktokapis.com/v2/research/video/query/";
    private static final String BACKUP_ENDPOINT  = "https://open.tiktokapis.com/v2/video/list/";

    private final String accessToken;
    private final HttpClient httpClient;

    public TikTokFetcher(String accessToken, HttpClient httpClient) {
        this.accessToken = accessToken;
        this.httpClient  = httpClient;
    }

    @Override
    public Platform platform() {
        return Platform.TIKTOK;
    }

    @Override
    public List<RawContent> fetch(TimeRange range) throws FetchException {
        try {
            log.info("TIKTOK: fetching {} trending content via primary endpoint", range);
            return fetchFromEndpoint(PRIMARY_ENDPOINT, range);
        } catch (IOException | InterruptedException e) {
            log.warn("TIKTOK: primary endpoint unavailable ({}), switching to backup", e.getMessage());
            try {
                return fetchFromEndpoint(BACKUP_ENDPOINT, range);
            } catch (IOException | InterruptedException ex) {
                throw new FetchException(
                        "TikTok primary and backup both failed: " + ex.getMessage(), ex);
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    private List<RawContent> fetchFromEndpoint(String endpoint, TimeRange range)
            throws IOException, InterruptedException {

        String body = buildRequestBody(range);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Unexpected HTTP status: " + response.statusCode());
        }

        return parseResponse(response.body());
    }

    private String buildRequestBody(TimeRange range) {
        Instant cutoff = range.cutoff();
        // Real implementation: build proper JSON body
        return """
                {
                  "query": { "and": [{ "operation": "GT", "field_name": "create_time", "field_values": ["%s"] }] },
                  "fields": "id,desc,author_name,share_count,view_count,hashtag_names,create_time",
                  "max_count": 100,
                  "sort_type": "0"
                }
                """.formatted(cutoff.getEpochSecond());
    }

    /**
     * Parses the TikTok API JSON response into {@link RawContent} records.
     *
     * TODO: Replace with a real JSON parser once added to pom.xml.
     */
    private List<RawContent> parseResponse(String json) {
        log.debug("TIKTOK: parsing response ({} chars)", json.length());
        return List.of();
    }
}

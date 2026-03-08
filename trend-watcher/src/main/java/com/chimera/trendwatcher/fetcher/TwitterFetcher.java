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
 * Fetches most-shared content from Twitter (X) API v2.
 *
 * Rule: If the primary endpoint is down, falls back to the backup endpoint.
 */
public class TwitterFetcher implements SocialMediaFetcher {

    private static final Logger log = LoggerFactory.getLogger(TwitterFetcher.class);

    private static final String PRIMARY_ENDPOINT  = "https://api.twitter.com/2/tweets/search/recent";
    private static final String BACKUP_ENDPOINT   = "https://api.twitter.com/2/tweets/search/all";

    private final String bearerToken;
    private final HttpClient httpClient;

    public TwitterFetcher(String bearerToken, HttpClient httpClient) {
        this.bearerToken = bearerToken;
        this.httpClient  = httpClient;
    }

    @Override
    public Platform platform() {
        return Platform.TWITTER;
    }

    @Override
    public List<RawContent> fetch(TimeRange range) throws FetchException {
        try {
            log.info("TWITTER: fetching {} trending content via primary endpoint", range);
            return fetchFromEndpoint(PRIMARY_ENDPOINT, range);
        } catch (IOException | InterruptedException e) {
            log.warn("TWITTER: primary endpoint unavailable ({}), switching to backup", e.getMessage());
            try {
                return fetchFromEndpoint(BACKUP_ENDPOINT, range);
            } catch (IOException | InterruptedException ex) {
                throw new FetchException(
                        "Twitter primary and backup both failed: " + ex.getMessage(), ex);
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
                .header("Authorization", "Bearer " + bearerToken)
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
        // Real implementation: encode params properly and add OAuth fields
        return "query=lang%3Aen&sort_order=relevancy&start_time=" + cutoff
                + "&max_results=100&tweet.fields=public_metrics,author_id,entities,created_at";
    }

    /**
     * Parses the Twitter API JSON response into {@link RawContent} records.
     *
     * TODO: Replace with a real JSON parser (e.g. Jackson) once the dependency
     *       is added to pom.xml. Stub returns empty list to keep the scaffold
     *       compilable without extra dependencies.
     */
    private List<RawContent> parseResponse(String json) {
        log.debug("TWITTER: parsing response ({} chars)", json.length());
        // Stub — wire in a JSON parser and map to RawContent records
        return List.of();
    }
}

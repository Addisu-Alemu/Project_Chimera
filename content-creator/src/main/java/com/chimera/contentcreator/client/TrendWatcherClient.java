package com.chimera.contentcreator.client;

import com.chimera.contentcreator.client.dto.TrendReportDto;
import com.chimera.contentcreator.exception.StaleTrendReportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Component
public class TrendWatcherClient {

    private static final Logger log = LoggerFactory.getLogger(TrendWatcherClient.class);

    private final WebClient webClient;

    public TrendWatcherClient(@Value("${trend.watcher.url:http://localhost:8081}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public TrendReportDto getReport(UUID reportId) {
        log.info("Fetching TrendReport reportId={}", reportId);
        return webClient.get()
                .uri("/trend-reports/{reportId}", reportId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    if (response.statusCode().value() == 410) {
                        return response.bodyToMono(String.class)
                                .map(body -> new StaleTrendReportException("TrendReport is stale: " + reportId));
                    }
                    return response.createException();
                })
                .bodyToMono(TrendReportDto.class)
                .block();
    }
}

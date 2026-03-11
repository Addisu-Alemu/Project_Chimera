package com.chimera.contentcreator.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class ContentSafetyFilter {

    private static final Logger log = LoggerFactory.getLogger(ContentSafetyFilter.class);

    private final List<String> blocklist;

    public ContentSafetyFilter() {
        this.blocklist = loadBlocklist();
    }

    public boolean passes(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String keyword : blocklist) {
            if (lower.contains(keyword)) {
                log.warn("SAFETY_FILTER [CREATE]: Discarding content matching blocked keyword '{}'", keyword);
                return false;
            }
        }
        return true;
    }

    private List<String> loadBlocklist() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("safety-blocklist.txt")) {
            if (is == null) {
                log.warn("SAFETY_FILTER [CREATE]: safety-blocklist.txt not found; using empty blocklist");
                return Collections.emptyList();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isBlank() && !line.startsWith("#"))
                        .map(line -> line.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("SAFETY_FILTER [CREATE]: Failed to load blocklist", e);
            return Collections.emptyList();
        }
    }
}

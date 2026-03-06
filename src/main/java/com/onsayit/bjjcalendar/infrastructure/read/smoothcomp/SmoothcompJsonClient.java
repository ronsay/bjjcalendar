package com.onsayit.bjjcalendar.infrastructure.read.smoothcomp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onsayit.bjjcalendar.infrastructure.exception.FetchException;
import com.onsayit.bjjcalendar.infrastructure.read.utils.JsoupFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmoothcompJsonClient {
    private final ObjectMapper mapper;
    private final JsoupFetcher fetcher;

    public List<SmoothcompEvent> fetchEventList(final String url) {
        try {
            final var doc = fetcher.fetch(url)
                    .orElseThrow(() -> new FetchException("Smoothcomp page not found: " + url));

            final var element = doc.select("script").stream()
                    .filter(s -> s.data().contains("var events ="))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No script with events found"));

            final var data = element.data();
            final int start = data.indexOf("var events =");
            final int eq = data.indexOf("=", start);

            var json = data.substring(eq + 1).trim();

            if (json.endsWith(";")) {
                json = json.substring(0, json.length() - 1).trim();
            }

            return mapper.readValue(json, new TypeReference<>() {});

        } catch (final IOException e) {
            throw new FetchException("Smoothcomp fetch error for " + url, e);
        }
    }
}

package com.onsayit.bjjcalendar.infrastructure.read.utils;

import com.onsayit.bjjcalendar.infrastructure.config.properties.SourcesProperties;
import com.onsayit.bjjcalendar.infrastructure.exception.FetchException;
import lombok.RequiredArgsConstructor;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JsoupFetcher {

    private static final String TEXT_HTML = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";

    private final SourcesProperties sourcesProperties;

    public Optional<Document> fetch(final String url) {
        try {
            final var doc = Jsoup.connect(url)
                    .timeout((int) Duration.ofSeconds(sourcesProperties.timeout()).toMillis())
                    .userAgent(ReaderUtils.USER_AGENT)
                    .header(HttpHeaders.ACCEPT, TEXT_HTML)
                    .get();
            return Optional.of(doc);
        } catch (final HttpStatusException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND.value() || e.getStatusCode() == HttpStatus.GONE.value()) {
                return Optional.empty();
            }
            throw new FetchException("HTTP error fetching " + url, e);
        } catch (final IOException e) {
            throw new FetchException("IO error fetching " + url, e);
        }
    }
}

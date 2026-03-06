package com.onsayit.bjjcalendar.infrastructure.read.ajp;

import com.onsayit.bjjcalendar.infrastructure.config.properties.AjpProperties;
import com.onsayit.bjjcalendar.infrastructure.read.utils.JsoupFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AjpHtmlClient {

    private final AjpProperties props;
    private final JsoupFetcher fetcher;

    public Optional<Document> fetchCalendar(final int year) {
        final var url = String.format(props.calendarPattern(), year);
        final var doc = fetcher.fetch(url);
        if (doc.isEmpty()) {
            log.info("AJP: inexistent calendar page: {}", url);
        }
        return doc;
    }
}

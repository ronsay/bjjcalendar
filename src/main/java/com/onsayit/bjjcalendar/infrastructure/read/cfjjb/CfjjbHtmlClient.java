package com.onsayit.bjjcalendar.infrastructure.read.cfjjb;

import com.onsayit.bjjcalendar.infrastructure.config.properties.CfjjbProperties;
import com.onsayit.bjjcalendar.infrastructure.exception.FetchException;
import com.onsayit.bjjcalendar.infrastructure.read.utils.JsoupFetcher;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CfjjbHtmlClient {
    private final CfjjbProperties props;
    private final JsoupFetcher fetcher;

    public Document fetch() {
        final var url = props.baseUrl() + props.calendarPage();
        return fetcher.fetch(url)
                .orElseThrow(() -> new FetchException("CFJJB calendar page not found: " + url));
    }
}

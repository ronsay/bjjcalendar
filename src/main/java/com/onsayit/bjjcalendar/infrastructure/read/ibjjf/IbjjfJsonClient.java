package com.onsayit.bjjcalendar.infrastructure.read.ibjjf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onsayit.bjjcalendar.infrastructure.config.properties.IbjjfProperties;
import com.onsayit.bjjcalendar.infrastructure.config.properties.SourcesProperties;
import com.onsayit.bjjcalendar.infrastructure.exception.FetchException;
import com.onsayit.bjjcalendar.infrastructure.read.utils.ReaderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class IbjjfJsonClient {

    private static final String APPLICATION_JSON = "application/json";
    private static final String X_REQUESTED_WITH = "X-Requested-With";
    private static final String XML_HTTP_REQUEST = "XMLHttpRequest";

    private final ObjectMapper mapper;
    private final IbjjfProperties props;
    private final SourcesProperties sourcesProperties;

    public Optional<List<IbjjfEvent>> fetch() {
        final var cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        final var timeout = Duration.ofSeconds(sourcesProperties.timeout());

        try (var client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(timeout)
                .build()) {

            final var pageReq = HttpRequest.newBuilder(URI.create(props.baseUrl() + props.calendarPage()))
                    .header(HttpHeaders.USER_AGENT, ReaderUtils.USER_AGENT)
                    .GET().build();
            client.send(pageReq, HttpResponse.BodyHandlers.discarding());

            final var jsonBuilder = HttpRequest.newBuilder(URI.create(props.baseUrl() + props.calendarJson()))
                    .header(HttpHeaders.USER_AGENT, ReaderUtils.USER_AGENT)
                    .header(HttpHeaders.ACCEPT, APPLICATION_JSON)
                    .header(HttpHeaders.REFERER, props.calendarPage())
                    .header(HttpHeaders.ORIGIN, props.baseUrl())
                    .header(X_REQUESTED_WITH, XML_HTTP_REQUEST)
                    .timeout(timeout)
                    .GET();

            final var etagFile = Path.of(props.etagFile());
            readEtag(etagFile).ifPresent(et -> jsonBuilder.header(HttpHeaders.IF_NONE_MATCH, et));

            final var res = client.send(jsonBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == HttpStatus.NOT_MODIFIED.value()) {
                log.info("IBJJF: 304 Not Modified (via ETag).");
                return Optional.empty();
            }
            if (res.statusCode() != HttpStatus.OK.value()) {
                throw new IllegalStateException("IBJJF JSON HTTP " + res.statusCode() + ": " + res.body());
            }

            res.headers().firstValue("etag").ifPresent(et -> createETag(etagFile, et));

            return Optional.of(mapper.readValue(res.body(), IbjjfCalendar.class).events());

        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FetchException("IBJJF JSON fetch error", e);
        } catch (final IOException e) {
            throw new FetchException("IBJJF JSON fetch error", e);
        }
    }

    private Optional<String> readEtag(final Path path) {
        try {
            if (Files.exists(path)) {
                final var v = Files.readString(path).trim();
                if (!v.isEmpty()) {
                    return Optional.of(v);
                }
            }
        } catch (final IOException e) {
            log.debug("Cannot read ETag file {}", path, e);
        }
        return Optional.empty();
    }

    private void createETag(final Path path, final String content) {
        try {
            final var parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content);
        } catch (final IOException e) {
            log.warn("Unable to write file {}", path, e);
        }
    }
}

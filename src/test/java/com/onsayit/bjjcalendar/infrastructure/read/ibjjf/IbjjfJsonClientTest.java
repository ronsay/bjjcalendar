package com.onsayit.bjjcalendar.infrastructure.read.ibjjf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onsayit.bjjcalendar.infrastructure.config.properties.IbjjfProperties;
import com.onsayit.bjjcalendar.infrastructure.config.properties.SourcesProperties;
import com.onsayit.bjjcalendar.infrastructure.exception.FetchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IbjjfJsonClientTest {

    @Mock
    private IbjjfProperties props;

    @Mock
    private SourcesProperties sourcesProperties;

    @TempDir
    private Path tempDir;

    private IbjjfJsonClient client;

    @BeforeEach
    void setUp() {
        client = new IbjjfJsonClient(new ObjectMapper(), props, sourcesProperties);
    }

    private void stubAllProperties(final Path etagFile) {
        when(props.baseUrl()).thenReturn("https://ibjjf.com");
        when(props.calendarPage()).thenReturn("/calendar");
        when(props.calendarJson()).thenReturn("/calendar.json");
        when(props.etagFile()).thenReturn(etagFile.toString());
        when(sourcesProperties.timeout()).thenReturn(10);
    }

    private void stubPropertiesForFirstSendOnly(final Path etagFile) {
        when(props.baseUrl()).thenReturn("https://ibjjf.com");
        when(props.calendarPage()).thenReturn("/calendar");
        lenient().when(props.etagFile()).thenReturn(etagFile.toString());
        when(sourcesProperties.timeout()).thenReturn(10);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockJsonResponse(final int statusCode, final String body) {
        final var response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        if (body != null) {
            lenient().when(response.body()).thenReturn(body);
        }
        return response;
    }

    private void stubEtagHeader(final HttpResponse<String> response, final String etag) {
        final var headersMap = etag != null
                ? Map.of("etag", List.of(etag))
                : Map.<String, List<String>>of();
        when(response.headers()).thenReturn(HttpHeaders.of(headersMap, (k, v) -> true));
    }

    @SuppressWarnings("unchecked")
    private HttpClient stubHttpClientBuilder(final MockedStatic<HttpClient> httpClientStatic) {
        final var builder = mock(HttpClient.Builder.class);
        final var httpClient = mock(HttpClient.class);
        httpClientStatic.when(HttpClient::newBuilder).thenReturn(builder);
        when(builder.cookieHandler(any())).thenReturn(builder);
        when(builder.followRedirects(any())).thenReturn(builder);
        when(builder.connectTimeout(any())).thenReturn(builder);
        when(builder.build()).thenReturn(httpClient);
        return httpClient;
    }

    @SuppressWarnings("unchecked")
    private void stubHttpClient(final MockedStatic<HttpClient> httpClientStatic,
                                final HttpResponse<String> jsonResponse)
            throws IOException, InterruptedException {
        final var httpClient = stubHttpClientBuilder(httpClientStatic);
        final var pageResponse = (HttpResponse<Void>) mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(pageResponse)
                .thenReturn(jsonResponse);
    }

    @Nested
    class WhenHttpReturns200 {

        @Test
        void should_return_events_on_success() throws Exception {
            // given
            final var etagFile = tempDir.resolve("etag.txt");
            stubAllProperties(etagFile);
            final var jsonBody = """
                    {"infosite_events":[{"id":1,"name":"World","championshipType":"Championship",
                    "region":"Europe","startDay":5,"endDay":7,"month":"JUN","year":2025,
                    "local":"Arena","city":"Paris","status":"Confirmed","pageUrl":"/e/1"}]}
                    """;
            final var jsonResponse = mockJsonResponse(200, jsonBody);
            stubEtagHeader(jsonResponse, "\"etag-123\"");

            try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
                stubHttpClient(httpClientStatic, jsonResponse);

                // when
                final var result = client.fetch();

                // then
                assertThat(result).isPresent();
                assertThat(result.get()).hasSize(1);
                assertThat(result.get().getFirst().name()).isEqualTo("World");
            }
        }

        @Test
        void should_write_etag_file_on_success() throws Exception {
            // given
            final var etagFile = tempDir.resolve("etag.txt");
            stubAllProperties(etagFile);
            final var jsonResponse = mockJsonResponse(200, "{\"infosite_events\":[]}");
            stubEtagHeader(jsonResponse, "\"new-etag\"");

            try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
                stubHttpClient(httpClientStatic, jsonResponse);

                // when
                client.fetch();

                // then
                assertThat(etagFile).exists();
                assertThat(Files.readString(etagFile)).isEqualTo("\"new-etag\"");
            }
        }

        @Test
        void should_not_write_etag_file_when_no_etag_header() throws Exception {
            // given
            final var etagFile = tempDir.resolve("etag.txt");
            stubAllProperties(etagFile);
            final var jsonResponse = mockJsonResponse(200, "{\"infosite_events\":[]}");
            stubEtagHeader(jsonResponse, null);

            try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
                stubHttpClient(httpClientStatic, jsonResponse);

                // when
                client.fetch();

                // then
                assertThat(etagFile).doesNotExist();
            }
        }

        @Test
        void should_send_etag_header_when_etag_file_exists() throws Exception {
            // given
            final var etagFile = tempDir.resolve("etag.txt");
            Files.writeString(etagFile, "\"existing-etag\"");
            stubAllProperties(etagFile);
            final var jsonResponse = mockJsonResponse(200, "{\"infosite_events\":[]}");
            stubEtagHeader(jsonResponse, null);

            try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
                stubHttpClient(httpClientStatic, jsonResponse);

                // when
                final var result = client.fetch();

                // then
                assertThat(result).isPresent();
            }
        }
    }

    @Nested
    class WhenHttpReturns304 {

        @Test
        void should_return_empty_on_not_modified() throws Exception {
            // given
            final var etagFile = tempDir.resolve("etag.txt");
            stubAllProperties(etagFile);
            final var jsonResponse = mockJsonResponse(304, null);

            try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
                stubHttpClient(httpClientStatic, jsonResponse);

                // when
                final var result = client.fetch();

                // then
                assertThat(result).isEmpty();
            }
        }
    }

    @Nested
    class WhenHttpReturnsUnexpectedStatus {

        @Test
        void should_throw_for_unexpected_status_code() throws Exception {
            // given
            final var etagFile = tempDir.resolve("etag.txt");
            stubAllProperties(etagFile);
            final var jsonResponse = mockJsonResponse(500, "Server Error");

            try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
                stubHttpClient(httpClientStatic, jsonResponse);

                // when / then
                assertThatThrownBy(() -> client.fetch())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("500");
            }
        }
    }

    @Nested
    class WhenNetworkError {

        @SuppressWarnings("unchecked")
        @Test
        void should_throw_fetch_exception_on_io_error() throws Exception {
            // given
            final var etagFile = tempDir.resolve("etag.txt");
            stubPropertiesForFirstSendOnly(etagFile);

            try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
                final var httpClient = stubHttpClientBuilder(httpClientStatic);
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenThrow(new IOException("Connection refused"));

                // when / then
                assertThatThrownBy(() -> client.fetch())
                        .isInstanceOf(FetchException.class)
                        .hasMessageContaining("IBJJF JSON fetch error");
            }
        }

        @SuppressWarnings("unchecked")
        @Test
        void should_throw_fetch_exception_and_interrupt_on_interrupted_exception() throws Exception {
            // given
            final var etagFile = tempDir.resolve("etag.txt");
            stubPropertiesForFirstSendOnly(etagFile);

            try (var httpClientStatic = mockStatic(HttpClient.class)) {
                final var httpClient = stubHttpClientBuilder(httpClientStatic);
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenThrow(new InterruptedException("Interrupted"));

                // when / then
                assertThatThrownBy(() -> client.fetch())
                        .isInstanceOf(FetchException.class)
                        .hasMessageContaining("IBJJF JSON fetch error");
                assertThat(Thread.currentThread().isInterrupted()).isTrue();
            }
        }
    }

    @Nested
    class EtagFileEdgeCases {

        @Test
        void should_ignore_empty_etag_file() throws Exception {
            // given
            final var etagFile = tempDir.resolve("etag.txt");
            Files.writeString(etagFile, "   ");
            stubAllProperties(etagFile);
            final var jsonResponse = mockJsonResponse(200, "{\"infosite_events\":[]}");
            stubEtagHeader(jsonResponse, null);

            try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
                stubHttpClient(httpClientStatic, jsonResponse);

                // when
                final var result = client.fetch();

                // then
                assertThat(result).isPresent();
            }
        }

        @Test
        void should_create_parent_directories_for_etag_file() throws Exception {
            // given
            final var etagFile = tempDir.resolve("sub/dir/etag.txt");
            stubAllProperties(etagFile);
            final var jsonResponse = mockJsonResponse(200, "{\"infosite_events\":[]}");
            stubEtagHeader(jsonResponse, "\"etag-val\"");

            try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
                stubHttpClient(httpClientStatic, jsonResponse);

                // when
                client.fetch();

                // then
                assertThat(etagFile).exists();
                assertThat(Files.readString(etagFile)).isEqualTo("\"etag-val\"");
            }
        }

        @Test
        void should_handle_nonexistent_etag_file() throws Exception {
            // given
            final var etagFile = tempDir.resolve("nonexistent.txt");
            stubAllProperties(etagFile);
            final var jsonResponse = mockJsonResponse(200, "{\"infosite_events\":[]}");
            stubEtagHeader(jsonResponse, null);

            try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
                stubHttpClient(httpClientStatic, jsonResponse);

                // when
                final var result = client.fetch();

                // then
                assertThat(result).isPresent();
            }
        }
    }
}

package com.onsayit.bjjcalendar.infrastructure.read.utils;

import com.onsayit.bjjcalendar.infrastructure.config.properties.SourcesProperties;
import com.onsayit.bjjcalendar.infrastructure.exception.FetchException;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsoupFetcherTest {

    @Mock
    private SourcesProperties sourcesProperties;

    @InjectMocks
    private JsoupFetcher fetcher;

    private Connection stubConnection(final MockedStatic<Jsoup> jsoup) throws IOException {
        when(sourcesProperties.timeout()).thenReturn(10);
        final var connection = mock(Connection.class);
        jsoup.when(() -> Jsoup.connect(anyString())).thenReturn(connection);
        when(connection.timeout(10_000)).thenReturn(connection);
        when(connection.userAgent(anyString())).thenReturn(connection);
        when(connection.header(anyString(), anyString())).thenReturn(connection);
        return connection;
    }

    @Nested
    class WhenHttpSuccess {

        @Test
        void should_return_document() throws IOException {
            try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
                // given
                final var connection = stubConnection(jsoup);
                final var doc = new org.jsoup.nodes.Document("https://example.com");
                when(connection.get()).thenReturn(doc);

                // when
                final var result = fetcher.fetch("https://example.com");

                // then
                assertThat(result).isPresent();
            }
        }
    }

    @Nested
    class WhenHttpError {

        @ParameterizedTest
        @ValueSource(ints = {404, 410})
        void should_return_empty_for_not_found_status(final int statusCode) throws IOException {
            try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
                // given
                final var connection = stubConnection(jsoup);
                when(connection.get()).thenThrow(new HttpStatusException("Error", statusCode, "https://example.com"));

                // when
                final var result = fetcher.fetch("https://example.com");

                // then
                assertThat(result).isEmpty();
            }
        }

        @Test
        void should_throw_fetch_exception_for_500() throws IOException {
            try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
                // given
                final var connection = stubConnection(jsoup);
                when(connection.get()).thenThrow(new HttpStatusException("Server Error", 500, "https://example.com"));

                // when / then
                assertThatThrownBy(() -> fetcher.fetch("https://example.com"))
                        .isInstanceOf(FetchException.class);
            }
        }

        @Test
        void should_throw_fetch_exception_for_io_error() throws IOException {
            try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
                // given
                final var connection = stubConnection(jsoup);
                when(connection.get()).thenThrow(new IOException("Connection refused"));

                // when / then
                assertThatThrownBy(() -> fetcher.fetch("https://example.com"))
                        .isInstanceOf(FetchException.class);
            }
        }
    }
}

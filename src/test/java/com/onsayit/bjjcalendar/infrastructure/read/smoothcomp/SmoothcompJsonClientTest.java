package com.onsayit.bjjcalendar.infrastructure.read.smoothcomp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onsayit.bjjcalendar.infrastructure.exception.FetchException;
import com.onsayit.bjjcalendar.infrastructure.read.utils.JsoupFetcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmoothcompJsonClientTest {

    @Spy
    private ObjectMapper mapper;

    @Mock
    private JsoupFetcher fetcher;

    @InjectMocks
    private SmoothcompJsonClient client;

    private Document documentWithScript(final String scriptContent) {
        final var html = "<html><head><script>" + scriptContent + "</script></head><body></body></html>";
        return Jsoup.parse(html);
    }

    @Nested
    class WhenFetchSucceeds {

        @Test
        void should_extract_events_from_script_with_semicolon() {
            // given
            final var script = "var events = [{\"id\":1,\"title\":\"Open Paris\",\"url\":\"/e/1\","
                    + "\"location_city\":\"Paris\",\"location_country_human\":\"France\","
                    + "\"location_country\":\"FR\",\"startdate\":\"2025-06-01\",\"enddate\":\"2025-06-02\"}];";
            when(fetcher.fetch("https://smoothcomp.com/en/events")).thenReturn(Optional.of(documentWithScript(script)));

            // when
            final var result = client.fetchEventList("https://smoothcomp.com/en/events");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().title()).isEqualTo("Open Paris");
            assertThat(result.getFirst().city()).isEqualTo("Paris");
            assertThat(result.getFirst().country()).isEqualTo("France");
            assertThat(result.getFirst().countryCode()).isEqualTo("FR");
        }

        @Test
        void should_extract_events_from_script_without_semicolon() {
            // given
            final var script = "var events = [{\"id\":2,\"title\":\"Open Lyon\",\"url\":\"/e/2\","
                    + "\"location_city\":\"Lyon\",\"location_country_human\":\"France\","
                    + "\"location_country\":\"FR\",\"startdate\":\"2025-07-01\",\"enddate\":\"2025-07-02\"}]";
            when(fetcher.fetch("https://smoothcomp.com/en/events")).thenReturn(Optional.of(documentWithScript(script)));

            // when
            final var result = client.fetchEventList("https://smoothcomp.com/en/events");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().title()).isEqualTo("Open Lyon");
        }

        @Test
        void should_return_empty_list_when_no_events() {
            // given
            final var script = "var events = [];";
            when(fetcher.fetch("https://smoothcomp.com/en/events")).thenReturn(Optional.of(documentWithScript(script)));

            // when
            final var result = client.fetchEventList("https://smoothcomp.com/en/events");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void should_extract_multiple_events() {
            // given
            final var script = "var events = ["
                    + "{\"id\":1,\"title\":\"E1\",\"url\":\"/e/1\","
                    + "\"location_city\":\"C1\",\"location_country_human\":\"Country1\","
                    + "\"location_country\":\"C1\",\"startdate\":\"2025-01-01\",\"enddate\":\"2025-01-02\"},"
                    + "{\"id\":2,\"title\":\"E2\",\"url\":\"/e/2\","
                    + "\"location_city\":\"C2\",\"location_country_human\":\"Country2\","
                    + "\"location_country\":\"C2\",\"startdate\":\"2025-02-01\",\"enddate\":\"2025-02-02\"}"
                    + "];";
            when(fetcher.fetch("https://smoothcomp.com/en/events")).thenReturn(Optional.of(documentWithScript(script)));

            // when
            final var result = client.fetchEventList("https://smoothcomp.com/en/events");

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    class WhenFetchFails {

        @Test
        void should_throw_when_fetch_returns_empty() {
            // given
            when(fetcher.fetch("https://smoothcomp.com/en/events")).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> client.fetchEventList("https://smoothcomp.com/en/events"))
                    .isInstanceOf(FetchException.class)
                    .hasMessageContaining("Smoothcomp page not found");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "<html><head><script>var x = 1;</script></head><body></body></html>",
                "<html><body><p>No scripts</p></body></html>"
        })
        void should_throw_when_no_events_script_found(final String html) {
            // given
            when(fetcher.fetch("https://smoothcomp.com/en/events"))
                    .thenReturn(Optional.of(Jsoup.parse(html)));

            // when / then
            assertThatThrownBy(() -> client.fetchEventList("https://smoothcomp.com/en/events"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("No script with events found");
        }

        @Test
        void should_wrap_io_exception_in_fetch_exception() {
            // given
            final var script = "var events = INVALID_JSON;";
            when(fetcher.fetch("https://smoothcomp.com/en/events")).thenReturn(Optional.of(documentWithScript(script)));

            // when / then
            assertThatThrownBy(() -> client.fetchEventList("https://smoothcomp.com/en/events"))
                    .isInstanceOf(FetchException.class)
                    .hasMessageContaining("Smoothcomp fetch error for https://smoothcomp.com/en/events");
        }
    }
}

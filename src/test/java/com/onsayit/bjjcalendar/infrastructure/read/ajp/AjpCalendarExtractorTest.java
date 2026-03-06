package com.onsayit.bjjcalendar.infrastructure.read.ajp;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AjpCalendarExtractorTest {

    private final AjpCalendarExtractor extractor = new AjpCalendarExtractor();

    private Element container(final String html) {
        return Jsoup.parse("<div>" + html + "</div>").selectFirst("div");
    }

    @Nested
    class BasicExtraction {

        @Test
        void should_extract_event_with_date_and_location() {
            // given
            final var html = "<p><a href=\"https://ajp.com/event/slug-1\">AJP Tour Paris</a> MAR 15-17 "
                    + "@ Arena, Paris, France</p>";

            // when
            final var result = extractor.extractFromContainer(container(html), 2025);

            // then
            assertThat(result).hasSize(1);
            final var event = result.getFirst();
            assertThat(event.name()).isEqualTo("AJP Tour Paris");
            assertThat(event.month()).isEqualTo("MAR");
            assertThat(event.startDay()).isEqualTo("15");
            assertThat(event.endDay()).isEqualTo("17");
            assertThat(event.venue()).isEqualTo("Arena");
            assertThat(event.city()).isEqualTo("Paris");
            assertThat(event.country()).isEqualTo("France");
            assertThat(event.year()).isEqualTo(2025);
        }

        @Test
        void should_extract_event_without_end_day() {
            // given
            final var html = "<p><a href=\"https://ajp.com/event/slug-2\">Single Day Event</a> JUN 10 "
                    + "@ Gym, London</p>";

            // when
            final var result = extractor.extractFromContainer(container(html), 2025);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().startDay()).isEqualTo("10");
            assertThat(result.getFirst().endDay()).isNull();
        }

        @Test
        void should_extract_url() {
            // given
            final var html = "<p><a href=\"https://ajp.com/event/test-event\">Test</a> JAN 1 @ Venue, City</p>";

            // when
            final var result = extractor.extractFromContainer(container(html), 2025);

            // then
            assertThat(result.getFirst().url()).isEqualTo("https://ajp.com/event/test-event");
        }
    }

    @Nested
    class MultipleEvents {

        @Test
        void should_extract_multiple_events() {
            // given
            final var html = """
                    <p><a href="/e/1">Event 1</a> JAN 10-11 @ V1, C1</p>
                    <p><a href="/e/2">Event 2</a> FEB 20-22 @ V2, C2</p>
                    """;

            // when
            final var result = extractor.extractFromContainer(container(html), 2025);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("Event 1");
            assertThat(result.get(1).name()).isEqualTo("Event 2");
        }
    }

    @Nested
    class LocationFromNextElement {

        @Test
        void should_extract_location_from_next_element() {
            // given
            final var html = """
                    <p><a href="/e/1">Event</a> MAR 15-17</p>
                    <p>Arena, Paris, France</p>
                    """;

            // when
            final var result = extractor.extractFromContainer(container(html), 2025);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().venue()).isEqualTo("Arena");
            assertThat(result.getFirst().city()).isEqualTo("Paris");
        }

        @Test
        void should_not_consume_next_element_if_it_has_a_link() {
            // given
            final var html = """
                    <p><a href="/e/1">Event 1</a> MAR 15</p>
                    <p><a href="/e/2">Event 2</a> APR 20 @ V, C</p>
                    """;

            // when
            final var result = extractor.extractFromContainer(container(html), 2025);

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    class FlagEmoji {

        @Test
        void should_extract_country_code_from_flag_emoji() {
            // given — \uD83C\uDDEB\uD83C\uDDF7 = 🇫🇷 (FR)
            final var html = "<p><a href=\"/e/1\">Event</a> MAR 15 @ Arena, Paris \uD83C\uDDEB\uD83C\uDDF7</p>";

            // when
            final var result = extractor.extractFromContainer(container(html), 2025);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().countryCode()).isEqualTo("FR");
        }

        @Test
        void should_return_null_country_code_when_no_flag() {
            // given
            final var html = "<p><a href=\"/e/1\">Event</a> MAR 15 @ Arena, Paris</p>";

            // when
            final var result = extractor.extractFromContainer(container(html), 2025);

            // then
            assertThat(result.getFirst().countryCode()).isNull();
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void should_handle_empty_container() {
            // when
            final var result = extractor.extractFromContainer(container(""), 2025);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void should_skip_paragraphs_without_links() {
            // given
            final var html = "<p>Some text without link</p>";

            // when
            final var result = extractor.extractFromContainer(container(html), 2025);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void should_handle_link_without_date_or_location() {
            // given
            final var html = "<p><a href=\"/e/1\">Just A Name</a></p>";

            // when
            final var result = extractor.extractFromContainer(container(html), 2025);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().name()).isEqualTo("Just A Name");
            assertThat(result.getFirst().month()).isNull();
            assertThat(result.getFirst().startDay()).isNull();
        }
    }

    @Nested
    class DatePatternVariations {

        @ParameterizedTest
        @CsvSource({
                "'jun 15-17', 'jun', '15'",
                "'JUNE 15-17', 'JUNE', '15'"
        })
        void should_handle_month_format(final String dateSegment,
                                         final String expectedMonth,
                                         final String expectedStartDay) {
            // given
            final var html = "<p><a href=\"/e/1\">Event</a> " + dateSegment + " @ V, C</p>";

            // when
            final var result = extractor.extractFromContainer(container(html), 2025);

            // then
            assertThat(result.getFirst().month()).isEqualTo(expectedMonth);
            assertThat(result.getFirst().startDay()).isEqualTo(expectedStartDay);
        }
    }
}

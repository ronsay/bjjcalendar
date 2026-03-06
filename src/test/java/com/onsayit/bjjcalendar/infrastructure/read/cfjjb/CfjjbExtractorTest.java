package com.onsayit.bjjcalendar.infrastructure.read.cfjjb;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class CfjjbExtractorTest {

    private final CfjjbExtractor extractor = new CfjjbExtractor();

    private Document buildDocument(final String monthHeader, final String eventHtml) {
        return Jsoup.parse("""
                <div class="page1">
                    <div class="text-blue-900">%s</div>
                    <div>
                        <div class="bg-white">
                            <ul>
                                %s
                            </ul>
                        </div>
                    </div>
                </div>
                """.formatted(monthHeader, eventHtml));
    }

    private String eventLi(final String title, final String dateText, final String city, final String url) {
        return """
                <li>
                    <p id="compet_1">%s</p>
                    <div class="flex items-center text-sm text-black w-64"><div>%s</div></div>
                    <svg viewBox="0 0 20 20"></svg><p>%s</p>
                    <a class="btn" href="%s">Voir</a>
                </li>
                """.formatted(title, dateText, city, url);
    }

    @Nested
    class MonthHeaderParsing {

        @ParameterizedTest
        @CsvSource({
                "Octobre 2025, 'Le 7 octobre', 2025, 10",
                "Février 2026, 'Le 7 février', 2026, 2"
        })
        void should_parse_month_header(final String header, final String dateText,
                                        final int year, final int month) {
            // given
            final var doc = buildDocument(header, eventLi("Event", dateText, "Paris", "/c/1"));

            // when
            final var result = extractor.extract(doc);

            // then
            assertThat(result.getFirst().yearMonth()).isEqualTo(YearMonth.of(year, month));
        }

        @Test
        void should_skip_invalid_month_header() {
            // given
            final var doc = buildDocument("Invalid Header",
                    eventLi("Event", "Le 7 octobre", "Paris", "/c/1"));

            // when
            final var result = extractor.extract(doc);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class SingleDateParsing {

        @Test
        void should_parse_single_date_le_format() {
            // given
            final var doc = buildDocument("Octobre 2025",
                    eventLi("Championnat", "Le 7 octobre", "Lyon", "/c/1"));

            // when
            final var result = extractor.extract(doc);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().startDay()).isEqualTo("7");
            assertThat(result.getFirst().endDay()).isEqualTo("7");
        }
    }

    @Nested
    class DateRangeParsing {

        @ParameterizedTest
        @CsvSource({
                "Novembre 2025, 'Du 29 novembre au 30 novembre', 29, 30",
                "Janvier 2026, 'Du 31 janvier au 1 février', 31, 1",
                "Juin 2025, 'Du 10 juin au 12', 10, 12"
        })
        void should_parse_date_range(final String header, final String dateText,
                                      final String startDay, final String endDay) {
            // given
            final var doc = buildDocument(header, eventLi("Event", dateText, "Paris", "/c/1"));

            // when
            final var result = extractor.extract(doc);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().startDay()).isEqualTo(startDay);
            assertThat(result.getFirst().endDay()).isEqualTo(endDay);
        }
    }

    @Nested
    class EventExtraction {

        @Test
        void should_extract_event_title() {
            // given
            final var doc = buildDocument("Octobre 2025",
                    eventLi("Championnat de France", "Le 7 octobre", "Paris", "/c/1"));

            // when
            final var result = extractor.extract(doc);

            // then
            assertThat(result.getFirst().name()).isEqualTo("Championnat de France");
        }

        @Test
        void should_extract_city() {
            // given
            final var doc = buildDocument("Octobre 2025",
                    eventLi("Event", "Le 7 octobre", "Marseille", "/c/1"));

            // when
            final var result = extractor.extract(doc);

            // then
            assertThat(result.getFirst().city()).isEqualTo("Marseille");
        }

        @Test
        void should_extract_url() {
            // given
            final var doc = buildDocument("Octobre 2025",
                    eventLi("Event", "Le 7 octobre", "Paris", "/competitions/123"));

            // when
            final var result = extractor.extract(doc);

            // then
            assertThat(result.getFirst().url()).isEqualTo("/competitions/123");
        }
    }

    @Nested
    class MultipleEvents {

        @Test
        void should_extract_multiple_events_from_same_month() {
            // given
            final var events = eventLi("Event 1", "Le 7 octobre", "Paris", "/c/1")
                    + eventLi("Event 2", "Le 14 octobre", "Lyon", "/c/2");
            final var doc = buildDocument("Octobre 2025", events);

            // when
            final var result = extractor.extract(doc);

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void should_handle_empty_document() {
            // given
            final var doc = Jsoup.parse("<div></div>");

            // when
            final var result = extractor.extract(doc);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void should_handle_document_without_page1() {
            // given
            final var doc = Jsoup.parse("<div class='other'></div>");

            // when
            final var result = extractor.extract(doc);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void should_skip_li_without_competition_title() {
            // given
            final var html = """
                    <div class="page1">
                        <div class="text-blue-900">Octobre 2025</div>
                        <div>
                            <div class="bg-white">
                                <ul>
                                    <li><p>Not a competition title</p></li>
                                </ul>
                            </div>
                        </div>
                    </div>
                    """;
            final var doc = Jsoup.parse(html);

            // when
            final var result = extractor.extract(doc);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void should_skip_event_with_empty_date() {
            // given
            final var doc = buildDocument("Octobre 2025",
                    eventLi("Event", "", "Paris", "/c/1"));

            // when
            final var result = extractor.extract(doc);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void should_handle_date_with_asterisk_suffix() {
            // given
            final var doc = buildDocument("Octobre 2025",
                    eventLi("Event", "Le 7 octobre * sous réserve", "Paris", "/c/1"));

            // when
            final var result = extractor.extract(doc);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().startDay()).isEqualTo("7");
        }
    }
}

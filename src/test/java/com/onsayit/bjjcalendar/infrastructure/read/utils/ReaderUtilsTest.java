package com.onsayit.bjjcalendar.infrastructure.read.utils;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.model.Federation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ReaderUtilsTest {

    @Nested
    class GetMonth {

        @ParameterizedTest
        @CsvSource({
                "jan, JANUARY",
                "feb, FEBRUARY",
                "mar, MARCH",
                "apr, APRIL",
                "may, MAY",
                "jun, JUNE",
                "jul, JULY",
                "aug, AUGUST",
                "sep, SEPTEMBER",
                "sept, SEPTEMBER",
                "oct, OCTOBER",
                "nov, NOVEMBER",
                "dec, DECEMBER"
        })
        void should_resolve_english_abbreviations(final String input, final Month expected) {
            assertThat(ReaderUtils.getMonth(input)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
                "january, JANUARY",
                "february, FEBRUARY",
                "march, MARCH",
                "april, APRIL",
                "june, JUNE",
                "july, JULY",
                "august, AUGUST",
                "september, SEPTEMBER",
                "october, OCTOBER",
                "november, NOVEMBER",
                "december, DECEMBER"
        })
        void should_resolve_english_full_names(final String input, final Month expected) {
            assertThat(ReaderUtils.getMonth(input)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
                "janvier, JANUARY",
                "fevrier, FEBRUARY",
                "mars, MARCH",
                "avril, APRIL",
                "mai, MAY",
                "juin, JUNE",
                "juillet, JULY",
                "aout, AUGUST",
                "septembre, SEPTEMBER",
                "octobre, OCTOBER",
                "novembre, NOVEMBER",
                "decembre, DECEMBER"
        })
        void should_resolve_french_names(final String input, final Month expected) {
            assertThat(ReaderUtils.getMonth(input)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
                "février, FEBRUARY",
                "décembre, DECEMBER",
                "août, AUGUST"
        })
        void should_resolve_french_names_with_accents(final String input, final Month expected) {
            assertThat(ReaderUtils.getMonth(input)).isEqualTo(expected);
        }

        @Test
        void should_be_case_insensitive() {
            assertThat(ReaderUtils.getMonth("JANUARY")).isEqualTo(Month.JANUARY);
            assertThat(ReaderUtils.getMonth("January")).isEqualTo(Month.JANUARY);
            assertThat(ReaderUtils.getMonth("JAN")).isEqualTo(Month.JANUARY);
        }

        @Test
        void should_return_null_when_unknown() {
            assertThat(ReaderUtils.getMonth("invalid")).isNull();
        }
    }

    @Nested
    class GenerateId {

        @Test
        void should_generate_id_with_long() {
            assertThat(ReaderUtils.generateId(Federation.IBJJF, 42L)).isEqualTo("ibjjf-42");
        }

        @Test
        void should_generate_id_with_string() {
            assertThat(ReaderUtils.generateId(Federation.AJP, "some-slug")).isEqualTo("ajp-some-slug");
        }

        @Test
        void should_lowercase_federation() {
            assertThat(ReaderUtils.generateId(Federation.GRAPPLING_INDUSTRIES, "1"))
                    .isEqualTo("grappling_industries-1");
        }
    }

    @Nested
    class ParseDate {

        @Test
        void should_parse_iso_date() {
            assertThat(ReaderUtils.parseDate("2025-06-15")).isEqualTo(LocalDate.of(2025, 6, 15));
        }

        @Test
        void should_parse_date_with_trailing_content() {
            assertThat(ReaderUtils.parseDate("2025-06-15T10:00:00Z"))
                    .isEqualTo(LocalDate.of(2025, 6, 15));
        }
    }

    @Nested
    class ResolveEndDate {

        @Test
        void should_resolve_same_month() {
            // given
            final var startDate = LocalDate.of(2025, 6, 10);

            // when
            final var endDate = ReaderUtils.resolveEndDate(15, startDate);

            // then
            assertThat(endDate).isEqualTo(LocalDate.of(2025, 6, 15));
        }

        @Test
        void should_resolve_next_month_when_end_day_smaller() {
            // given
            final var startDate = LocalDate.of(2025, 6, 28);

            // when
            final var endDate = ReaderUtils.resolveEndDate(2, startDate);

            // then
            assertThat(endDate).isEqualTo(LocalDate.of(2025, 7, 2));
        }

        @Test
        void should_resolve_next_year_when_december() {
            // given
            final var startDate = LocalDate.of(2025, 12, 30);

            // when
            final var endDate = ReaderUtils.resolveEndDate(2, startDate);

            // then
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 1, 2));
        }
    }

    @Nested
    class NormalizeWhitespace {

        @ParameterizedTest
        @CsvSource({
                "'  hello  world  ', 'hello world'",
                "'hello\tworld', 'hello world'",
                "'', ''"
        })
        void should_normalize_whitespace(final String input, final String expected) {
            assertThat(ReaderUtils.normalizeWhitespace(input)).isEqualTo(expected);
        }

        @Test
        void should_replace_non_breaking_spaces() {
            assertThat(ReaderUtils.normalizeWhitespace("hello\u00A0world")).isEqualTo("hello world");
        }

        @Test
        void should_return_empty_for_null() {
            assertThat(ReaderUtils.normalizeWhitespace(null)).isEmpty();
        }
    }

    @Nested
    class MapAndFilter {

        @Test
        void should_map_and_filter_null_results() {
            // given
            final Stream<String> input = Stream.of("a", "b");

            // when
            final var result = ReaderUtils.mapAndFilter(input, s -> null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void should_filter_past_events() {
            // given
            final var pastEvent = new Event("1", Federation.IBJJF, "Past", "V", "C", "FR",
                    LocalDate.now().minusDays(5), LocalDate.now().minusDays(4), Map.of());
            final var futureEvent = new Event("2", Federation.IBJJF, "Future", "V", "C", "FR",
                    LocalDate.now().plusDays(5), LocalDate.now().plusDays(6), Map.of());

            // when
            final var result = ReaderUtils.mapAndFilter(Stream.of(pastEvent, futureEvent),
                    event -> event);

            // then
            assertThat(result).containsExactly(futureEvent);
        }

        @Test
        void should_keep_events_starting_today() {
            // given
            final var todayEvent = new Event("1", Federation.IBJJF, "Today", "V", "C", "FR",
                    LocalDate.now(), LocalDate.now().plusDays(1), Map.of());

            // when
            final var result = ReaderUtils.mapAndFilter(Stream.of(todayEvent), event -> event);

            // then
            assertThat(result).containsExactly(todayEvent);
        }

        @Test
        void should_keep_events_ending_today() {
            // given
            final var endingTodayEvent = new Event("1", Federation.IBJJF, "Ending", "V", "C", "FR",
                    LocalDate.now().minusDays(1), LocalDate.now(), Map.of());

            // when
            final var result = ReaderUtils.mapAndFilter(Stream.of(endingTodayEvent), event -> event);

            // then
            assertThat(result).containsExactly(endingTodayEvent);
        }
    }

    @Nested
    class BuildMergedEvent {

        @Test
        void should_merge_events_with_earliest_start_and_latest_end() {
            // given
            final var e1 = new Event("1", Federation.IBJJF, "A", "V", "C", "FR",
                    LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 11), Map.of());
            final var e2 = new Event("2", Federation.IBJJF, "B", "V", "C", "FR",
                    LocalDate.of(2025, 6, 8), LocalDate.of(2025, 6, 12), Map.of());
            final var urls = Map.of("Gi", "url1", "No-Gi", "url2");

            // when
            final var merged = ReaderUtils.buildMergedEvent(e1, "Merged", List.of(e1, e2), urls);

            // then
            assertThat(merged.id()).isEqualTo("1");
            assertThat(merged.name()).isEqualTo("Merged");
            assertThat(merged.startDate()).isEqualTo(LocalDate.of(2025, 6, 8));
            assertThat(merged.endDate()).isEqualTo(LocalDate.of(2025, 6, 12));
            assertThat(merged.urls()).isEqualTo(urls);
        }
    }

    @Nested
    class ExtractLastPathSegment {

        @Test
        void should_extract_last_segment() {
            assertThat(ReaderUtils.extractLastPathSegment("https://example.com/events/123"))
                    .isEqualTo("123");
        }

        @Test
        void should_return_empty_for_null() {
            assertThat(ReaderUtils.extractLastPathSegment(null)).isEmpty();
        }

        @Test
        void should_return_empty_for_blank() {
            assertThat(ReaderUtils.extractLastPathSegment("  ")).isEmpty();
        }

        @Test
        void should_return_input_when_no_slash() {
            assertThat(ReaderUtils.extractLastPathSegment("no-slash")).isEqualTo("no-slash");
        }
    }

    @Nested
    class Slugify {

        @ParameterizedTest
        @CsvSource({
                "'Hello World', 'hello-world'",
                "'Paris 2025!', 'paris-2025'",
                "'Café São Paulo', 'caf-s-o-paulo'",
                "'  multiple   spaces  ', 'multiple-spaces'"
        })
        void should_slugify(final String input, final String expected) {
            assertThat(ReaderUtils.slugify(input)).isEqualTo(expected);
        }

        @Test
        void should_remove_leading_and_trailing_dashes() {
            assertThat(ReaderUtils.slugify(" -test- ")).isEqualTo("test");
        }
    }
}

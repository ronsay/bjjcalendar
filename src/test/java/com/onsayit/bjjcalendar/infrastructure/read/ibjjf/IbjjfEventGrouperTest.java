package com.onsayit.bjjcalendar.infrastructure.read.ibjjf;

import com.onsayit.bjjcalendar.TestEventFactory;
import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.model.Federation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IbjjfEventGrouperTest {

    private final IbjjfEventGrouper grouper = new IbjjfEventGrouper();

    private Event ibjjfEvent(final String id, final String name, final String city,
                              final int startDay, final int endDay,
                              final Map<String, String> urls) {
        return TestEventFactory.create(Federation.IBJJF, id, name,
                city, LocalDate.of(2025, 6, startDay), LocalDate.of(2025, 6, endDay), urls);
    }

    @Nested
    class NameNormalization {

        @Test
        void should_group_gi_and_no_gi_variants() {
            // given
            final var gi = ibjjfEvent("1", "European Championship", "Paris", 10, 11,
                    Map.of("Gi", "https://gi.com"));
            final var noGi = ibjjfEvent("2", "No-Gi European Championship", "Paris", 10, 12,
                    Map.of("No-Gi", "https://nogi.com"));

            // when
            final var result = grouper.group(List.of(gi, noGi));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().urls()).containsKeys("Gi", "No-Gi");
        }

        @Test
        void should_group_kids_variant() {
            // given
            final var main = ibjjfEvent("1", "European Championship", "Paris", 10, 11,
                    Map.of("Gi", "https://gi.com"));
            final var kids = ibjjfEvent("2", "Kids European Championship", "Paris", 10, 10,
                    Map.of("Kids", "https://kids.com"));

            // when
            final var result = grouper.group(List.of(main, kids));

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        void should_strip_trailing_year() {
            // given
            final var e1 = ibjjfEvent("1", "European Championship 2025", "Paris", 10, 11,
                    Map.of("Gi", "https://gi.com"));
            final var e2 = ibjjfEvent("2", "European Championship", "Paris", 10, 11,
                    Map.of("No-Gi", "https://nogi.com"));

            // when
            final var result = grouper.group(List.of(e1, e2));

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    class DateContinuity {

        @Test
        void should_merge_events_with_continuous_dates() {
            // given
            final var e1 = ibjjfEvent("1", "Event", "Paris", 10, 11,
                    Map.of("Gi", "https://gi.com"));
            final var e2 = ibjjfEvent("2", "Event", "Paris", 12, 13,
                    Map.of("No-Gi", "https://nogi.com"));

            // when
            final var result = grouper.group(List.of(e1, e2));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().startDate()).isEqualTo(LocalDate.of(2025, 6, 10));
            assertThat(result.getFirst().endDate()).isEqualTo(LocalDate.of(2025, 6, 13));
        }

        @Test
        void should_split_events_with_date_gap() {
            // given
            final var e1 = ibjjfEvent("1", "Event", "Paris", 10, 11,
                    Map.of("Gi", "https://gi.com"));
            final var e2 = ibjjfEvent("2", "Event", "Paris", 20, 21,
                    Map.of("Gi", "https://gi2.com"));

            // when
            final var result = grouper.group(List.of(e1, e2));

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        void should_allow_one_day_gap() {
            // given
            final var e1 = ibjjfEvent("1", "Event", "Paris", 10, 11,
                    Map.of("Gi", "https://gi.com"));
            final var e2 = ibjjfEvent("2", "Event", "Paris", 12, 13,
                    Map.of("No-Gi", "https://nogi.com"));

            // when
            final var result = grouper.group(List.of(e1, e2));

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    class GiFirst {

        @Test
        void should_prefer_gi_event_as_primary() {
            // given
            final var noGi = ibjjfEvent("2", "No-Gi Event", "Paris", 10, 11,
                    Map.of("No-Gi", "https://nogi.com"));
            final var gi = ibjjfEvent("1", "Event", "Paris", 10, 11,
                    Map.of("Gi", "https://gi.com"));

            // when
            final var result = grouper.group(List.of(noGi, gi));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().name()).isEqualTo("Event");
        }
    }

    @Nested
    class CityGrouping {

        @Test
        void should_not_group_events_from_different_cities() {
            // given
            final var e1 = ibjjfEvent("1", "Event", "Paris", 10, 11,
                    Map.of("Gi", "https://gi.com"));
            final var e2 = ibjjfEvent("2", "Event", "London", 10, 11,
                    Map.of("Gi", "https://gi2.com"));

            // when
            final var result = grouper.group(List.of(e1, e2));

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Test
    void should_handle_single_event() {
        // given
        final var event = ibjjfEvent("1", "Solo Event", "Paris", 10, 11,
                Map.of("Gi", "https://gi.com"));

        // when
        final var result = grouper.group(List.of(event));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Solo Event");
    }

    @Test
    void should_handle_empty_list() {
        assertThat(grouper.group(List.of())).isEmpty();
    }

    @Test
    void should_merge_urls_from_all_group_members() {
        // given
        final var e1 = ibjjfEvent("1", "Event", "Paris", 10, 11,
                Map.of("Gi", "https://gi.com"));
        final var e2 = ibjjfEvent("2", "No-Gi Event", "Paris", 10, 12,
                Map.of("No-Gi", "https://nogi.com"));
        final var e3 = ibjjfEvent("3", "Kids Event", "Paris", 10, 10,
                Map.of("Kids", "https://kids.com"));

        // when
        final var result = grouper.group(List.of(e1, e2, e3));

        // then
        assertThat(result).hasSize(1);
        final var mergedUrls = result.getFirst().urls();
        assertThat(mergedUrls).containsKeys("Gi", "No-Gi", "Kids");
    }
}

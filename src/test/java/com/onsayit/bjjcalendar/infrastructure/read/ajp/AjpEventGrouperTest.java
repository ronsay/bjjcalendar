package com.onsayit.bjjcalendar.infrastructure.read.ajp;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.model.Federation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AjpEventGrouperTest {

    private final AjpEventGrouper grouper = new AjpEventGrouper();

    private Event buildEvent(final String id, final String name,
                              final LocalDate startDate, final LocalDate endDate) {
        return new Event(id, Federation.AJP, name, "Arena", "Paris", "France",
                startDate, endDate, Map.of("", "https://ajp.com/e/" + id));
    }

    @Test
    void should_group_events_by_id() {
        // given
        final var e1 = buildEvent("ajp-slug", "AJP Tour - GI",
                LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 11));
        final var e2 = buildEvent("ajp-slug", "AJP Tour - NO GI",
                LocalDate.of(2025, 6, 12), LocalDate.of(2025, 6, 13));

        // when
        final var result = grouper.group(List.of(e1, e2));

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    void should_not_group_different_ids() {
        // given
        final var e1 = buildEvent("ajp-a", "Event A",
                LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 11));
        final var e2 = buildEvent("ajp-b", "Event B",
                LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 11));

        // when
        final var result = grouper.group(List.of(e1, e2));

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    void should_clean_name_removing_trailing_dash() {
        // given
        final var event = buildEvent("ajp-slug", "AJP Tour - GI",
                LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 11));

        // when
        final var result = grouper.group(List.of(event));

        // then
        assertThat(result.getFirst().name()).isEqualTo("AJP Tour");
    }

    @Test
    void should_keep_name_without_trailing_dash() {
        // given
        final var event = buildEvent("ajp-slug", "AJP Grand Slam",
                LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 11));

        // when
        final var result = grouper.group(List.of(event));

        // then
        assertThat(result.getFirst().name()).isEqualTo("AJP Grand Slam");
    }

    @Test
    void should_merge_start_and_end_dates() {
        // given
        final var e1 = buildEvent("ajp-slug", "AJP Tour - GI",
                LocalDate.of(2025, 6, 12), LocalDate.of(2025, 6, 13));
        final var e2 = buildEvent("ajp-slug", "AJP Tour - NO GI",
                LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 11));

        // when
        final var result = grouper.group(List.of(e1, e2));

        // then
        assertThat(result.getFirst().startDate()).isEqualTo(LocalDate.of(2025, 6, 10));
        assertThat(result.getFirst().endDate()).isEqualTo(LocalDate.of(2025, 6, 13));
    }

    @Test
    void should_handle_empty_list() {
        assertThat(grouper.group(List.of())).isEmpty();
    }

    @Test
    void should_handle_single_event() {
        // given
        final var event = buildEvent("ajp-slug", "AJP Tour",
                LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 11));

        // when
        final var result = grouper.group(List.of(event));

        // then
        assertThat(result).hasSize(1);
    }
}

package com.onsayit.bjjcalendar.infrastructure.read.cfjjb;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.model.Federation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CfjjbEventGrouperTest {

    private final CfjjbEventGrouper grouper = new CfjjbEventGrouper();

    private Event buildEvent(final String id, final String name, final String url) {
        return new Event(id, Federation.CFJJB, name, null, "Paris", "France",
                LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 11),
                url != null ? Map.of("", url) : Map.of());
    }

    @Nested
    class UrlKeyResolution {

        @ParameterizedTest
        @CsvSource({
                "'Championnat GI', 'GI'",
                "'Championnat NO GI', 'NO GI'",
                "'Championnat Kids', 'Kids'",
                "'Championnat Kids NO GI', 'Kids NO GI'"
        })
        void should_resolve_url_key_from_name(final String name, final String expectedKey) {
            // given
            final var event = buildEvent("cfjjb-1", name, "https://cfjjb.com/c/1");

            // when
            final var result = grouper.group(List.of(event));

            // then
            assertThat(result.getFirst().urls()).containsKey(expectedKey);
        }
    }

    @Nested
    class NameCleaning {

        @Test
        void should_remove_kids_and_no_gi_from_name() {
            // given
            final var event = buildEvent("cfjjb-1", "Championnat Kids NO GI Paris",
                    "https://cfjjb.com/c/1");

            // when
            final var result = grouper.group(List.of(event));

            // then
            assertThat(result.getFirst().name()).isEqualTo("Championnat Paris");
        }

        @Test
        void should_keep_clean_name() {
            // given
            final var event = buildEvent("cfjjb-1", "Championnat Paris",
                    "https://cfjjb.com/c/1");

            // when
            final var result = grouper.group(List.of(event));

            // then
            assertThat(result.getFirst().name()).isEqualTo("Championnat Paris");
        }
    }

    @Nested
    class Grouping {

        @Test
        void should_group_events_by_id() {
            // given
            final var gi = buildEvent("cfjjb-1", "Championnat GI", "https://cfjjb.com/gi");
            final var noGi = buildEvent("cfjjb-1", "Championnat NO GI", "https://cfjjb.com/nogi");

            // when
            final var result = grouper.group(List.of(gi, noGi));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().urls()).containsKeys("GI", "NO GI");
        }

        @Test
        void should_not_group_different_ids() {
            // given
            final var e1 = buildEvent("cfjjb-1", "Event A GI", "https://cfjjb.com/1");
            final var e2 = buildEvent("cfjjb-2", "Event B GI", "https://cfjjb.com/2");

            // when
            final var result = grouper.group(List.of(e1, e2));

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        void should_skip_events_with_empty_urls() {
            // given
            final var withUrl = buildEvent("cfjjb-1", "Championnat GI", "https://cfjjb.com/gi");
            final var withoutUrl = buildEvent("cfjjb-1", "Championnat NO GI", null);

            // when
            final var result = grouper.group(List.of(withUrl, withoutUrl));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().urls()).hasSize(1);
            assertThat(result.getFirst().urls()).containsKey("GI");
        }
    }

    @Test
    void should_handle_empty_list() {
        assertThat(grouper.group(List.of())).isEmpty();
    }
}

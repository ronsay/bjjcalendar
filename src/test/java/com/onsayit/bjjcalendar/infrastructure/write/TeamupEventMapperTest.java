package com.onsayit.bjjcalendar.infrastructure.write;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.model.Federation;
import com.onsayit.bjjcalendar.infrastructure.teamup.model.EventRead;
import com.onsayit.bjjcalendar.infrastructure.teamup.model.SubCalendarRead;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TeamupEventMapperTest {

    private final TeamupEventMapper mapper = new TeamupEventMapper();

    private Event buildEvent() {
        return new Event("ibjjf-1", Federation.IBJJF, "European Open", "Arena", "Paris", "France",
                LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 12),
                Map.of("Gi", "https://example.com/gi"));
    }

    private List<SubCalendarRead> buildSubCalendars() {
        final var ibjjfSub = new SubCalendarRead();
        ibjjfSub.setId(10);
        ibjjfSub.setName("IBJJF");

        final var diversSub = new SubCalendarRead();
        diversSub.setId(99);
        diversSub.setName("Divers");

        return List.of(ibjjfSub, diversSub);
    }

    @Nested
    class ToEventCreate {

        @Test
        void should_create_event_with_correct_fields() {
            // given
            final var event = buildEvent();
            final var subCalendars = buildSubCalendars();

            // when
            final var result = mapper.toEventCreate(event, subCalendars);

            // then
            assertThat(result.getTitle()).isEqualTo("European Open");
            assertThat(result.getRemoteId()).isEqualTo("ibjjf-1");
            assertThat(result.getLocation()).isEqualTo("Paris");
            assertThat(result.getStartDt()).isEqualTo("2025-06-10");
            assertThat(result.getEndDt()).isEqualTo("2025-06-12");
            assertThat(result.getAllDay()).isTrue();
        }

        @Test
        void should_map_to_matching_subcalendar() {
            // given
            final var event = buildEvent();
            final var subCalendars = buildSubCalendars();

            // when
            final var result = mapper.toEventCreate(event, subCalendars);

            // then
            assertThat(result.getSubcalendarIds()).containsExactly(10);
        }

        @Test
        void should_fallback_to_divers_subcalendar() {
            // given
            final var event = new Event("other-1", Federation.OTHER, "Open", "V", "Paris", "FR",
                    LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 11), Map.of());
            final var subCalendars = buildSubCalendars();

            // when
            final var result = mapper.toEventCreate(event, subCalendars);

            // then
            assertThat(result.getSubcalendarIds()).containsExactly(99);
        }
    }

    @Nested
    class ToEventUpdate {

        @Test
        void should_update_event_with_existing_id() {
            // given
            final var event = buildEvent();
            final var existingEvent = new EventRead("abc-123", null, null, null);
            final var subCalendars = buildSubCalendars();

            // when
            final var result = mapper.toEventUpdate(event, existingEvent, subCalendars);

            // then
            assertThat(result.getId()).isEqualTo("abc-123");
            assertThat(result.getTitle()).isEqualTo("European Open");
            assertThat(result.getRemoteId()).isEqualTo("ibjjf-1");
        }
    }

    @Nested
    class BuildNotes {

        private Event eventWithUrls(final Map<String, String> urls) {
            return new Event("1", Federation.IBJJF, "Event", "V", "C", "FR",
                    LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 11), urls);
        }

        @Test
        void should_return_empty_for_empty_urls() {
            // given
            final var event = eventWithUrls(Collections.emptyMap());

            // when
            final var result = mapper.toEventCreate(event, buildSubCalendars());

            // then
            assertThat(result.getNotes()).isEmpty();
        }

        @Test
        void should_build_single_link() {
            // given
            final var event = eventWithUrls(Map.of("", "https://example.com"));

            // when
            final var result = mapper.toEventCreate(event, buildSubCalendars());

            // then
            assertThat(result.getNotes())
                    .contains("<a href=\"https://example.com\"")
                    .contains("target=\"_blank\"")
                    .contains("rel=\"noreferrer noopener external\"");
        }

        @Test
        void should_build_multiple_links_with_keys() {
            // given
            final var urls = new LinkedHashMap<String, String>();
            urls.put("Gi", "https://example.com/gi");
            urls.put("No-Gi", "https://example.com/nogi");
            final var event = eventWithUrls(urls);

            // when
            final var result = mapper.toEventCreate(event, buildSubCalendars());

            // then
            assertThat(result.getNotes())
                    .contains("Gi : <a href=\"https://example.com/gi\"")
                    .contains("No-Gi : <a href=\"https://example.com/nogi\"")
                    .contains("<br>");
        }

        @Test
        void should_return_empty_for_null_urls() {
            // given
            final var event = eventWithUrls(null);

            // when
            final var result = mapper.toEventCreate(event, buildSubCalendars());

            // then
            assertThat(result.getNotes()).isEmpty();
        }
    }
}

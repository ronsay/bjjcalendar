package com.onsayit.bjjcalendar.infrastructure.read.ajp;

import com.onsayit.bjjcalendar.domain.model.Federation;
import com.onsayit.bjjcalendar.infrastructure.read.utils.LocationResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AjpEventMapperTest {

    @Mock
    private LocationResolver locationResolver;

    @InjectMocks
    private AjpEventMapper mapper;

    private AjpEvent buildAjpEvent(final String month, final String url) {
        return new AjpEvent("AJP Tour", "Arena", "Paris", "France", "FR",
                month, 2025, "10", "12", url);
    }

    private void stubParis() {
        when(locationResolver.resolveCity(anyString())).thenReturn("Paris");
    }

    @Test
    void should_map_valid_event() {
        // given
        stubParis();
        final var ajpEvent = buildAjpEvent("June", "https://ajp.com/events/slug-123");

        // when
        final var result = mapper.toEvent(ajpEvent);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("ajp-slug-123");
        assertThat(result.federation()).isEqualTo(Federation.AJP);
        assertThat(result.name()).isEqualTo("AJP Tour");
        assertThat(result.startDate()).isEqualTo(LocalDate.of(2025, 6, 10));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2025, 6, 12));
        assertThat(result.urls()).containsEntry("", "https://ajp.com/events/slug-123");
    }

    @Test
    void should_return_null_when_month_unknown() {
        // given
        final var ajpEvent = buildAjpEvent("INVALID", "https://ajp.com/e/1");

        // when
        final var result = mapper.toEvent(ajpEvent);

        // then
        assertThat(result).isNull();
    }

    @Test
    void should_return_null_when_start_day_null() {
        // given
        final var ajpEvent = new AjpEvent("AJP Tour", "Arena", "Paris", "France", "FR",
                "June", 2025, null, "12", "https://ajp.com/e/1");

        // when
        final var result = mapper.toEvent(ajpEvent);

        // then
        assertThat(result).isNull();
    }

    @Test
    void should_return_null_when_url_null() {
        // given
        final var ajpEvent = buildAjpEvent("June", null);

        // when
        final var result = mapper.toEvent(ajpEvent);

        // then
        assertThat(result).isNull();
    }

    @Test
    void should_use_start_date_as_end_date_when_end_day_null() {
        // given
        stubParis();
        final var ajpEvent = new AjpEvent("AJP Tour", "Arena", "Paris", "France", "FR",
                "June", 2025, "10", null, "https://ajp.com/e/1");

        // when
        final var result = mapper.toEvent(ajpEvent);

        // then
        assertThat(result.startDate()).isEqualTo(result.endDate());
    }

    @Test
    void should_resolve_city_via_location_resolver() {
        // given
        when(locationResolver.resolveCity("NYC")).thenReturn("New York");
        final var ajpEvent = new AjpEvent("AJP Tour", "Arena", "NYC", "USA", "US",
                "June", 2025, "10", "12", "https://ajp.com/e/1");

        // when
        final var result = mapper.toEvent(ajpEvent);

        // then
        assertThat(result.city()).isEqualTo("New York");
    }

    @Test
    void should_resolve_end_date_crossing_month() {
        // given
        stubParis();
        final var ajpEvent = new AjpEvent("AJP Tour", "Arena", "Paris", "France", "FR",
                "June", 2025, "28", "2", "https://ajp.com/e/1");

        // when
        final var result = mapper.toEvent(ajpEvent);

        // then
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2025, 7, 2));
    }

    @Test
    void should_extract_id_from_url_last_segment() {
        // given
        stubParis();
        final var ajpEvent = buildAjpEvent("June", "https://ajp.com/tournaments/my-event");

        // when
        final var result = mapper.toEvent(ajpEvent);

        // then
        assertThat(result.id()).isEqualTo("ajp-my-event");
    }
}

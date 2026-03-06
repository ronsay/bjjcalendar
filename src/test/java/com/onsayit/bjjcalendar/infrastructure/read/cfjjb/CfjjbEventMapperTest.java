package com.onsayit.bjjcalendar.infrastructure.read.cfjjb;

import com.onsayit.bjjcalendar.domain.model.Federation;
import com.onsayit.bjjcalendar.infrastructure.config.properties.CfjjbProperties;
import com.onsayit.bjjcalendar.infrastructure.read.utils.LocationResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CfjjbEventMapperTest {

    @Mock
    private LocationResolver locationResolver;

    @Mock
    private CfjjbProperties props;

    @InjectMocks
    private CfjjbEventMapper mapper;

    private CfjjbEvent buildCfjjbEvent(final String startDay, final String endDay, final String url) {
        return new CfjjbEvent("Championnat de France", "Paris",
                YearMonth.of(2025, 6), startDay, endDay, url);
    }

    private void stubBaseUrl() {
        when(props.baseUrl()).thenReturn("https://cfjjb.com");
    }

    private void stubParisResolver() {
        when(locationResolver.resolveCity(anyString())).thenReturn("Paris");
    }

    @Test
    void should_map_valid_event() {
        // given
        stubParisResolver();
        stubBaseUrl();
        final var cfjjbEvent = buildCfjjbEvent("10", "12", "/competitions/123");

        // when
        final var result = mapper.toEvent(cfjjbEvent);

        // then
        assertThat(result).isNotNull();
        assertThat(result.federation()).isEqualTo(Federation.CFJJB);
        assertThat(result.name()).isEqualTo("Championnat de France");
        assertThat(result.country()).isEqualTo("France");
        assertThat(result.startDate()).isEqualTo(LocalDate.of(2025, 6, 10));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2025, 6, 12));
        assertThat(result.urls()).containsEntry("", "https://cfjjb.com/competitions/123");
    }

    @Test
    void should_return_null_when_start_day_null() {
        // given
        final var cfjjbEvent = buildCfjjbEvent(null, "12", "/c/1");

        // when
        final var result = mapper.toEvent(cfjjbEvent);

        // then
        assertThat(result).isNull();
    }

    @Test
    void should_return_null_when_year_month_null() {
        // given
        final var cfjjbEvent = new CfjjbEvent("Event", "Paris", null, "10", "12", "/c/1");

        // when
        final var result = mapper.toEvent(cfjjbEvent);

        // then
        assertThat(result).isNull();
    }

    @Test
    void should_return_empty_urls_when_url_null() {
        // given
        stubParisResolver();
        final var cfjjbEvent = buildCfjjbEvent("10", "12", null);

        // when
        final var result = mapper.toEvent(cfjjbEvent);

        // then
        assertThat(result.urls()).isEqualTo(Collections.emptyMap());
    }

    @Test
    void should_generate_slugified_id() {
        // given
        stubParisResolver();
        stubBaseUrl();
        final var cfjjbEvent = buildCfjjbEvent("10", "12", "/c/1");

        // when
        final var result = mapper.toEvent(cfjjbEvent);

        // then
        assertThat(result.id()).isEqualTo("cfjjb-paris-2025-06");
    }

    @Test
    void should_hardcode_country_france() {
        // given
        when(locationResolver.resolveCity(anyString())).thenReturn("Lyon");
        stubBaseUrl();
        final var cfjjbEvent = new CfjjbEvent("Event", "Lyon",
                YearMonth.of(2025, 6), "10", "12", "/c/1");

        // when
        final var result = mapper.toEvent(cfjjbEvent);

        // then
        assertThat(result.country()).isEqualTo("France");
    }

    @Test
    void should_use_start_date_as_end_date_when_end_day_null() {
        // given
        stubParisResolver();
        stubBaseUrl();
        final var cfjjbEvent = buildCfjjbEvent("10", null, "/c/1");

        // when
        final var result = mapper.toEvent(cfjjbEvent);

        // then
        assertThat(result.startDate()).isEqualTo(result.endDate());
    }

    @Test
    void should_set_venue_to_null() {
        // given
        stubParisResolver();
        stubBaseUrl();
        final var cfjjbEvent = buildCfjjbEvent("10", "12", "/c/1");

        // when
        final var result = mapper.toEvent(cfjjbEvent);

        // then
        assertThat(result.venue()).isNull();
    }
}

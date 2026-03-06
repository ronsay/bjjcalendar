package com.onsayit.bjjcalendar.infrastructure.read.smoothcomp;

import com.onsayit.bjjcalendar.infrastructure.config.properties.SmoothcompProperties;
import com.onsayit.bjjcalendar.infrastructure.read.geoapify.GeoapifyBatchGeocodingClient;
import com.onsayit.bjjcalendar.infrastructure.read.geoapify.GeocodingResult;
import com.onsayit.bjjcalendar.infrastructure.read.utils.LocationResolver;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmoothcompDistanceFilterTest {

    @Mock
    private GeoapifyBatchGeocodingClient geocodingClient;

    @Mock
    private SmoothcompProperties props;

    @Mock
    private LocationResolver locationResolver;

    @InjectMocks
    private SmoothcompDistanceFilter filter;

    private SmoothcompEvent buildEvent(final String city, final String country) {
        return new SmoothcompEvent(1L, "Event", "https://other.com/e/1",
                city, country, "FR", "2025-06-10T00:00:00", "2025-06-11T00:00:00");
    }

    private void stubEnabled() {
        when(props.distanceFilterEnabled()).thenReturn(true);
    }

    private void stubOriginCoordinates() {
        when(props.originLatitude()).thenReturn(48.8566);
        when(props.originLongitude()).thenReturn(2.3522);
    }

    @Nested
    class WhenDistanceFilterDisabled {

        @Test
        void should_return_empty_when_disabled() {
            // given
            when(props.distanceFilterEnabled()).thenReturn(false);
            final var events = List.of(buildEvent("Paris", "France"));

            // when
            final var result = filter.filterByDistance(events);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class WhenDistanceFilterEnabled {

        @Test
        void should_keep_events_within_distance() {
            // given
            stubEnabled();
            stubOriginCoordinates();
            when(props.maxDistance()).thenReturn(500);
            when(locationResolver.resolveCity(anyString())).thenReturn("London");
            final var event = buildEvent("London", "UK");
            when(geocodingClient.batchGeocode(anyList()))
                    .thenReturn(Map.of("London, UK", new GeocodingResult(51.5074, -0.1278)));

            // when
            final var result = filter.filterByDistance(List.of(event));

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        void should_exclude_events_beyond_distance() {
            // given
            stubEnabled();
            stubOriginCoordinates();
            when(props.maxDistance()).thenReturn(100);
            when(locationResolver.resolveCity(anyString())).thenReturn("London");
            final var event = buildEvent("London", "UK");
            when(geocodingClient.batchGeocode(anyList()))
                    .thenReturn(Map.of("London, UK", new GeocodingResult(51.5074, -0.1278)));

            // when
            final var result = filter.filterByDistance(List.of(event));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_when_geocoding_returns_empty_fail_closed() {
            // given
            stubEnabled();
            when(locationResolver.resolveCity(anyString())).thenReturn("Unknown");
            final var event = buildEvent("Unknown", "Nowhere");
            when(geocodingClient.batchGeocode(anyList())).thenReturn(Map.of());

            // when
            final var result = filter.filterByDistance(List.of(event));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void should_exclude_event_when_no_geocoding_result() {
            // given
            stubEnabled();
            stubOriginCoordinates();
            when(props.maxDistance()).thenReturn(500);
            when(locationResolver.resolveCity("Paris")).thenReturn("Paris");
            when(locationResolver.resolveCity("Unknown")).thenReturn("Unknown");
            final var knownEvent = buildEvent("Paris", "France");
            final var unknownEvent = new SmoothcompEvent(2L, "Event2", "https://other.com/e/2",
                    "Unknown", "Nowhere", "XX", "2025-06-10T00:00:00", "2025-06-11T00:00:00");
            when(geocodingClient.batchGeocode(anyList()))
                    .thenReturn(Map.of("Paris, France", new GeocodingResult(48.8566, 2.3522)));

            // when
            final var result = filter.filterByDistance(List.of(knownEvent, unknownEvent));

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Test
    void should_return_empty_for_empty_list() {
        // when
        final var result = filter.filterByDistance(List.of());

        // then
        assertThat(result).isEmpty();
        verify(geocodingClient, never()).batchGeocode(anyList());
    }

    @Test
    void should_deduplicate_addresses_before_geocoding() {
        // given
        stubEnabled();
        lenient().when(props.originLatitude()).thenReturn(48.8566);
        lenient().when(props.originLongitude()).thenReturn(2.3522);
        lenient().when(props.maxDistance()).thenReturn(500);
        when(locationResolver.resolveCity(anyString())).thenReturn("Paris");
        final var e1 = buildEvent("Paris", "France");
        final var e2 = new SmoothcompEvent(2L, "Event2", "https://other.com/e/2",
                "Paris", "France", "FR", "2025-06-10T00:00:00", "2025-06-11T00:00:00");
        when(geocodingClient.batchGeocode(List.of("Paris, France")))
                .thenReturn(Map.of("Paris, France", new GeocodingResult(48.8566, 2.3522)));

        // when
        filter.filterByDistance(List.of(e1, e2));

        // then
        verify(geocodingClient).batchGeocode(List.of("Paris, France"));
    }
}

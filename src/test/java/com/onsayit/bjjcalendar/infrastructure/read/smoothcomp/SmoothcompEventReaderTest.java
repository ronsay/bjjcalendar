package com.onsayit.bjjcalendar.infrastructure.read.smoothcomp;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.model.Federation;
import com.onsayit.bjjcalendar.infrastructure.config.properties.SmoothcompProperties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmoothcompEventReaderTest {

    @Mock
    private SmoothcompJsonClient client;

    @Mock
    private SmoothcompMapper mapper;

    @Mock
    private SmoothcompProperties props;

    @Mock
    private SmoothcompDistanceFilter distanceFilter;

    @InjectMocks
    private SmoothcompEventReader reader;

    private void stubProps() {
        lenient().when(props.url()).thenReturn("https://smoothcomp.com/api/events");
        lenient().when(props.countries()).thenReturn(List.of("FR"));
        lenient().when(props.grapplingIndustriesUrl()).thenReturn("grapplingindustries.com");
        lenient().when(props.nagaUrl()).thenReturn("nagafighter.com");
        lenient().when(props.otherUrls()).thenReturn(List.of());
    }

    private SmoothcompEvent buildSmoothcompEvent(final long id, final String url) {
        final var futureDate = LocalDate.now().plusMonths(3).toString() + "T00:00:00";
        final var futureEndDate = LocalDate.now().plusMonths(3).plusDays(1).toString() + "T00:00:00";
        return new SmoothcompEvent(id, "Event " + id, url, "City", "Country", "FR",
                futureDate, futureEndDate);
    }

    @Nested
    class PartitioningByFederation {

        @Test
        void should_include_known_federation_events() {
            // given
            stubProps();
            final var giEvent = buildSmoothcompEvent(1L, "https://grapplingindustries.com/e/1");
            when(client.fetchEventList(anyString())).thenReturn(List.of(giEvent));
            when(distanceFilter.filterByDistance(anyList())).thenReturn(List.of());
            final var mappedEvent = new Event("gi-1", Federation.GRAPPLING_INDUSTRIES, "GI Event",
                    null, "Paris", "France",
                    LocalDate.now().plusMonths(3), LocalDate.now().plusMonths(3).plusDays(1), Map.of());
            when(mapper.toEvent(any())).thenReturn(mappedEvent);

            // when
            final var result = reader.fetchEvents();

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        void should_filter_other_events_by_distance() {
            // given
            stubProps();
            final var otherEvent = buildSmoothcompEvent(2L, "https://other.com/e/2");
            when(client.fetchEventList(anyString())).thenReturn(List.of(otherEvent));
            when(distanceFilter.filterByDistance(anyList())).thenReturn(List.of(otherEvent));
            final var mappedEvent = new Event("other-2", Federation.OTHER, "Other Event",
                    null, "Paris", "France",
                    LocalDate.now().plusMonths(3), LocalDate.now().plusMonths(3).plusDays(1), Map.of());
            when(mapper.toEvent(any())).thenReturn(mappedEvent);

            // when
            final var result = reader.fetchEvents();

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    class UrlBuilding {

        @Test
        void should_append_countries_to_url() {
            // given
            stubProps();
            when(client.fetchEventList("https://smoothcomp.com/api/events?countries=FR"))
                    .thenReturn(List.of());
            when(distanceFilter.filterByDistance(anyList())).thenReturn(List.of());

            // when
            final var result = reader.fetchEvents();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void should_use_base_url_when_no_countries() {
            // given
            when(props.url()).thenReturn("https://smoothcomp.com/api/events");
            when(props.countries()).thenReturn(List.of());
            lenient().when(props.grapplingIndustriesUrl()).thenReturn("grapplingindustries.com");
            lenient().when(props.nagaUrl()).thenReturn("nagafighter.com");
            lenient().when(props.otherUrls()).thenReturn(List.of());
            when(client.fetchEventList("https://smoothcomp.com/api/events")).thenReturn(List.of());
            when(distanceFilter.filterByDistance(anyList())).thenReturn(List.of());

            // when
            final var result = reader.fetchEvents();

            // then
            assertThat(result).isEmpty();
        }
    }
}

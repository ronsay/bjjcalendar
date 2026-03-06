package com.onsayit.bjjcalendar.infrastructure.read.ibjjf;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.model.Federation;
import com.onsayit.bjjcalendar.infrastructure.config.properties.IbjjfProperties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IbjjfEventReaderTest {

    @Mock
    private IbjjfProperties props;

    @Mock
    private IbjjfJsonClient client;

    @Mock
    private IbjjfEventMapper mapper;

    @Mock
    private IbjjfEventGrouper grouper;

    @InjectMocks
    private IbjjfEventReader reader;

    private IbjjfEvent buildIbjjfEvent(final String name, final String region, final String championshipType) {
        final int futureYear = LocalDate.now().getYear() + 1;
        return new IbjjfEvent(1L, name, championshipType, region,
                10, 12, "June", futureYear, "Arena", "Paris", "Active", "/e/1");
    }

    private Event buildFutureEvent(final String name) {
        return new Event("ibjjf-1", Federation.IBJJF, name,
                "Arena", "Paris", "Europe",
                LocalDate.now().plusMonths(3), LocalDate.now().plusMonths(3).plusDays(2), Map.of());
    }

    @Nested
    class WhenDataAvailable {

        @Test
        void should_filter_by_region() {
            // given
            when(props.regions()).thenReturn(List.of("Europe"));
            lenient().when(props.championshipTypes()).thenReturn(List.of());
            final var event = buildIbjjfEvent("European Open", "Europe", "Open");
            when(client.fetch()).thenReturn(Optional.of(List.of(event)));
            when(mapper.toEvent(any())).thenReturn(buildFutureEvent("European Open"));
            when(grouper.group(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // when
            final var result = reader.fetchEvents();

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        void should_filter_by_championship_type() {
            // given
            lenient().when(props.regions()).thenReturn(List.of());
            when(props.championshipTypes()).thenReturn(List.of("International"));
            final var event = buildIbjjfEvent("World Championship", "Americas", "International");
            when(client.fetch()).thenReturn(Optional.of(List.of(event)));
            when(mapper.toEvent(any())).thenReturn(buildFutureEvent("World Championship"));
            when(grouper.group(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // when
            final var result = reader.fetchEvents();

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        void should_exclude_events_not_matching_any_filter() {
            // given
            when(props.regions()).thenReturn(List.of("Europe"));
            lenient().when(props.championshipTypes()).thenReturn(List.of());
            final var event = buildIbjjfEvent("Asian Open", "Asia", "Open");
            when(client.fetch()).thenReturn(Optional.of(List.of(event)));
            when(grouper.group(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // when
            final var result = reader.fetchEvents();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class WhenNoData {

        @Test
        void should_return_empty_when_client_returns_empty() {
            // given
            when(client.fetch()).thenReturn(Optional.empty());

            // when
            final var result = reader.fetchEvents();

            // then
            assertThat(result).isEmpty();
        }
    }
}

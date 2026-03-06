package com.onsayit.bjjcalendar.infrastructure.read.ajp;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.model.Federation;
import com.onsayit.bjjcalendar.infrastructure.config.properties.AjpProperties;
import org.jsoup.Jsoup;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AjpEventReaderTest {

    @Mock
    private AjpHtmlClient client;

    @Mock
    private AjpCalendarExtractor extractor;

    @Mock
    private AjpProperties props;

    @Mock
    private AjpEventMapper mapper;

    @Mock
    private AjpEventGrouper grouper;

    @InjectMocks
    private AjpEventReader reader;

    private Event buildFutureEvent(final String name) {
        return new Event("ajp-1", Federation.AJP, name,
                "Arena", "Paris", "FR",
                LocalDate.now().plusMonths(3), LocalDate.now().plusMonths(3).plusDays(2), Map.of());
    }

    @Nested
    class WhenCalendarAvailable {

        @Test
        void should_filter_by_event_name() {
            // given
            final var doc = Jsoup.parse("<div><h1>Calendar</h1></div>");
            when(client.fetchCalendar(anyInt())).thenReturn(Optional.of(doc));
            final var matchingEvent = new AjpEvent("Grand Slam", "Arena", "Paris", "FR", "FR",
                    "Jun", 2025, "10", "12", "https://ajp.com/e/1");
            final var nonMatchingEvent = new AjpEvent("Other Event", "Arena", "London", "UK", "GB",
                    "Jun", 2025, "15", "16", "https://ajp.com/e/2");
            when(extractor.extractFromContainer(any(), anyInt()))
                    .thenReturn(List.of(matchingEvent, nonMatchingEvent))
                    .thenReturn(List.of());
            when(props.events()).thenReturn(List.of("Grand Slam"));
            when(props.countries()).thenReturn(List.of());
            when(mapper.toEvent(any())).thenReturn(buildFutureEvent("Grand Slam"));
            when(grouper.group(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // when
            final var result = reader.fetchEvents();

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        void should_filter_by_country_code() {
            // given
            final var doc = Jsoup.parse("<div><h1>Calendar</h1></div>");
            when(client.fetchCalendar(anyInt())).thenReturn(Optional.of(doc));
            final var event = new AjpEvent("Local Event", "Arena", "Paris", "FR", "FR",
                    "Jun", 2025, "10", "12", "https://ajp.com/e/1");
            when(extractor.extractFromContainer(any(), anyInt()))
                    .thenReturn(List.of(event))
                    .thenReturn(List.of());
            when(props.events()).thenReturn(List.of());
            when(props.countries()).thenReturn(List.of("FR"));
            when(mapper.toEvent(any())).thenReturn(buildFutureEvent("Local Event"));
            when(grouper.group(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // when
            final var result = reader.fetchEvents();

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    class WhenCalendarUnavailable {

        @Test
        void should_return_empty_when_no_calendar() {
            // given
            when(client.fetchCalendar(anyInt())).thenReturn(Optional.empty());
            when(grouper.group(anyList())).thenReturn(List.of());

            // when
            final var result = reader.fetchEvents();

            // then
            assertThat(result).isEmpty();
        }
    }
}

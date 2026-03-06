package com.onsayit.bjjcalendar.infrastructure.read.cfjjb;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.model.Federation;
import com.onsayit.bjjcalendar.infrastructure.config.properties.CfjjbProperties;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CfjjbEventReaderTest {

    @Mock
    private CfjjbProperties props;

    @Mock
    private CfjjbHtmlClient client;

    @Mock
    private CfjjbExtractor extractor;

    @Mock
    private CfjjbEventMapper mapper;

    @Mock
    private CfjjbEventGrouper grouper;

    @InjectMocks
    private CfjjbEventReader reader;

    private void stubExcludeScenario(final String eventName) {
        when(client.fetch()).thenReturn(Jsoup.parse("<div></div>"));
        final var futureYearMonth = YearMonth.now().plusMonths(3);
        final var cfjjbEvent = new CfjjbEvent(eventName, "Paris", futureYearMonth, "10", "11", "/c/1");
        when(extractor.extract(any())).thenReturn(List.of(cfjjbEvent));
        final var mappedEvent = new Event("cfjjb-1", Federation.CFJJB, eventName,
                null, "Paris", "France",
                LocalDate.now().plusMonths(3), LocalDate.now().plusMonths(3).plusDays(1), Map.of());
        when(mapper.toEvent(any())).thenReturn(mappedEvent);
        when(props.excludes()).thenReturn(List.of("interclub"));
        when(grouper.group(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void should_delegate_to_grouper() {
        // given
        when(client.fetch()).thenReturn(Jsoup.parse("<div></div>"));
        when(extractor.extract(any())).thenReturn(List.of());
        when(grouper.group(anyList())).thenReturn(List.of());

        // when
        reader.fetchEvents();

        // then
        verify(grouper).group(anyList());
    }

    @Test
    void should_exclude_events_by_name_case_insensitive() {
        // given
        stubExcludeScenario("Interclub Paris");

        // when
        final var result = reader.fetchEvents();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_keep_events_not_in_excludes() {
        // given
        stubExcludeScenario("Championnat");

        // when
        final var result = reader.fetchEvents();

        // then
        assertThat(result).hasSize(1);
    }
}

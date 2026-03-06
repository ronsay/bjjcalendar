package com.onsayit.bjjcalendar.infrastructure.read.ajp;

import com.onsayit.bjjcalendar.infrastructure.config.properties.AjpProperties;
import com.onsayit.bjjcalendar.infrastructure.read.utils.JsoupFetcher;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AjpHtmlClientTest {

    @Mock
    private AjpProperties props;

    @Mock
    private JsoupFetcher fetcher;

    @InjectMocks
    private AjpHtmlClient client;

    private void stubCalendarPattern() {
        when(props.calendarPattern()).thenReturn("https://ajp.com/calendar/%d");
    }

    @Test
    void should_format_url_with_year() {
        // given
        stubCalendarPattern();
        when(fetcher.fetch("https://ajp.com/calendar/2025")).thenReturn(Optional.of(Jsoup.parse("")));

        // when
        client.fetchCalendar(2025);

        // then
        verify(fetcher).fetch("https://ajp.com/calendar/2025");
    }

    @Test
    void should_return_empty_when_page_not_found() {
        // given
        stubCalendarPattern();
        when(fetcher.fetch("https://ajp.com/calendar/2025")).thenReturn(Optional.empty());

        // when
        final var result = client.fetchCalendar(2025);

        // then
        assertThat(result).isEmpty();
    }
}

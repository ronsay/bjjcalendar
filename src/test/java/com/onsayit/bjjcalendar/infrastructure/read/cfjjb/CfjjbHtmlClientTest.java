package com.onsayit.bjjcalendar.infrastructure.read.cfjjb;

import com.onsayit.bjjcalendar.infrastructure.config.properties.CfjjbProperties;
import com.onsayit.bjjcalendar.infrastructure.exception.FetchException;
import com.onsayit.bjjcalendar.infrastructure.read.utils.JsoupFetcher;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CfjjbHtmlClientTest {

    @Mock
    private CfjjbProperties props;

    @Mock
    private JsoupFetcher fetcher;

    @InjectMocks
    private CfjjbHtmlClient client;

    @Test
    void should_build_url_and_return_document() {
        // given
        when(props.baseUrl()).thenReturn("https://cfjjb.com");
        when(props.calendarPage()).thenReturn("/calendar");
        when(fetcher.fetch("https://cfjjb.com/calendar")).thenReturn(Optional.of(Jsoup.parse("<html></html>")));

        // when
        final var result = client.fetch();

        // then
        assertThat(result).isNotNull();
    }

    @Test
    void should_throw_when_page_not_found() {
        // given
        when(props.baseUrl()).thenReturn("https://cfjjb.com");
        when(props.calendarPage()).thenReturn("/calendar");
        when(fetcher.fetch("https://cfjjb.com/calendar")).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> client.fetch())
                .isInstanceOf(FetchException.class)
                .hasMessageContaining("CFJJB calendar page not found");
    }
}

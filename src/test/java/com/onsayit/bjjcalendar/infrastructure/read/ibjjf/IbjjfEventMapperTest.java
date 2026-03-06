package com.onsayit.bjjcalendar.infrastructure.read.ibjjf;

import com.onsayit.bjjcalendar.domain.model.Federation;
import com.onsayit.bjjcalendar.infrastructure.config.properties.IbjjfProperties;
import com.onsayit.bjjcalendar.infrastructure.read.utils.LocationResolver;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IbjjfEventMapperTest {

    @Mock
    private IbjjfProperties props;

    @Mock
    private LocationResolver locationResolver;

    @InjectMocks
    private IbjjfEventMapper mapper;

    private IbjjfEvent buildIbjjfEvent(final String name, final String month, final String pageUrl) {
        return new IbjjfEvent(100L, name, "Championship", "Europe",
                10, 12, month, 2025, "Arena", "Paris", "Active", pageUrl);
    }

    private void stubDeps() {
        when(locationResolver.resolveCity(anyString())).thenReturn("Paris");
        when(props.baseUrl()).thenReturn("https://ibjjf.com");
    }

    @Nested
    class BasicMapping {

        @Test
        void should_map_valid_event() {
            // given
            stubDeps();
            final var ibjjfEvent = buildIbjjfEvent("European Championship", "June", "/events/123");

            // when
            final var result = mapper.toEvent(ibjjfEvent);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("ibjjf-100");
            assertThat(result.federation()).isEqualTo(Federation.IBJJF);
            assertThat(result.name()).isEqualTo("European Championship");
            assertThat(result.city()).isEqualTo("Paris");
            assertThat(result.country()).isEqualTo("Europe");
            assertThat(result.startDate()).isEqualTo(LocalDate.of(2025, 6, 10));
            assertThat(result.endDate()).isEqualTo(LocalDate.of(2025, 6, 12));
        }

        @Test
        void should_return_null_when_month_unknown() {
            // given
            final var ibjjfEvent = buildIbjjfEvent("Event", "InvalidMonth", "/events/1");

            // when
            final var result = mapper.toEvent(ibjjfEvent);

            // then
            assertThat(result).isNull();
        }

        @Test
        void should_return_empty_urls_when_page_url_null() {
            // given
            when(locationResolver.resolveCity("Paris")).thenReturn("Paris");
            final var ibjjfEvent = buildIbjjfEvent("Event", "June", null);

            // when
            final var result = mapper.toEvent(ibjjfEvent);

            // then
            assertThat(result).isNotNull();
            assertThat(result.urls()).isEqualTo(Collections.emptyMap());
        }
    }

    @Nested
    class EventType {

        @ParameterizedTest
        @CsvSource({
                "'European No-Gi Championship', 'No-Gi'",
                "'Kids International', 'Kids'",
                "'European Championship', 'Gi'"
        })
        void should_detect_event_type_from_name(final String name, final String expectedKey) {
            // given
            stubDeps();
            final var ibjjfEvent = buildIbjjfEvent(name, "June", "/events/1");

            // when
            final var result = mapper.toEvent(ibjjfEvent);

            // then
            assertThat(result.urls()).containsKey(expectedKey);
            assertThat(result.urls()).containsEntry(expectedKey, "https://ibjjf.com/events/1");
        }
    }

    @Nested
    class EndDateResolution {

        @Test
        void should_resolve_end_date_in_same_month() {
            // given
            stubDeps();
            final var ibjjfEvent = new IbjjfEvent(1L, "Event", "Championship", "Europe",
                    10, 15, "June", 2025, "Arena", "Paris", "Active", "/e/1");

            // when
            final var result = mapper.toEvent(ibjjfEvent);

            // then
            assertThat(result.endDate()).isEqualTo(LocalDate.of(2025, 6, 15));
        }

        @Test
        void should_resolve_end_date_crossing_month() {
            // given
            stubDeps();
            final var ibjjfEvent = new IbjjfEvent(1L, "Event", "Championship", "Europe",
                    28, 2, "June", 2025, "Arena", "Paris", "Active", "/e/1");

            // when
            final var result = mapper.toEvent(ibjjfEvent);

            // then
            assertThat(result.endDate()).isEqualTo(LocalDate.of(2025, 7, 2));
        }
    }
}

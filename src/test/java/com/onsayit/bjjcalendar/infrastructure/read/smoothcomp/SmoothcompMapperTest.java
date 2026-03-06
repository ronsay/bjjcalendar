package com.onsayit.bjjcalendar.infrastructure.read.smoothcomp;

import com.onsayit.bjjcalendar.domain.model.Federation;
import com.onsayit.bjjcalendar.infrastructure.config.properties.SmoothcompProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SmoothcompMapperTest {

    @Mock
    private SmoothcompProperties props;

    @InjectMocks
    private SmoothcompMapper mapper;

    private void stubFederationUrls() {
        lenient().when(props.grapplingIndustriesUrl()).thenReturn("grapplingindustries.com");
        lenient().when(props.nagaUrl()).thenReturn("nagafighter.com");
    }

    @Test
    void should_map_valid_event() {
        // given
        stubFederationUrls();
        final var event = new SmoothcompEvent(42L, "Open Mat", "https://other.com/e/42",
                "Paris", "France", "FR", "2025-06-10T00:00:00", "2025-06-12T00:00:00");

        // when
        final var result = mapper.toEvent(event);

        // then
        assertThat(result.id()).isEqualTo("other-42");
        assertThat(result.federation()).isEqualTo(Federation.OTHER);
        assertThat(result.name()).isEqualTo("Open Mat");
        assertThat(result.city()).isEqualTo("Paris");
        assertThat(result.country()).isEqualTo("France");
        assertThat(result.startDate()).isEqualTo(LocalDate.of(2025, 6, 10));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2025, 6, 12));
        assertThat(result.urls()).containsEntry("", "https://other.com/e/42");
    }

    @ParameterizedTest
    @CsvSource({
            "'https://grapplingindustries.com/events/1', GRAPPLING_INDUSTRIES",
            "'https://nagafighter.com/events/2', NAGA",
            "'https://other.com/events/3', OTHER"
    })
    void should_detect_federation_from_url(final String url, final Federation expected) {
        // given
        stubFederationUrls();
        final var event = new SmoothcompEvent(1L, "Test", url,
                "City", "Country", "CC", "2025-06-10T00:00:00", "2025-06-11T00:00:00");

        // when
        final var result = mapper.toEvent(event);

        // then
        assertThat(result.federation()).isEqualTo(expected);
    }

    @Test
    void should_generate_id_with_federation_prefix() {
        // given
        stubFederationUrls();
        final var event = new SmoothcompEvent(99L, "GI Open", "https://grapplingindustries.com/e/99",
                "London", "UK", "GB", "2025-06-10T00:00:00", "2025-06-11T00:00:00");

        // when
        final var result = mapper.toEvent(event);

        // then
        assertThat(result.id()).isEqualTo("grappling_industries-99");
    }

    @Test
    void should_set_venue_to_null() {
        // given
        stubFederationUrls();
        final var event = new SmoothcompEvent(1L, "Test", "https://other.com/e/1",
                "Paris", "France", "FR", "2025-06-10T00:00:00", "2025-06-11T00:00:00");

        // when
        final var result = mapper.toEvent(event);

        // then
        assertThat(result.venue()).isNull();
    }
}

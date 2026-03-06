package com.onsayit.bjjcalendar.infrastructure.read.utils;

import com.onsayit.bjjcalendar.infrastructure.config.properties.SourcesProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationResolverTest {

    @Mock
    private SourcesProperties sourcesProperties;

    @InjectMocks
    private LocationResolver locationResolver;

    @Test
    void should_return_override_when_present() {
        // given
        when(sourcesProperties.cityOverrides()).thenReturn(Map.of("NYC", "New York"));

        // when
        final var result = locationResolver.resolveCity("NYC");

        // then
        assertThat(result).isEqualTo("New York");
    }

    @Test
    void should_return_original_when_no_override() {
        // given
        when(sourcesProperties.cityOverrides()).thenReturn(Map.of("NYC", "New York"));

        // when
        final var result = locationResolver.resolveCity("Paris");

        // then
        assertThat(result).isEqualTo("Paris");
    }

    @Test
    void should_return_original_when_overrides_null() {
        // given
        when(sourcesProperties.cityOverrides()).thenReturn(null);

        // when
        final var result = locationResolver.resolveCity("Paris");

        // then
        assertThat(result).isEqualTo("Paris");
    }
}

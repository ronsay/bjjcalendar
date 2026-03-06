package com.onsayit.bjjcalendar.infrastructure.read.utils;

import com.onsayit.bjjcalendar.infrastructure.config.properties.SourcesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocationResolver {

    private final SourcesProperties sourcesProperties;

    public String resolveCity(final String city) {
        final var overrides = sourcesProperties.cityOverrides();
        if (overrides != null && overrides.containsKey(city)) {
            return overrides.get(city);
        }
        return city;
    }
}

package com.onsayit.bjjcalendar.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "sources.smoothcomp")
public record SmoothcompProperties(
        boolean enabled,
        String url,
        String grapplingIndustriesUrl,
        String nagaUrl,
        List<String> otherUrls,
        List<String> countries,
        boolean distanceFilterEnabled,
        int maxDistance,
        double originLatitude,
        double originLongitude
) { }

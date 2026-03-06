package com.onsayit.bjjcalendar.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "sources.cfjjb")
public record CfjjbProperties(
        boolean enabled,
        String baseUrl,
        String calendarPage,
        List<String> excludes
) { }

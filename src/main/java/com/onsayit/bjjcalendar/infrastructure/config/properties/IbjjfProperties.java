package com.onsayit.bjjcalendar.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "sources.ibjjf")
public record IbjjfProperties(
        boolean enabled,
        List<String> regions,
        List<String> championshipTypes,
        String baseUrl,
        String calendarPage,
        String calendarJson,
        String etagFile
) { }

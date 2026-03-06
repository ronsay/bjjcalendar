package com.onsayit.bjjcalendar.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "sources.ajp")
public record AjpProperties(
        boolean enabled,
        String calendarPattern,
        List<String> events,
        List<String> countries
) { }


package com.onsayit.bjjcalendar.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "sources")
public record SourcesProperties(
        int timeout,
        Map<String, String> cityOverrides
) { }

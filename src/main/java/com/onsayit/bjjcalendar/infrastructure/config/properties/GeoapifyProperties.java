package com.onsayit.bjjcalendar.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "geoapify")
public record GeoapifyProperties(
        String apiKey,
        int pollingIntervalMs,
        int maxPollingAttempts
) { }

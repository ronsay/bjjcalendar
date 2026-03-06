package com.onsayit.bjjcalendar.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "teamup.api")
public record TeamupEventWriterProperties(
        boolean enabled,
        String url,
        String email,
        String password,
        String token,
        String calendarId,
        String appName
) {}

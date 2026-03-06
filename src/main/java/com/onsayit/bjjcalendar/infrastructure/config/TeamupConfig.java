package com.onsayit.bjjcalendar.infrastructure.config;

import com.onsayit.bjjcalendar.infrastructure.config.properties.TeamupEventWriterProperties;
import com.onsayit.bjjcalendar.infrastructure.teamup.ApiClient;
import com.onsayit.bjjcalendar.infrastructure.teamup.api.AuthenticateApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TeamupConfig {

    private static final String TEAMUP_TOKEN = "Teamup-Token";

    @Bean
    @ConditionalOnProperty(name = "teamup.api.enabled", havingValue = "true")
    public AuthenticateApi authenticateApi(final TeamupEventWriterProperties props) {
        final var apiClient = new ApiClient();
        apiClient.updateBaseUri(props.url());
        apiClient.setRequestInterceptor(builder -> builder.header(TEAMUP_TOKEN, props.token()));
        return new AuthenticateApi(apiClient);
    }
}

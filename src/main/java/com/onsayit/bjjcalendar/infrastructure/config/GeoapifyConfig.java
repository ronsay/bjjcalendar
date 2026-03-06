package com.onsayit.bjjcalendar.infrastructure.config;

import com.onsayit.bjjcalendar.infrastructure.geoapify.ApiClient;
import com.onsayit.bjjcalendar.infrastructure.geoapify.api.BatchGeocodingApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeoapifyConfig {

    @Bean
    public BatchGeocodingApi batchGeocodingApi() {
        return new BatchGeocodingApi(new ApiClient());
    }
}

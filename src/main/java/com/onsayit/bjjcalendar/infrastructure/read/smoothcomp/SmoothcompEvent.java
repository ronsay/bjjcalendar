package com.onsayit.bjjcalendar.infrastructure.read.smoothcomp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SmoothcompEvent(
        long id,
        String title,
        String url,
        @JsonProperty("location_city")
        String city,
        @JsonProperty("location_country_human")
        String country,
        @JsonProperty("location_country")
        String countryCode,
        @JsonProperty("startdate")
        String startDate,
        @JsonProperty("enddate")
        String endDate
) {}

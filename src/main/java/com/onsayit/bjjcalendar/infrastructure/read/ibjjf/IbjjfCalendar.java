package com.onsayit.bjjcalendar.infrastructure.read.ibjjf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IbjjfCalendar(
        @JsonProperty(TAG)
        List<IbjjfEvent> events
) {
    private static final String TAG = "infosite_events";
}

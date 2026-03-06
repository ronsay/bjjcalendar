package com.onsayit.bjjcalendar.infrastructure.read.ibjjf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IbjjfEvent(
    long id,
    String name,
    String championshipType,
    String region,
    int startDay,
    int endDay,
    String month,
    int year,
    String local,
    String city,
    String status,
    String pageUrl
) {}

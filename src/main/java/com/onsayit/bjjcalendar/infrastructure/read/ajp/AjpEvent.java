package com.onsayit.bjjcalendar.infrastructure.read.ajp;

public record AjpEvent(
        String name,
        String venue,
        String city,
        String country,
        String countryCode,
        String month,
        int year,
        String startDay,
        String endDay,
        String url
) { }

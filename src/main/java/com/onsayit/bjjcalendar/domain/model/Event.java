package com.onsayit.bjjcalendar.domain.model;

import java.time.LocalDate;
import java.util.Map;

@SuppressWarnings("PMD.UnusedAssignment") // PMD false positive on record compact constructor
public record Event(
        String id,
        Federation federation,
        String name,
        String venue,
        String city,
        String country,
        LocalDate startDate,
        LocalDate endDate,
        Map<String, String> urls
) {
    public Event {
        urls = urls == null ? Map.of() : Map.copyOf(urls);
    }
}

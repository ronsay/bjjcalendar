package com.onsayit.bjjcalendar;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.model.Federation;

import java.time.LocalDate;
import java.util.Map;

public final class TestEventFactory {

    private static final LocalDate FUTURE = LocalDate.now().plusMonths(3);

    private TestEventFactory() {
    }

    public static Event create(final Federation federation, final String name) {
        return create(federation, federation.name().toLowerCase() + "-1", name);
    }

    public static Event create(final Federation federation, final String id, final String name) {
        return new Event(id, federation, name, "Arena", "Paris", "France",
                FUTURE, FUTURE.plusDays(1), Map.of("Gi", "https://example.com"));
    }

    public static Event create(final Federation federation, final String id, final String name,
                               final LocalDate startDate, final LocalDate endDate,
                               final Map<String, String> urls) {
        return new Event(id, federation, name, "Arena", "Paris", "France",
                startDate, endDate, urls);
    }

    public static Event create(final Federation federation, final String id, final String name,
                               final String city, final LocalDate startDate, final LocalDate endDate,
                               final Map<String, String> urls) {
        return new Event(id, federation, name, "Arena", city, "France",
                startDate, endDate, urls);
    }
}

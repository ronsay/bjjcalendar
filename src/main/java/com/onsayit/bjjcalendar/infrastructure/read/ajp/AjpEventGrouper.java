package com.onsayit.bjjcalendar.infrastructure.read.ajp;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.infrastructure.read.utils.ReaderUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
class AjpEventGrouper {

    public List<Event> group(final List<Event> events) {
        return events.stream()
                .collect(Collectors.groupingBy(Event::id))
                .values().stream()
                .map(this::mergeGroup)
                .toList();
    }

    private String cleanName(final String name) {
        final var lastDash = name.lastIndexOf(" -");
        return lastDash >= 0 ? name.substring(0, lastDash) : name;
    }

    private Event mergeGroup(final List<Event> group) {
        final var primary = group.getFirst();
        return ReaderUtils.buildMergedEvent(
                primary,
                this.cleanName(primary.name()),
                group,
                Map.of("", primary.urls().values().iterator().next())
        );
    }
}

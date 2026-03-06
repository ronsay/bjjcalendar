package com.onsayit.bjjcalendar.infrastructure.read.cfjjb;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.infrastructure.read.utils.ReaderUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
class CfjjbEventGrouper {

    private static final String GI = "GI";
    private static final String NO_GI = "NO GI";
    private static final String KIDS = "Kids";
    private static final String KIDS_NO_GI = "Kids NO GI";

    public List<Event> group(final List<Event> events) {
        return events.stream()
                .collect(Collectors.groupingBy(Event::id))
                .values().stream()
                .map(this::mergeGroup)
                .toList();
    }

    private Event mergeGroup(final List<Event> group) {
        final var primary = group.getFirst();

        final var mergedUrls = group.stream()
                .filter(event -> !event.urls().isEmpty())
                .collect(Collectors.toMap(
                        event -> this.resolveUrlKey(event.name()),
                        event -> event.urls().values().iterator().next(),
                        (existing, duplicate) -> existing
                ));

        return ReaderUtils.buildMergedEvent(
                primary,
                this.cleanName(primary.name()),
                group,
                mergedUrls
        );
    }

    private String resolveUrlKey(final String name) {
        final var upper = name.toUpperCase();

        if (upper.contains(KIDS.toUpperCase()) && upper.contains(NO_GI)) {
            return KIDS_NO_GI;
        }

        if (upper.contains(NO_GI)) {
            return NO_GI;
        }

        if (upper.contains(KIDS.toUpperCase())) {
            return KIDS;
        }
        return GI;
    }

    private String cleanName(final String name) {
        return name
                .replaceAll("(?i)\\s*Kids\\s*", " ")
                .replaceAll("(?i)\\s*NO GI\\s*", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }
}

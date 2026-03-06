package com.onsayit.bjjcalendar.infrastructure.read.ibjjf;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.infrastructure.read.utils.ReaderUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
class IbjjfEventGrouper {

    /**
     * Groups events by normalized name and city, then splits them into subgroups of continuous dates,
     * and finally merges each subgroup into a single event.
     */
    public List<Event> group(final List<Event> events) {
        return events.stream()
                .collect(Collectors.groupingBy(
                        event -> this.normalizeEventName(event.name()) + "|" + event.city(),
                        HashMap::new,
                        Collectors.toList()
                ))
                .values().stream()
                .flatMap(group -> this.splitByDateContinuity(group).stream())
                .map(this::mergeGroup)
                .toList();
    }

    /**
     * Splits a group of events into subgroups where each subgroup contains events that are continuous in time.
     * Two events are considered continuous if the start date of one is at most one day after the end date of the other.
     */
    private List<List<Event>> splitByDateContinuity(final List<Event> group) {
        if (group.size() <= 1) {
            return List.of(group);
        }

        final var sorted = group.stream()
                .sorted(Comparator.comparing(Event::startDate))
                .toList();

        final List<List<Event>> subGroups = new ArrayList<>();
        var currentSubGroup = new ArrayList<>(List.of(sorted.getFirst()));
        var currentEndDate = sorted.getFirst().endDate();

        for (int i = 1; i < sorted.size(); i++) {
            final var event = sorted.get(i);
            if (!event.startDate().isAfter(currentEndDate.plusDays(1))) {
                currentSubGroup.add(event);
                if (event.endDate().isAfter(currentEndDate)) {
                    currentEndDate = event.endDate();
                }
            } else {
                subGroups.add(currentSubGroup);
                currentSubGroup = new ArrayList<>(List.of(event));
                currentEndDate = event.endDate();
            }
        }
        subGroups.add(currentSubGroup);

        return subGroups;
    }

    private String normalizeEventName(final String name) {
        return name
                .replace("No-Gi ", "")
                .replace("Kids ", "")
                .replaceAll("\\s+\\d{4}$", "");
    }

    private Event mergeGroup(final List<Event> group) {
        final var giFirst = group.stream()
                .sorted(Comparator.comparingInt(e -> e.urls().containsKey("Gi") ? 0 : 1))
                .toList();

        final var primary = giFirst.getFirst();

        final Map<String, String> mergedUrls = new HashMap<>();

        for (final var event : group) {
            event.urls().forEach(mergedUrls::putIfAbsent);
        }

        return ReaderUtils.buildMergedEvent(
                primary,
                primary.name(),
                group,
                mergedUrls
        );
    }
}

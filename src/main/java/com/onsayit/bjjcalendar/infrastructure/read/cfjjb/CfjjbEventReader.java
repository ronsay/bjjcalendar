package com.onsayit.bjjcalendar.infrastructure.read.cfjjb;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.ports.EventReader;
import com.onsayit.bjjcalendar.infrastructure.config.properties.CfjjbProperties;
import com.onsayit.bjjcalendar.infrastructure.read.utils.ReaderUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "sources.cfjjb", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class CfjjbEventReader implements EventReader {

    private final CfjjbProperties props;
    private final CfjjbHtmlClient client;
    private final CfjjbExtractor extractor;
    private final CfjjbEventMapper mapper;
    private final CfjjbEventGrouper grouper;

    @Override
    public List<Event> fetchEvents() {
        final var events = ReaderUtils.mapAndFilter(
                extractor.extract(client.fetch()).stream(),
                mapper::toEvent).stream()
                .filter(this::filterOnName)
                .toList();

        return grouper.group(events);
    }

    private boolean filterOnName(final Event event) {
        return props.excludes().stream()
                .noneMatch(exclude -> event.name().toLowerCase().contains(exclude.toLowerCase()));
    }
}

package com.onsayit.bjjcalendar.infrastructure.read.ibjjf;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.ports.EventReader;
import com.onsayit.bjjcalendar.infrastructure.config.properties.IbjjfProperties;
import com.onsayit.bjjcalendar.infrastructure.read.utils.ReaderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "sources.ibjjf", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class IbjjfEventReader implements EventReader {

    private final IbjjfProperties props;
    private final IbjjfJsonClient client;
    private final IbjjfEventMapper mapper;
    private final IbjjfEventGrouper grouper;

    @Override
    public List<Event> fetchEvents() {
        return client.fetch()
                .map(events -> grouper.group(ReaderUtils.mapAndFilter(
                        events.stream().filter(this.filterBeforeMapping()),
                        mapper::toEvent)))
                .orElseGet(() -> {
                    log.info("IBJJF: no new data (cached), skipping.");
                    return List.of();
                });
    }

    private Predicate<IbjjfEvent> filterBeforeMapping() {
        return ibjjfEvent -> this.matchesRegion(ibjjfEvent) || this.matchesChampionshipType(ibjjfEvent);
    }

    private boolean matchesRegion(final IbjjfEvent event) {
        return !props.regions().isEmpty() && props.regions().contains(event.region());
    }

    private boolean matchesChampionshipType(final IbjjfEvent event) {
        return !props.championshipTypes().isEmpty() && props.championshipTypes().contains(event.championshipType());
    }
}

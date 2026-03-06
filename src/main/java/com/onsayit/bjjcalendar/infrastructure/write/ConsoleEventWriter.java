package com.onsayit.bjjcalendar.infrastructure.write;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.ports.EventWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ConsoleEventWriter implements EventWriter {

    @Override
    public void writeAll(final List<Event> events) {
        events.forEach(event -> log.info(
                "{} ({}) {} ({} → {}) - {} - {}",
                event.federation().name(),
                event.id(),
                event.name(),
                event.startDate(),
                event.endDate(),
                event.city(),
                event.urls()
        ));
    }
}

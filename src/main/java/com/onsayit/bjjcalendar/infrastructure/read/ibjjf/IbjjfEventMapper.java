package com.onsayit.bjjcalendar.infrastructure.read.ibjjf;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.infrastructure.config.properties.IbjjfProperties;
import com.onsayit.bjjcalendar.infrastructure.read.utils.LocationResolver;
import com.onsayit.bjjcalendar.infrastructure.read.utils.ReaderUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import static com.onsayit.bjjcalendar.domain.model.Federation.IBJJF;

@Component
@RequiredArgsConstructor
public class IbjjfEventMapper {

    private final IbjjfProperties props;
    private final LocationResolver locationResolver;

    public Event toEvent(final IbjjfEvent ibjjfEvent) {
        final var month = ReaderUtils.getMonth(ibjjfEvent.month());

        if (month == null) {
            return null;
        }

        final var startDate = LocalDate.of(ibjjfEvent.year(), month, ibjjfEvent.startDay());

        return new Event(
                ReaderUtils.generateId(IBJJF, ibjjfEvent.id()),
                IBJJF,
                ibjjfEvent.name(),
                ibjjfEvent.local(),
                locationResolver.resolveCity(ibjjfEvent.city()),
                ibjjfEvent.region(),
                startDate,
                ReaderUtils.resolveEndDate(ibjjfEvent.endDay(), startDate),
                ibjjfEvent.pageUrl() != null
                        ? Map.of(this.getEventType(ibjjfEvent.name()), this.getEventUrl(ibjjfEvent))
                        : Collections.emptyMap()
        );
    }

    private String getEventType(final String name) {
        return switch (name) {
            case String n when n.contains("No-Gi") -> "No-Gi";
            case String n when n.contains("Kids") -> "Kids";
            default -> "Gi";
        };
    }

    private String getEventUrl(final IbjjfEvent event) {
        return props.baseUrl() + event.pageUrl();
    }
}

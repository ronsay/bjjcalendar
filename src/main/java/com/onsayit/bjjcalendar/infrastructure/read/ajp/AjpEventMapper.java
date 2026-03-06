package com.onsayit.bjjcalendar.infrastructure.read.ajp;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.infrastructure.read.utils.LocationResolver;
import com.onsayit.bjjcalendar.infrastructure.read.utils.ReaderUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

import static com.onsayit.bjjcalendar.domain.model.Federation.AJP;

@Component
@RequiredArgsConstructor
public class AjpEventMapper {

    private final LocationResolver locationResolver;

    public Event toEvent(final AjpEvent ajpEvent) {
        final var month = ReaderUtils.getMonth(ajpEvent.month());

        if (month == null || ajpEvent.startDay() == null || ajpEvent.url() == null) {
            return null;
        }

        final var startDay = Integer.parseInt(ajpEvent.startDay());
        final var startDate = LocalDate.of(ajpEvent.year(), month, startDay);

        return new Event(
                ReaderUtils.generateId(AJP, ReaderUtils.extractLastPathSegment(ajpEvent.url())),
                AJP,
                ajpEvent.name(),
                ajpEvent.venue(),
                locationResolver.resolveCity(ajpEvent.city()),
                ajpEvent.country(),
                startDate,
                ajpEvent.endDay() != null
                        ? ReaderUtils.resolveEndDate(Integer.parseInt(ajpEvent.endDay()), startDate)
                        : startDate,
                Map.of("", ajpEvent.url())
        );
    }

}

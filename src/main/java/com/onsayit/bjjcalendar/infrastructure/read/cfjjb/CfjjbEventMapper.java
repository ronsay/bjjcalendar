package com.onsayit.bjjcalendar.infrastructure.read.cfjjb;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.infrastructure.config.properties.CfjjbProperties;
import com.onsayit.bjjcalendar.infrastructure.read.utils.LocationResolver;
import com.onsayit.bjjcalendar.infrastructure.read.utils.ReaderUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import static com.onsayit.bjjcalendar.domain.model.Federation.CFJJB;

@Component
@RequiredArgsConstructor
public class CfjjbEventMapper {

    private static final String COUNTRY = "France";

    private final LocationResolver locationResolver;
    private final CfjjbProperties props;

    public Event toEvent(final CfjjbEvent cfjjbEvent) {
        if (cfjjbEvent.startDay() == null || cfjjbEvent.yearMonth() == null) {
            return null;
        }

        final var startDay = Integer.parseInt(cfjjbEvent.startDay());
        final var startDate = LocalDate.of(
                cfjjbEvent.yearMonth().getYear(),
                cfjjbEvent.yearMonth().getMonth(),
                startDay
        );

        final var endDate = cfjjbEvent.endDay() != null
                ? ReaderUtils.resolveEndDate(Integer.parseInt(cfjjbEvent.endDay()), startDate)
                : startDate;

        return new Event(
                ReaderUtils.generateId(CFJJB, this.buildDiscriminant(cfjjbEvent)),
                CFJJB,
                cfjjbEvent.name(),
                null,
                locationResolver.resolveCity(cfjjbEvent.city()),
                COUNTRY,
                startDate,
                endDate,
                cfjjbEvent.url() != null ? Map.of("", props.baseUrl() + cfjjbEvent.url()) : Collections.emptyMap()
        );
    }

    private String buildDiscriminant(final CfjjbEvent event) {
        return ReaderUtils.slugify(event.city()) + "-" + event.yearMonth();
    }
}

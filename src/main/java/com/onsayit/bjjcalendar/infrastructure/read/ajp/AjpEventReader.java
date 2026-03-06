package com.onsayit.bjjcalendar.infrastructure.read.ajp;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.ports.EventReader;
import com.onsayit.bjjcalendar.infrastructure.config.properties.AjpProperties;
import com.onsayit.bjjcalendar.infrastructure.read.utils.ReaderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "sources.ajp", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class AjpEventReader implements EventReader {

    private final AjpHtmlClient client;
    private final AjpCalendarExtractor extractor;
    private final AjpProperties props;
    private final AjpEventMapper mapper;
    private final AjpEventGrouper grouper;

    @Override
    public List<Event> fetchEvents() {
        final int currentYear = LocalDate.now().getYear();
        final List<Event> events = new ArrayList<>();

        for (final int year : List.of(currentYear, currentYear + 1)) {
            client.fetchCalendar(year).ifPresent(doc -> {
                final var h1 = doc.selectFirst("div h1");

                if (h1 == null || h1.parent() == null) {
                    log.warn("AJP: no calendar container found for year {}", year);
                    return;
                }

                events.addAll(ReaderUtils.mapAndFilter(
                        extractor.extractFromContainer(h1.parent(), year)
                                .stream()
                                .filter(this.filterBeforeMapping()),
                        mapper::toEvent));
            });
        }

        return grouper.group(events);
    }

    private Predicate<AjpEvent> filterBeforeMapping() {
        return event -> this.isInEvent(event.name()) || this.isInCountries(event.countryCode());
    }

    private boolean isInEvent(final String name) {
        return props.events().stream()
                .anyMatch(eventName -> name != null
                        && name.toLowerCase(Locale.ROOT).contains(eventName.toLowerCase(Locale.ROOT)));
    }

    private boolean isInCountries(final String countryCode) {
        return props.countries().stream()
                .anyMatch(code -> code.equalsIgnoreCase(countryCode));
    }
}


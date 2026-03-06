package com.onsayit.bjjcalendar.application;

import com.onsayit.bjjcalendar.domain.ports.EventReader;
import com.onsayit.bjjcalendar.domain.ports.EventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportEventsUseCase {
    private final List<EventReader> readers;
    private final EventWriter writer;

    /**
     * Fetches events from all configured readers, and writes them to the destination.
     */
    public void run() {
        writer.writeAll(readers.stream()
                .flatMap(r -> {
                    try {
                        return r.fetchEvents().stream();
                    } catch (final Exception e) {
                        log.error("Failed to fetch events from {}: {}",
                                r.getClass().getSimpleName(), e.getMessage(), e);
                        return Stream.empty();
                    }
                })
                .filter(e -> !e.endDate().isBefore(LocalDate.now()))
                .toList());
    }
}


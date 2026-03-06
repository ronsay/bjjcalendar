package com.onsayit.bjjcalendar.infrastructure.read.utils;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.model.Federation;
import lombok.extern.slf4j.Slf4j;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
public final class ReaderUtils {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Firefox/128.0";

    private static final Map<String, Month> MONTH_MAP = Map.ofEntries(
            // English abbreviations
            Map.entry("jan", Month.JANUARY),
            Map.entry("feb", Month.FEBRUARY),
            Map.entry("mar", Month.MARCH),
            Map.entry("apr", Month.APRIL),
            Map.entry("may", Month.MAY),
            Map.entry("jun", Month.JUNE),
            Map.entry("jul", Month.JULY),
            Map.entry("aug", Month.AUGUST),
            Map.entry("sep", Month.SEPTEMBER),
            Map.entry("sept", Month.SEPTEMBER),
            Map.entry("oct", Month.OCTOBER),
            Map.entry("nov", Month.NOVEMBER),
            Map.entry("dec", Month.DECEMBER),

            // English full names
            Map.entry("january", Month.JANUARY),
            Map.entry("february", Month.FEBRUARY),
            Map.entry("march", Month.MARCH),
            Map.entry("april", Month.APRIL),
            Map.entry("june", Month.JUNE),
            Map.entry("july", Month.JULY),
            Map.entry("august", Month.AUGUST),
            Map.entry("september", Month.SEPTEMBER),
            Map.entry("october", Month.OCTOBER),
            Map.entry("november", Month.NOVEMBER),
            Map.entry("december", Month.DECEMBER),

            // French names
            Map.entry("janvier", Month.JANUARY),
            Map.entry("fevrier", Month.FEBRUARY),
            Map.entry("mars", Month.MARCH),
            Map.entry("avril", Month.APRIL),
            Map.entry("mai", Month.MAY),
            Map.entry("juin", Month.JUNE),
            Map.entry("juillet", Month.JULY),
            Map.entry("aout", Month.AUGUST),
            Map.entry("septembre", Month.SEPTEMBER),
            Map.entry("octobre", Month.OCTOBER),
            Map.entry("novembre", Month.NOVEMBER),
            Map.entry("decembre", Month.DECEMBER)
    );

    private ReaderUtils() {
        // Utility class
    }

    public static Month getMonth(final String monthName) {
        final var normalized = Normalizer.normalize(monthName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        final var month = MONTH_MAP.get(normalized.toLowerCase(Locale.ROOT));

        if (month == null) {
            log.warn("Unknown month: '{}' — skipping", monthName);
        }

        return month;
    }

    public static String generateId(final Federation federation, final long eventId) {
        return generateId(federation, String.valueOf(eventId));
    }

    public static String generateId(final Federation federation, final String discriminant) {
        return federation.name().toLowerCase(Locale.ROOT) + "-" + discriminant;
    }

    public static LocalDate parseDate(final String date) {
        return LocalDate.parse(date.substring(0, 10));
    }

    public static LocalDate resolveEndDate(final int endDay, final LocalDate startDate) {
        if (endDay < startDate.getDayOfMonth()) {
            final int endYear = startDate.getMonth() == Month.DECEMBER ? startDate.getYear() + 1 : startDate.getYear();
            return LocalDate.of(endYear, startDate.getMonth().plus(1), endDay);
        }
        return LocalDate.of(startDate.getYear(), startDate.getMonth(), endDay);
    }

    public static String normalizeWhitespace(final String s) {
        return s == null ? "" : s.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static <T> List<Event> mapAndFilter(final Stream<T> rawEvents, final Function<T, Event> mapper) {
        return rawEvents
                .map(mapper)
                .filter(Objects::nonNull)
                .filter(event -> !event.endDate().isBefore(LocalDate.now()))
                .toList();
    }

    public static Event buildMergedEvent(final Event primary, final String name,
                                         final List<Event> group, final Map<String, String> urls) {
        return new Event(
                primary.id(),
                primary.federation(),
                name,
                primary.venue(),
                primary.city(),
                primary.country(),
                mergeStartDate(group),
                mergeEndDate(group),
                urls
        );
    }

    public static LocalDate mergeStartDate(final List<Event> group) {
        return group.stream()
                .map(Event::startDate)
                .min(LocalDate::compareTo)
                .orElse(group.getFirst().startDate());
    }

    public static LocalDate mergeEndDate(final List<Event> group) {
        return group.stream()
                .map(Event::endDate)
                .max(LocalDate::compareTo)
                .orElse(group.getFirst().endDate());
    }

    public static String extractLastPathSegment(final String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        final var lastSlash = url.lastIndexOf('/');
        return lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
    }

    public static String slugify(final String text) {
        return normalizeWhitespace(text)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-)|(-$)", "");
    }
}

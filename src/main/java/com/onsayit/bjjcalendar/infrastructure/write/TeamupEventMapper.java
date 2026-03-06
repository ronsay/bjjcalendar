package com.onsayit.bjjcalendar.infrastructure.write;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.infrastructure.teamup.model.EventCreate;
import com.onsayit.bjjcalendar.infrastructure.teamup.model.EventRead;
import com.onsayit.bjjcalendar.infrastructure.teamup.model.EventUpdate;
import com.onsayit.bjjcalendar.infrastructure.teamup.model.SubCalendarRead;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
public class TeamupEventMapper {

    public EventCreate toEventCreate(final Event event, final List<SubCalendarRead> subCalendars) {
        return new EventCreate()
                .title(event.name())
                .remoteId(event.id())
                .location(event.city())
                .notes(this.buildNotes(event.urls()))
                .subcalendarIds(List.of(this.mapSubCalendarId(event, subCalendars)))
                .startDt(this.formatDate(event.startDate()))
                .endDt(this.formatDate(event.endDate()))
                .allDay(Boolean.TRUE);
    }

    public EventUpdate toEventUpdate(final Event event,
                                     final EventRead existingEvent,
                                     final List<SubCalendarRead> subCalendars) {
        return new EventUpdate(existingEvent.getId())
                .title(event.name())
                .remoteId(event.id())
                .location(event.city())
                .notes(this.buildNotes(event.urls()))
                .subcalendarIds(List.of(this.mapSubCalendarId(event, subCalendars)))
                .startDt(this.formatDate(event.startDate()))
                .endDt(this.formatDate(event.endDate()))
                .allDay(Boolean.TRUE);
    }

    private String formatDate(final LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String buildNotes(final Map<String, String> urls) {
        if (urls == null || urls.isEmpty()) {
            return "";
        }
        if (urls.size() == 1) {
            final var entry = urls.entrySet().iterator().next();
            final var escapedUrl = escapeHtml(entry.getValue());
            return "<p><a href=\"" + escapedUrl
                    + "\" target=\"_blank\" rel=\"noreferrer noopener external\">"
                    + escapedUrl + "</a></p>";
        }
        final var links = urls.entrySet().stream()
                .map(entry -> {
                    final var escapedUrl = escapeHtml(entry.getValue());
                    return escapeHtml(entry.getKey()) + " : <a href=\"" + escapedUrl
                            + "\" target=\"_blank\" rel=\"noreferrer noopener external\">"
                            + escapedUrl + "</a>";
                })
                .collect(Collectors.joining("<br>"));
        return "<p>" + links + "</p>";
    }

    private String escapeHtml(final String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private Integer mapSubCalendarId(final Event event, final List<SubCalendarRead> subCalendars) {
        final var defaultSubCalendarId = subCalendars.stream()
                .filter(sub -> sub.getName() != null && "Divers".equalsIgnoreCase(sub.getName()))
                .findFirst()
                .map(SubCalendarRead::getId)
                .orElse(null);

        final var matchingSubCalendar = subCalendars.stream()
                .filter(sub -> sub.getName() != null
                        && sub.getName().equalsIgnoreCase(event.federation().name()))
                .findFirst();

        return matchingSubCalendar.map(SubCalendarRead::getId).orElse(defaultSubCalendarId);
    }
}

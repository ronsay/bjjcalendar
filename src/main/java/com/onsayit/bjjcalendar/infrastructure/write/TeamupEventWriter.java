package com.onsayit.bjjcalendar.infrastructure.write;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.ports.EventWriter;
import com.onsayit.bjjcalendar.infrastructure.config.properties.TeamupEventWriterProperties;
import com.onsayit.bjjcalendar.infrastructure.exception.TeamupException;
import com.onsayit.bjjcalendar.infrastructure.teamup.ApiClient;
import com.onsayit.bjjcalendar.infrastructure.teamup.ApiException;
import com.onsayit.bjjcalendar.infrastructure.teamup.api.AuthenticateApi;
import com.onsayit.bjjcalendar.infrastructure.teamup.api.EventsApi;
import com.onsayit.bjjcalendar.infrastructure.teamup.api.SubCalendarsApi;
import com.onsayit.bjjcalendar.infrastructure.teamup.model.EventCreate;
import com.onsayit.bjjcalendar.infrastructure.teamup.model.EventRead;
import com.onsayit.bjjcalendar.infrastructure.teamup.model.EventUpdate;
import com.onsayit.bjjcalendar.infrastructure.teamup.model.PostCreateEventRequest;
import com.onsayit.bjjcalendar.infrastructure.teamup.model.PostFosUserAuthTokensRequest;
import com.onsayit.bjjcalendar.infrastructure.teamup.model.SubCalendarRead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "teamup.api.enabled", havingValue = "true")
public class TeamupEventWriter implements EventWriter {

    private static final String DEVICE_ID = "application";
    private static final String TEAMUP_TOKEN = "Teamup-Token";

    private final TeamupEventWriterProperties props;
    private final AuthenticateApi authenticateApi;
    private final TeamupEventMapper mapper;

    @Override
    public void writeAll(final List<Event> events) {
        final var bearerToken = this.getBearerToken();
        final var subCalendarsApi = this.buildSubCalendarsApi(bearerToken);
        final var eventsApi = this.buildEventsApi(bearerToken);

        final var subCalendars = this.getSubCalendars(subCalendarsApi);
        final var existingEvents = this.getExistingEvents(eventsApi, this.getLatestStartDate(events));
        log.info("Retrieved {} sub-calendars from Teamup API", subCalendars.size());
        log.info("Retrieved {} existing events from Teamup API", existingEvents.size());

        final var eventsToCreate = new ArrayList<Event>();
        final var eventsToUpdate = new ArrayList<Map.Entry<Event, EventRead>>();

        for (final var event : events) {
            final var match = this.findMatchingEvent(event, existingEvents);
            if (match.isPresent()) {
                eventsToUpdate.add(Map.entry(event, match.get()));
            } else {
                eventsToCreate.add(event);
            }
        }

        log.info("Identified {} new events to create in Teamup", eventsToCreate.size());
        log.info("Identified {} existing events to update in Teamup", eventsToUpdate.size());

        eventsToCreate.forEach(event -> {
            final var eventCreate = mapper.toEventCreate(event, subCalendars);
            this.createEvent(eventsApi, eventCreate);
            log.info("Created event '{}' in Teamup", event.name());
        });

        eventsToUpdate.forEach(entry -> {
            final var eventUpdate = mapper.toEventUpdate(entry.getKey(), entry.getValue(), subCalendars);
            this.updateEvent(eventsApi, eventUpdate);
            log.info("Updated event '{}' in Teamup", entry.getKey().name());
        });
    }

    private String getBearerToken() {
        final var authRequest = new PostFosUserAuthTokensRequest()
                .appName(props.appName())
                .deviceId(DEVICE_ID)
                .email(props.email())
                .password(props.password());

        try {
            final var authResponse = authenticateApi.postFosUserAuthTokens(authRequest);
            return authResponse.getAuthToken();

        } catch (final ApiException e) {
            log.error("Failed to authenticate with Teamup API: {}", e.getMessage());
            throw new TeamupException("Teamup authentication failed", e);
        }
    }

    private SubCalendarsApi buildSubCalendarsApi(final String bearerToken) {
        return new SubCalendarsApi(this.buildApiClient(bearerToken));
    }

    private EventsApi buildEventsApi(final String bearerToken) {
        return new EventsApi(this.buildApiClient(bearerToken));
    }

    private ApiClient buildApiClient(final String bearerToken) {
        final var apiClient = new ApiClient();
        apiClient.updateBaseUri(props.url());
        apiClient.setRequestInterceptor(builder -> {
            builder.header(TEAMUP_TOKEN, props.token());
            builder.header("Authorization", "Bearer " + bearerToken);
        });
        return apiClient;
    }

    private List<SubCalendarRead> getSubCalendars(final SubCalendarsApi subCalendarsApi) {
        try {
            final var subCalendarsResponse = subCalendarsApi
                    .getListResources(props.calendarId(), false, null, null, null, null);

            return subCalendarsResponse.getSubcalendars();

        } catch (final ApiException e) {
            log.error("Failed to retrieve sub-calendars from Teamup API: {}", e.getMessage());
            throw new TeamupException("Failed to retrieve sub-calendars", e);
        }
    }

    private List<EventRead> getExistingEvents(final EventsApi eventsApi, final LocalDate endDate) {
        try {
            final var eventsResponse = eventsApi
                    .getListEvents(props.calendarId(), null, LocalDate.now(), endDate, null, null);

            return eventsResponse.getEvents();

        } catch (final ApiException e) {
            log.error("Failed to retrieve existing events from Teamup API: {}", e.getMessage());
            throw new TeamupException("Failed to retrieve existing events", e);
        }
    }

    private Optional<EventRead> findMatchingEvent(final Event event,
                                                  final List<EventRead> existingEvents) {
        return existingEvents.stream()
                .filter(existing ->
                        (existing.getRemoteId() != null && event.id().equals(existing.getRemoteId()))
                                || event.name().equals(existing.getTitle()))
                .findFirst();
    }

    private void createEvent(final EventsApi eventsApi, final EventCreate eventCreate) {
        try {
            eventsApi.postCreateEvent(props.calendarId(), new PostCreateEventRequest(eventCreate), null, null);

        } catch (final ApiException e) {
            log.error("Failed to create event '{}' in Teamup API: {}", eventCreate.getTitle(), e.getMessage());
            throw new TeamupException("Failed to create event: " + eventCreate.getTitle(), e);
        }
    }

    private void updateEvent(final EventsApi eventsApi, final EventUpdate eventUpdate) {
        try {
            eventsApi.putUpdateEvent(props.calendarId(), eventUpdate.getId(), eventUpdate, null);
        } catch (final ApiException e) {
            log.error("Failed to update event '{}' in Teamup API: {}", eventUpdate.getTitle(), e.getMessage());
            throw new TeamupException("Failed to update event: " + eventUpdate.getTitle(), e);
        }
    }

    private LocalDate getLatestStartDate(final List<Event> events) {
        return events.stream()
                .map(Event::startDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());
    }

}

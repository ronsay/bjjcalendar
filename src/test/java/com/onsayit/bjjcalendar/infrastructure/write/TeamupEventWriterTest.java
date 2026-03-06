package com.onsayit.bjjcalendar.infrastructure.write;

import com.onsayit.bjjcalendar.TestEventFactory;
import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.model.Federation;
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
import com.onsayit.bjjcalendar.infrastructure.teamup.model.GetListEvents200Response;
import com.onsayit.bjjcalendar.infrastructure.teamup.model.GetListResources200Response;
import com.onsayit.bjjcalendar.infrastructure.teamup.model.PostCreateEventRequest;
import com.onsayit.bjjcalendar.infrastructure.teamup.model.PostFosUserAuthTokens200Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamupEventWriterTest {

    @Mock
    private TeamupEventWriterProperties props;

    @Mock
    private AuthenticateApi authenticateApi;

    @Mock
    private TeamupEventMapper mapper;

    private TeamupEventWriter writer;

    @BeforeEach
    void setUp() {
        writer = new TeamupEventWriter(props, authenticateApi, mapper);
    }

    private void stubAuth() throws ApiException {
        final var authResponse = mock(PostFosUserAuthTokens200Response.class);
        when(authResponse.getAuthToken()).thenReturn("bearer-token-123");
        when(authenticateApi.postFosUserAuthTokens(any())).thenReturn(authResponse);
        when(props.email()).thenReturn("test@test.com");
        when(props.password()).thenReturn("password");
        when(props.appName()).thenReturn("BJJ Calendar");
        when(props.calendarId()).thenReturn("cal-123");
    }

    private MockedConstruction.MockInitializer<SubCalendarsApi> subCalInitializer(
            final GetListResources200Response response) {
        return (mock, ctx) -> when(mock.getListResources(any(), any(), any(), any(), any(), any()))
                .thenReturn(response);
    }

    private MockedConstruction.MockInitializer<EventsApi> eventsInitializer(
            final GetListEvents200Response response) {
        return (mock, ctx) -> when(mock.getListEvents(any(), any(), any(), any(), any(), any()))
                .thenReturn(response);
    }

    private GetListResources200Response subCalResponse() {
        final var response = mock(GetListResources200Response.class);
        when(response.getSubcalendars()).thenReturn(List.of());
        return response;
    }

    private GetListEvents200Response eventsResponse(final List<EventRead> events) {
        final var response = mock(GetListEvents200Response.class);
        when(response.getEvents()).thenReturn(events);
        return response;
    }

    @Nested
    class WhenCreatingEvents {

        @Test
        void should_create_new_events_not_found_in_existing() throws Exception {
            // given
            stubAuth();
            final var event = TestEventFactory.create(Federation.IBJJF, "ibjjf-1", "World",
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), Map.of());
            when(mapper.toEventCreate(any(Event.class), any())).thenReturn(new EventCreate().title("World"));

            try (var ignoredApiClient = mockConstruction(ApiClient.class);
                 var ignoredSubCalendarsApi = mockConstruction(SubCalendarsApi.class,
                         subCalInitializer(subCalResponse()));
                 var eventsMock = mockConstruction(EventsApi.class,
                         eventsInitializer(eventsResponse(List.of())))) {

                // when
                writer.writeAll(List.of(event));

                // then
                final var eventsApi = eventsMock.constructed().getFirst();
                verify(eventsApi).postCreateEvent(eq("cal-123"), any(PostCreateEventRequest.class),
                        isNull(), isNull());
            }
        }

        @Test
        void should_create_all_new_events() throws Exception {
            // given
            stubAuth();
            final var events = List.of(
                    TestEventFactory.create(Federation.IBJJF, "ibjjf-1", "E1",
                            LocalDate.now().plusDays(10), LocalDate.now().plusDays(11), Map.of()),
                    TestEventFactory.create(Federation.IBJJF, "ibjjf-2", "E2",
                            LocalDate.now().plusDays(20), LocalDate.now().plusDays(21), Map.of()),
                    TestEventFactory.create(Federation.IBJJF, "ibjjf-3", "E3",
                            LocalDate.now().plusDays(30), LocalDate.now().plusDays(31), Map.of()));
            when(mapper.toEventCreate(any(Event.class), any())).thenReturn(new EventCreate().title("E"));

            try (var ignoredApiClient = mockConstruction(ApiClient.class);
                 var ignoredSubCalendarsApi = mockConstruction(SubCalendarsApi.class,
                         subCalInitializer(subCalResponse()));
                 var eventsMock = mockConstruction(EventsApi.class,
                         eventsInitializer(eventsResponse(List.of())))) {

                // when
                writer.writeAll(events);

                // then
                final var eventsApi = eventsMock.constructed().getFirst();
                verify(eventsApi, times(3)).postCreateEvent(any(), any(PostCreateEventRequest.class), any(), any());
            }
        }
    }

    @Nested
    class WhenUpdatingEvents {

        @Test
        void should_update_event_matched_by_remote_id() throws Exception {
            // given
            stubAuth();
            final var existingEvent = new EventRead();
            existingEvent.setRemoteId("ibjjf-1");
            existingEvent.setTitle("Old Title");
            final var event = TestEventFactory.create(Federation.IBJJF, "ibjjf-1", "New Title",
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), Map.of());
            final var eventUpdate = new EventUpdate("evt-id-1").title("New Title");
            when(mapper.toEventUpdate(any(Event.class), any(EventRead.class), any())).thenReturn(eventUpdate);

            try (var ignoredApiClient = mockConstruction(ApiClient.class);
                 var ignoredSubCalendarsApi = mockConstruction(SubCalendarsApi.class,
                         subCalInitializer(subCalResponse()));
                 var eventsMock = mockConstruction(EventsApi.class,
                         eventsInitializer(eventsResponse(List.of(existingEvent))))) {

                // when
                writer.writeAll(List.of(event));

                // then
                final var eventsApi = eventsMock.constructed().getFirst();
                verify(eventsApi).putUpdateEvent(eq("cal-123"), eq("evt-id-1"), eq(eventUpdate), isNull());
                verify(eventsApi, never()).postCreateEvent(any(), any(), any(), any());
            }
        }

        @Test
        void should_update_event_matched_by_title() throws Exception {
            // given
            stubAuth();
            final var existingEvent = new EventRead();
            existingEvent.setTitle("World Championship");
            final var event = TestEventFactory.create(Federation.IBJJF, "ibjjf-1", "World Championship",
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), Map.of());
            final var eventUpdate = new EventUpdate("evt-id-2").title("World Championship");
            when(mapper.toEventUpdate(any(Event.class), any(EventRead.class), any())).thenReturn(eventUpdate);

            try (var ignoredApiClient = mockConstruction(ApiClient.class);
                 var ignoredSubCalendarsApi = mockConstruction(SubCalendarsApi.class,
                         subCalInitializer(subCalResponse()));
                 var eventsMock = mockConstruction(EventsApi.class,
                         eventsInitializer(eventsResponse(List.of(existingEvent))))) {

                // when
                writer.writeAll(List.of(event));

                // then
                final var eventsApi = eventsMock.constructed().getFirst();
                verify(eventsApi).putUpdateEvent(eq("cal-123"), eq("evt-id-2"), eq(eventUpdate), isNull());
            }
        }

        @Test
        void should_update_all_matching_events() throws Exception {
            // given
            stubAuth();
            final var existing1 = new EventRead();
            existing1.setRemoteId("ibjjf-1");
            final var existing2 = new EventRead();
            existing2.setRemoteId("ibjjf-2");
            final var existing3 = new EventRead();
            existing3.setRemoteId("ibjjf-3");
            final var events = List.of(
                    TestEventFactory.create(Federation.IBJJF, "ibjjf-1", "E1",
                            LocalDate.now().plusDays(10), LocalDate.now().plusDays(11), Map.of()),
                    TestEventFactory.create(Federation.IBJJF, "ibjjf-2", "E2",
                            LocalDate.now().plusDays(20), LocalDate.now().plusDays(21), Map.of()),
                    TestEventFactory.create(Federation.IBJJF, "ibjjf-3", "E3",
                            LocalDate.now().plusDays(30), LocalDate.now().plusDays(31), Map.of()));
            when(mapper.toEventUpdate(any(Event.class), any(EventRead.class), any()))
                    .thenReturn(new EventUpdate("id").title("E"));

            try (var ignoredApiClient = mockConstruction(ApiClient.class);
                 var ignoredSubCalendarsApi = mockConstruction(SubCalendarsApi.class,
                         subCalInitializer(subCalResponse()));
                 var eventsMock = mockConstruction(EventsApi.class,
                         eventsInitializer(eventsResponse(List.of(existing1, existing2, existing3))))) {

                // when
                writer.writeAll(events);

                // then
                final var eventsApi = eventsMock.constructed().getFirst();
                verify(eventsApi, times(3)).putUpdateEvent(any(), any(), any(EventUpdate.class), any());
            }
        }
    }

    @Nested
    class WhenMixedCreateAndUpdate {

        @Test
        void should_partition_events_into_create_and_update() throws Exception {
            // given
            stubAuth();
            final var existingEvent = new EventRead();
            existingEvent.setRemoteId("ibjjf-1");
            final var existingDomainEvent = TestEventFactory.create(Federation.IBJJF, "ibjjf-1", "Existing",
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(11), Map.of());
            final var newDomainEvent = TestEventFactory.create(Federation.AJP, "ajp-1", "New",
                    LocalDate.now().plusDays(20), LocalDate.now().plusDays(21), Map.of());
            when(mapper.toEventCreate(any(Event.class), any())).thenReturn(new EventCreate().title("New"));
            when(mapper.toEventUpdate(any(Event.class), any(EventRead.class), any()))
                    .thenReturn(new EventUpdate("id").title("Existing"));

            try (var ignoredApiClient = mockConstruction(ApiClient.class);
                 var ignoredSubCalendarsApi = mockConstruction(SubCalendarsApi.class,
                         subCalInitializer(subCalResponse()));
                 var eventsMock = mockConstruction(EventsApi.class,
                         eventsInitializer(eventsResponse(List.of(existingEvent))))) {

                // when
                writer.writeAll(List.of(existingDomainEvent, newDomainEvent));

                // then
                final var eventsApi = eventsMock.constructed().getFirst();
                verify(eventsApi).postCreateEvent(any(), any(PostCreateEventRequest.class), any(), any());
                verify(eventsApi).putUpdateEvent(any(), any(), any(EventUpdate.class), any());
            }
        }
    }

    @Nested
    class WhenEmptyEvents {

        @Test
        void should_handle_empty_events_list() throws Exception {
            // given
            stubAuth();

            try (var ignoredApiClient = mockConstruction(ApiClient.class);
                 var ignoredSubCalendarsApi = mockConstruction(SubCalendarsApi.class,
                         subCalInitializer(subCalResponse()));
                 var eventsMock = mockConstruction(EventsApi.class,
                         eventsInitializer(eventsResponse(List.of())))) {

                // when
                writer.writeAll(List.of());

                // then
                final var eventsApi = eventsMock.constructed().getFirst();
                verify(eventsApi, never()).postCreateEvent(any(), any(), any(), any());
                verify(eventsApi, never()).putUpdateEvent(any(), any(), any(EventUpdate.class), any());
            }
        }
    }

    @Nested
    class BuildApiClientInterceptor {

        @SuppressWarnings("unchecked")
        @Test
        void should_configure_api_client_with_base_uri_and_interceptor() throws Exception {
            // given
            stubAuth();
            when(props.url()).thenReturn("https://api.teamup.com");
            when(props.token()).thenReturn("teamup-token-xyz");

            try (var apiClientMock = mockConstruction(ApiClient.class);
                 var ignoredSubCalendarsApi = mockConstruction(SubCalendarsApi.class,
                         subCalInitializer(subCalResponse()));
                 var ignoredEventsApi = mockConstruction(EventsApi.class,
                         eventsInitializer(eventsResponse(List.of())))) {

                // when
                writer.writeAll(List.of());

                // then — two ApiClient instances are created (one for SubCalendarsApi, one for EventsApi)
                assertThat(apiClientMock.constructed()).hasSize(2);
                final var firstApiClient = apiClientMock.constructed().getFirst();
                verify(firstApiClient).updateBaseUri("https://api.teamup.com");

                // capture and invoke the interceptor to cover the lambda body
                final var captor = org.mockito.ArgumentCaptor.forClass(Consumer.class);
                verify(firstApiClient).setRequestInterceptor(captor.capture());
                final Consumer<HttpRequest.Builder> interceptor = captor.getValue();

                final var requestBuilder = mock(HttpRequest.Builder.class);
                when(requestBuilder.header(any(), any())).thenReturn(requestBuilder);
                interceptor.accept(requestBuilder);

                verify(requestBuilder).header("Teamup-Token", "teamup-token-xyz");
                verify(requestBuilder).header("Authorization", "Bearer bearer-token-123");
            }
        }
    }

    @Nested
    class WhenAuthenticationFails {

        @Test
        void should_throw_teamup_exception_on_auth_failure() throws ApiException {
            // given
            when(props.email()).thenReturn("test@test.com");
            when(props.password()).thenReturn("password");
            when(authenticateApi.postFosUserAuthTokens(any())).thenThrow(new ApiException("Unauthorized"));

            // when / then
            final List<Event> list = List.of();
            assertThatThrownBy(() -> writer.writeAll(list))
                    .isInstanceOf(TeamupException.class)
                    .hasMessageContaining("Teamup authentication failed");
        }
    }

    @Nested
    class WhenSubCalendarsFetchFails {

        @Test
        void should_throw_teamup_exception_on_subcalendar_failure() throws ApiException {
            // given
            stubAuth();

            try (var ignoredApiClient = mockConstruction(ApiClient.class);
                 var ignoredSubCalendarsApi = mockConstruction(SubCalendarsApi.class, (mock, ctx) ->
                         when(mock.getListResources(any(), any(), any(), any(), any(), any()))
                                 .thenThrow(new ApiException("Sub-calendar error")));
                 var ignoredEventsApi = mockConstruction(EventsApi.class)) {

                // when / then
                final List<Event> list = List.of();
                assertThatThrownBy(() -> writer.writeAll(list))
                        .isInstanceOf(TeamupException.class)
                        .hasMessageContaining("Failed to retrieve sub-calendars");
            }
        }
    }

    @Nested
    class WhenEventsFetchFails {

        @Test
        void should_throw_teamup_exception_on_events_fetch_failure() throws ApiException {
            // given
            stubAuth();

            try (var ignoredApiClient = mockConstruction(ApiClient.class);
                 var ignoredSubCalendarsApi = mockConstruction(SubCalendarsApi.class,
                         subCalInitializer(subCalResponse()));
                 var ignoredEventsApi = mockConstruction(EventsApi.class, (mock, ctx) ->
                         when(mock.getListEvents(any(), any(), any(), any(), any(), any()))
                                 .thenThrow(new ApiException("Events fetch error")))) {

                // when / then
                final List<Event> list = List.of();
                assertThatThrownBy(() -> writer.writeAll(list))
                        .isInstanceOf(TeamupException.class)
                        .hasMessageContaining("Failed to retrieve existing events");
            }
        }
    }

    @Nested
    class WhenCreateEventFails {

        @Test
        void should_throw_teamup_exception_on_create_failure() throws Exception {
            // given
            stubAuth();
            final var event = TestEventFactory.create(Federation.IBJJF, "ibjjf-1", "World",
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), Map.of());
            when(mapper.toEventCreate(any(Event.class), any())).thenReturn(new EventCreate().title("World"));
            final var emptyEventsResponse = eventsResponse(List.of());

            try (var ignoredApiClient = mockConstruction(ApiClient.class);
                 var ignoredSubCalendarsApi = mockConstruction(SubCalendarsApi.class,
                         subCalInitializer(subCalResponse()));
                 var ignoredEventsApi = mockConstruction(EventsApi.class, (mock, ctx) -> {
                     when(mock.getListEvents(any(), any(), any(), any(), any(), any()))
                             .thenReturn(emptyEventsResponse);
                     when(mock.postCreateEvent(any(), any(PostCreateEventRequest.class), any(), any()))
                             .thenThrow(new ApiException("Create failed"));
                 })) {

                // when / then
                final var list = List.of(event);
                assertThatThrownBy(() -> writer.writeAll(list))
                        .isInstanceOf(TeamupException.class)
                        .hasMessageContaining("Failed to create event: World");
            }
        }
    }

    @Nested
    class WhenUpdateEventFails {

        @Test
        void should_throw_teamup_exception_on_update_failure() throws Exception {
            // given
            stubAuth();
            final var existingEvent = new EventRead();
            existingEvent.setRemoteId("ibjjf-1");
            final var event = TestEventFactory.create(Federation.IBJJF, "ibjjf-1", "World",
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), Map.of());
            final var eventUpdate = new EventUpdate("evt-id").title("World");
            when(mapper.toEventUpdate(any(Event.class), any(EventRead.class), any())).thenReturn(eventUpdate);
            final var existingEventsResponse = eventsResponse(List.of(existingEvent));

            try (var ignoredApiClient = mockConstruction(ApiClient.class);
                 var ignoredSubCalendarsApi = mockConstruction(SubCalendarsApi.class,
                         subCalInitializer(subCalResponse()));
                 var ignoredEventsApi = mockConstruction(EventsApi.class, (mock, ctx) -> {
                     when(mock.getListEvents(any(), any(), any(), any(), any(), any()))
                             .thenReturn(existingEventsResponse);
                     when(mock.putUpdateEvent(any(), any(), any(EventUpdate.class), any()))
                             .thenThrow(new ApiException("Update failed"));
                 })) {

                // when / then
                final var list = List.of(event);
                assertThatThrownBy(() -> writer.writeAll(list))
                        .isInstanceOf(TeamupException.class)
                        .hasMessageContaining("Failed to update event: World");
            }
        }
    }
}

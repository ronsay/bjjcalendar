package com.onsayit.bjjcalendar.application;

import com.onsayit.bjjcalendar.TestEventFactory;
import com.onsayit.bjjcalendar.domain.model.Federation;
import com.onsayit.bjjcalendar.domain.ports.EventReader;
import com.onsayit.bjjcalendar.domain.ports.EventWriter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ImportEventsUseCaseTest {

    @Mock
    private EventWriter writer;

    @InjectMocks
    private ImportEventsUseCase useCase;

    @Captor
    private ArgumentCaptor<List<com.onsayit.bjjcalendar.domain.model.Event>> eventsCaptor;

    @Nested
    class WhenAllReadersSucceed {

        @Test
        void should_write_events_from_all_readers() {
            // given
            final var reader1 = (EventReader) () -> List.of(
                    TestEventFactory.create(Federation.IBJJF, "Event A"));
            final var reader2 = (EventReader) () -> List.of(
                    TestEventFactory.create(Federation.AJP, "Event B"));
            useCase = new ImportEventsUseCase(List.of(reader1, reader2), writer);
            doNothing().when(writer).writeAll(anyList());

            // when
            useCase.run();

            // then
            verify(writer).writeAll(eventsCaptor.capture());
            assertThat(eventsCaptor.getValue()).hasSize(2);
        }

        @Test
        void should_filter_past_events() {
            // given
            final var pastEvent = TestEventFactory.create(Federation.IBJJF, "ibjjf-1", "Past",
                    LocalDate.now().minusDays(5), LocalDate.now().minusDays(4),
                    Map.of());
            final var futureEvent = TestEventFactory.create(Federation.IBJJF, "ibjjf-2", "Future",
                    LocalDate.now().plusDays(5), LocalDate.now().plusDays(6),
                    Map.of());
            final var reader = (EventReader) () -> List.of(pastEvent, futureEvent);
            useCase = new ImportEventsUseCase(List.of(reader), writer);
            doNothing().when(writer).writeAll(anyList());

            // when
            useCase.run();

            // then
            verify(writer).writeAll(eventsCaptor.capture());
            assertThat(eventsCaptor.getValue()).hasSize(1);
            assertThat(eventsCaptor.getValue().getFirst().name()).isEqualTo("Future");
        }

        @Test
        void should_keep_events_ending_today() {
            // given
            final var todayEvent = TestEventFactory.create(Federation.IBJJF, "ibjjf-1", "Today",
                    LocalDate.now().minusDays(1), LocalDate.now(),
                    Map.of());
            final var reader = (EventReader) () -> List.of(todayEvent);
            useCase = new ImportEventsUseCase(List.of(reader), writer);
            doNothing().when(writer).writeAll(anyList());

            // when
            useCase.run();

            // then
            verify(writer).writeAll(eventsCaptor.capture());
            assertThat(eventsCaptor.getValue()).hasSize(1);
        }
    }

    @Nested
    class WhenOneReaderFails {

        @Test
        void should_continue_with_other_readers() {
            // given
            final var failingReader = (EventReader) () -> {
                throw new RuntimeException("API down");
            };
            final var successReader = (EventReader) () -> List.of(
                    TestEventFactory.create(Federation.AJP, "Event B"));
            useCase = new ImportEventsUseCase(List.of(failingReader, successReader), writer);
            doNothing().when(writer).writeAll(anyList());

            // when
            useCase.run();

            // then
            verify(writer).writeAll(eventsCaptor.capture());
            assertThat(eventsCaptor.getValue()).hasSize(1);
        }

        @Test
        void should_write_empty_list_when_all_readers_fail() {
            // given
            final var failingReader = (EventReader) () -> {
                throw new RuntimeException("API down");
            };
            useCase = new ImportEventsUseCase(List.of(failingReader), writer);
            doNothing().when(writer).writeAll(anyList());

            // when
            useCase.run();

            // then
            verify(writer).writeAll(eventsCaptor.capture());
            assertThat(eventsCaptor.getValue()).isEmpty();
        }
    }
}

package com.onsayit.bjjcalendar.infrastructure.write;

import com.onsayit.bjjcalendar.TestEventFactory;
import com.onsayit.bjjcalendar.domain.model.Federation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;

class ConsoleEventWriterTest {

    private final ConsoleEventWriter writer = new ConsoleEventWriter();

    @Test
    void should_not_throw_for_events() {
        // given
        final var events = List.of(
                TestEventFactory.create(Federation.IBJJF, "Event A"),
                TestEventFactory.create(Federation.AJP, "Event B")
        );

        // when / then
        assertThatNoException().isThrownBy(() -> writer.writeAll(events));
    }

    @Test
    void should_handle_empty_list() {
        assertThatNoException().isThrownBy(() -> writer.writeAll(List.of()));
    }
}

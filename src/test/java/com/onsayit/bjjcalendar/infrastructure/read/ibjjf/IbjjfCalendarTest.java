package com.onsayit.bjjcalendar.infrastructure.read.ibjjf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IbjjfCalendarTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void should_deserialize_from_json() throws Exception {
        // given
        final var json = """
                {
                  "infosite_events": [
                    {
                      "id": 1,
                      "name": "World Championship",
                      "championshipType": "Championship",
                      "region": "Europe",
                      "startDay": 5,
                      "endDay": 7,
                      "month": "JUN",
                      "year": 2025,
                      "local": "Arena",
                      "city": "Paris",
                      "status": "Confirmed",
                      "pageUrl": "/events/world-2025"
                    }
                  ]
                }
                """;

        // when
        final var calendar = mapper.readValue(json, IbjjfCalendar.class);

        // then
        assertThat(calendar.events()).hasSize(1);
        assertThat(calendar.events().getFirst().name()).isEqualTo("World Championship");
        assertThat(calendar.events().getFirst().id()).isEqualTo(1);
    }

    @Test
    void should_deserialize_multiple_events() throws Exception {
        // given
        final var json = """
                {
                  "infosite_events": [
                    {"id": 1, "name": "Event A", "championshipType": "Open", "region": "Europe",
                     "startDay": 1, "endDay": 2, "month": "JAN", "year": 2025,
                     "local": "V1", "city": "C1", "status": "Confirmed", "pageUrl": "/e/1"},
                    {"id": 2, "name": "Event B", "championshipType": "Pro", "region": "Asia",
                     "startDay": 10, "endDay": 11, "month": "FEB", "year": 2025,
                     "local": "V2", "city": "C2", "status": "Confirmed", "pageUrl": "/e/2"}
                  ]
                }
                """;

        // when
        final var calendar = mapper.readValue(json, IbjjfCalendar.class);

        // then
        assertThat(calendar.events()).hasSize(2);
    }

    @Test
    void should_deserialize_empty_events_list() throws Exception {
        // given
        final var json = """
                {"infosite_events": []}
                """;

        // when
        final var calendar = mapper.readValue(json, IbjjfCalendar.class);

        // then
        assertThat(calendar.events()).isEmpty();
    }

    @Test
    void should_ignore_unknown_properties() throws Exception {
        // given
        final var json = """
                {
                  "infosite_events": [],
                  "unknown_field": "some value"
                }
                """;

        // when
        final var calendar = mapper.readValue(json, IbjjfCalendar.class);

        // then
        assertThat(calendar.events()).isEmpty();
    }
}

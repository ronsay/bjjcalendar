package com.onsayit.bjjcalendar.domain.ports;

import com.onsayit.bjjcalendar.domain.model.Event;

import java.util.List;

public interface EventReader {
    List<Event> fetchEvents();
}


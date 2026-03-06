package com.onsayit.bjjcalendar.application;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JobRunner implements CommandLineRunner {
    private final ImportEventsUseCase useCase;
    @Override public void run(final String... args) {
        useCase.run();
    }
}

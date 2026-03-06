package com.onsayit.bjjcalendar.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobRunnerTest {

    @Mock
    private ImportEventsUseCase useCase;

    @InjectMocks
    private JobRunner jobRunner;

    @Test
    void should_delegate_to_use_case() {
        // when
        jobRunner.run();

        // then
        verify(useCase).run();
    }
}

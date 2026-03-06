package com.onsayit.bjjcalendar.infrastructure.read.smoothcomp;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.model.Federation;
import com.onsayit.bjjcalendar.infrastructure.config.properties.SmoothcompProperties;
import com.onsayit.bjjcalendar.infrastructure.read.utils.ReaderUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SmoothcompMapper {

    private final SmoothcompProperties props;

    public Event toEvent(final SmoothcompEvent event) {
        final var federation = this.getFederation(event.url());

        return new Event(
                ReaderUtils.generateId(federation, event.id()),
                federation,
                event.title(),
                null,
                event.city(),
                event.country(),
                ReaderUtils.parseDate(event.startDate()),
                ReaderUtils.parseDate(event.endDate()),
                Map.of("", event.url())
        );
    }

    private Federation getFederation(final String url) {
        return switch (url) {
            case String c when c.contains(props.grapplingIndustriesUrl())
                    -> Federation.GRAPPLING_INDUSTRIES;
            case String c when c.contains(props.nagaUrl())
                    -> Federation.NAGA;
            default -> Federation.OTHER;
        };
    }
}

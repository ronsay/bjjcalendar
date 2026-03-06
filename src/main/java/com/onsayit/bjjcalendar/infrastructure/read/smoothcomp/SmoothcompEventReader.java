package com.onsayit.bjjcalendar.infrastructure.read.smoothcomp;

import com.onsayit.bjjcalendar.domain.model.Event;
import com.onsayit.bjjcalendar.domain.ports.EventReader;
import com.onsayit.bjjcalendar.infrastructure.config.properties.SmoothcompProperties;
import com.onsayit.bjjcalendar.infrastructure.read.utils.ReaderUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "sources.smoothcomp", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class SmoothcompEventReader implements EventReader {

    private static final String URL_FORMAT = "%s?countries=%s";

    private final SmoothcompJsonClient client;
    private final SmoothcompMapper mapper;
    private final SmoothcompProperties props;
    private final SmoothcompDistanceFilter distanceFilter;

    @Override
    public List<Event> fetchEvents() {
        final var allEvents = client.fetchEventList(buildUrl());

        final Map<Boolean, List<SmoothcompEvent>> partitioned = allEvents.stream()
                .collect(Collectors.partitioningBy(e -> matchesKnownFederation(e.url())));

        final var knownEvents = partitioned.get(true);

        final var otherCandidates = partitioned.get(false).stream()
                .filter(e -> matchesOtherUrl(e.url()) || isInCountryList(e.countryCode()))
                .toList();

        final var filteredOthers = distanceFilter.filterByDistance(otherCandidates);

        final var combined = new ArrayList<SmoothcompEvent>(knownEvents.size() + filteredOthers.size());
        combined.addAll(knownEvents);
        combined.addAll(filteredOthers);

        return ReaderUtils.mapAndFilter(combined.stream(), mapper::toEvent);
    }

    private String buildUrl() {
        final var baseUrl = props.url();
        if (props.countries().isEmpty()) {
            return baseUrl;
        }
        final var countriesParam = String.join(",", props.countries());
        return String.format(URL_FORMAT, baseUrl, countriesParam);
    }

    private boolean matchesKnownFederation(final String url) {
        return url.contains(props.grapplingIndustriesUrl())
                || url.contains(props.nagaUrl());
    }

    private boolean matchesOtherUrl(final String url) {
        return props.otherUrls().stream().anyMatch(url::contains);
    }

    private boolean isInCountryList(final String countryCode) {
        return !props.countries().isEmpty()
                && props.countries().stream().anyMatch(c -> c.equalsIgnoreCase(countryCode));
    }
}

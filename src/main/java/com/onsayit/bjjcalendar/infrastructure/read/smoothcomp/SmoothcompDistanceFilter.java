package com.onsayit.bjjcalendar.infrastructure.read.smoothcomp;

import com.onsayit.bjjcalendar.infrastructure.config.properties.SmoothcompProperties;
import com.onsayit.bjjcalendar.infrastructure.read.geoapify.GeoapifyBatchGeocodingClient;
import com.onsayit.bjjcalendar.infrastructure.read.utils.DistanceCalculator;
import com.onsayit.bjjcalendar.infrastructure.read.utils.LocationResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmoothcompDistanceFilter {

    private final GeoapifyBatchGeocodingClient geocodingClient;
    private final SmoothcompProperties props;
    private final LocationResolver locationResolver;

    public List<SmoothcompEvent> filterByDistance(final List<SmoothcompEvent> otherEvents) {
        if (otherEvents.isEmpty() || !props.distanceFilterEnabled()) {
            return List.of();
        }

        final var uniqueAddresses = otherEvents.stream()
                .map(this::buildAddress)
                .distinct()
                .toList();


        final var geocoded = geocodingClient.batchGeocode(uniqueAddresses);

        if (geocoded.isEmpty()) {
            log.warn("Smoothcomp: geocoding returned no results, excluding all {} OTHER events (fail-closed)",
                    otherEvents.size());
            return List.of();
        }

        final var kept = new ArrayList<SmoothcompEvent>();

        for (final var event : otherEvents) {
            final var address = buildAddress(event);
            final var coords = geocoded.get(address);

            if (coords == null) {
                log.warn("Smoothcomp: no geocoding result for '{}' — excluding '{}'",
                        address, event.title());
                continue;
            }

            final double distance = DistanceCalculator.haversine(
                    props.originLatitude(), props.originLongitude(),
                    coords.latitude(), coords.longitude());

            if (distance <= props.maxDistance()) {
                log.info("Smoothcomp: keeping OTHER event '{}' ({}, {} km)",
                        event.title(), address, Math.round(distance));
                kept.add(event);
            } else {
                log.debug("Smoothcomp: excluding OTHER event '{}' ({}, {} km > {} km)",
                        event.title(), address, Math.round(distance), props.maxDistance());
            }
        }

        return kept;
    }

    private String buildAddress(final SmoothcompEvent event) {
        final var city = locationResolver.resolveCity(event.city());
        return city + ", " + event.country();
    }
}

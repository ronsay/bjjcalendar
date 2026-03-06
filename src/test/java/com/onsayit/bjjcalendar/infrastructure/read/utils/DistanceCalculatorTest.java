package com.onsayit.bjjcalendar.infrastructure.read.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DistanceCalculatorTest {

    @Test
    void should_return_zero_for_same_point() {
        assertThat(DistanceCalculator.haversine(48.8566, 2.3522, 48.8566, 2.3522))
                .isEqualTo(0.0);
    }

    @ParameterizedTest
    @CsvSource({
            // Paris to London ~343 km
            "48.8566, 2.3522, 51.5074, -0.1278, 343, 50",
            // Paris to Lyon ~392 km
            "48.8566, 2.3522, 45.7640, 4.8357, 392, 50",
            // New York to Los Angeles ~3944 km
            "40.7128, -74.0060, 34.0522, -118.2437, 3944, 100"
    })
    void should_compute_known_distances(final double lat1, final double lon1,
                                        final double lat2, final double lon2,
                                        final double expectedKm, final double tolerance) {
        assertThat(DistanceCalculator.haversine(lat1, lon1, lat2, lon2))
                .isCloseTo(expectedKm, within(tolerance));
    }

    @Test
    void should_be_symmetric() {
        // given
        final double d1 = DistanceCalculator.haversine(48.8566, 2.3522, 51.5074, -0.1278);
        final double d2 = DistanceCalculator.haversine(51.5074, -0.1278, 48.8566, 2.3522);

        // then
        assertThat(d1).isEqualTo(d2);
    }
}

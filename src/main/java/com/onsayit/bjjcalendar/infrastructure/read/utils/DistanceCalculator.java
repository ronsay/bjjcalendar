package com.onsayit.bjjcalendar.infrastructure.read.utils;

public final class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6_371.0;

    private DistanceCalculator() {
        // Utility class
    }

    public static double haversine(final double lat1, final double lon1,
                                   final double lat2, final double lon2) {
        final double dLat = Math.toRadians(lat2 - lat1);
        final double dLon = Math.toRadians(lon2 - lon1);

        final double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}

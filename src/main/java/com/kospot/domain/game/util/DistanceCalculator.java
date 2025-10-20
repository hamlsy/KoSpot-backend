package com.kospot.domain.game.util;

import com.kospot.domain.coordinate.entity.Coordinate;

public class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public static double calculateHaversineDistance(double lat, double lng, Coordinate targetCoordinate) {
        double targetLat = targetCoordinate.getLat();
        double targetLng = targetCoordinate.getLng();

        double dLat = deg2rad(targetLat - lat);
        double dLng = deg2rad(targetLng - lng);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(deg2rad(lat)) * Math.cos(deg2rad(targetLat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private static double deg2rad(double deg) {
        return deg * Math.PI / 180.0;
    }

}

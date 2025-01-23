package com.kospot.kospot.domain.coordinate.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

public class RandomCoordinateGenerator {

    private static final double MIN_LAT = 33.10000;
    private static final double MAX_LAT = 38.61000;
    private static final double MIN_LNG = 124.60000;
    private static final double MAX_LNG = 131.87000;

    // 소수점 5자리로 위도 생성
    public static double generateRandomLatitude() {
        double latitude = ThreadLocalRandom.current().nextDouble(MIN_LAT, MAX_LAT);
        return roundToFiveDecimalPlaces(latitude);
    }

    // 소수점 5자리로 경도 생성
    public static double generateRandomLongitude() {
        double longitude = ThreadLocalRandom.current().nextDouble(MIN_LNG, MAX_LNG);
        return roundToFiveDecimalPlaces(longitude);
    }

    // 소수점 5자리로 반올림
    private static double roundToFiveDecimalPlaces(double value) {
        return BigDecimal.valueOf(value)
                .setScale(5, RoundingMode.HALF_UP)
                .doubleValue();
    }

}

package com.kospot.kospot.domain.coordinate.util;

import com.kospot.kospot.domain.coordinate.entity.BoundingBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;
public class RandomCoordinateGenerator {

    private static final double MIN_LAT = 33.10000;
    private static final double MAX_LAT = 38.61000;
    private static final double MIN_LNG = 125.26000;
    private static final double MAX_LNG = 129.35;

    private static final int DECIMAL_PLACES = 14;

    // 소수점 5자리로 위도 생성
    public static double generateRandomLatitude() {
        BoundingBox box = getBoundingBox();
        double latitude = ThreadLocalRandom.current().nextDouble(box.getMinLat(), box.getMaxLat());
        return roundToDecimalPlaces(latitude);
    }

    // 소수점 5자리로 경도 생성
    public static double generateRandomLongitude() {
        BoundingBox box = getBoundingBox();
        double longitude = ThreadLocalRandom.current().nextDouble(box.getMinLng(), box.getMaxLng());
        return roundToDecimalPlaces(longitude);
    }

    // 소수점 10자리로 반올림
    private static double roundToDecimalPlaces(double value) {
        return BigDecimal.valueOf(value)
                .setScale(DECIMAL_PLACES, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static BoundingBox getBoundingBox() {
        return new BoundingBox(MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG);
    }

}
package com.kospot.kospot.domain.coordinate.util;

import com.kospot.kospot.domain.coordinate.entity.BoundingBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;
public class RandomCoordinateGenerator {

    private static final double MIN_LAT = 33.10000;
    private static final double MAX_LAT = 38.61000;
    private static final double MIN_LNG = 124.60000;
    private static final double MAX_LNG = 131.87000;

    // 대한민국 내 육지 좌표 범위
    private static final BoundingBox[] LAND_BOUNDING_BOXES = {
        new BoundingBox(33.10000, 38.61000, 124.60000, 126.60000), // 서해안
        new BoundingBox(33.10000, 38.61000, 126.60000, 129.30000), // 내륙
        new BoundingBox(33.10000, 38.61000, 129.30000, 131.87000)  // 동해안
    };

    // 소수점 5자리로 위도 생성
    public static double generateRandomLatitude() {
        BoundingBox box = getRandomBoundingBox();
        double latitude = ThreadLocalRandom.current().nextDouble(box.getMinLat(), box.getMaxLat());
        return roundToFiveDecimalPlaces(latitude);
    }

    // 소수점 5자리로 경도 생성
    public static double generateRandomLongitude() {
        BoundingBox box = getRandomBoundingBox();
        double longitude = ThreadLocalRandom.current().nextDouble(box.getMinLng(), box.getMaxLng());
        return roundToFiveDecimalPlaces(longitude);
    }

    // 소수점 5자리로 반올림
    private static double roundToFiveDecimalPlaces(double value) {
        return BigDecimal.valueOf(value)
                .setScale(5, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // 랜덤한 BoundingBox 선택
    private static BoundingBox getRandomBoundingBox() {
        int randomIndex = ThreadLocalRandom.current().nextInt(LAND_BOUNDING_BOXES.length);
        return LAND_BOUNDING_BOXES[randomIndex];
    }
}
package com.kospot.kospot.domain.coordinate.service.generator;

import com.kospot.kospot.domain.coordinate.dto.response.RandomCoordinateResponse;
import com.kospot.kospot.domain.coordinate.dto.response.kakao.KakaoPanoResponse;
import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.util.RandomCoordinateGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


@Slf4j
@Service
@RequiredArgsConstructor
public class RoadViewCoordinateGenerator {

    private WebClient webClient;

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    private static final String KAKAO_PANO_API_URL = "https://dapi.kakao.com/v2/local/geo/coord2panoinfo";
    private static final String KAKAO_ADDRESS_API_URL = "https://dapi.kakao.com/v2/local/geo/coord2address";
    private static final String KAKAO_API_URL = "https://dapi.kakao.com/v2/local/geo/coord2panoinfo";
    private static final String KAKAO_API_HEADER = "KakaoAK ";
    private static final int MAX_DISTANCE = 150;
    private static final int DISTANCE_INCREMENT = 50;

    public RandomCoordinateResponse getRandomCoordinate() {
        double randomLat = RandomCoordinateGenerator.generateRandomLatitude();
        double randomLng = RandomCoordinateGenerator.generateRandomLongitude();
        Coordinate coordinate = Coordinate.builder()
                .lat(randomLat)
                .lng(randomLng)
                .build();
        Coordinate resultCoordinate = findNearestPanoId(coordinate, DISTANCE_INCREMENT);
        return RandomCoordinateResponse.from(resultCoordinate);
    }

    private Coordinate findNearestPanoId(Coordinate coordinate, int distance){
        if (distance >= MAX_DISTANCE) {
            // todo
            throw new IllegalArgumentException();
        }

        try {
            KakaoPanoResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(KAKAO_API_URL)
                            .queryParam("x", coordinate.getLat())
                            .queryParam("y", coordinate.getLng())
                            .queryParam("radius", distance)
                            .build())
                    .header("Authorization", KAKAO_API_HEADER + kakaoApiKey)
                    .retrieve()
                    .bodyToMono(KakaoPanoResponse.class)
                    .block();

            if (response != null && response.getPanoId() != null) {
                return Coordinate.builder()
                                .lat(response.getLat())
                                .lng(response.getLng())
                        .build();
            }

            return findNearestPanoId(coordinate, distance + DISTANCE_INCREMENT);

        } catch (Exception e) {
            log.error("Failed to fetch pano info for coordinates: {}, {}",
                    coordinate.getLat(), coordinate.getLng(), e);
            // todo
            throw new IllegalArgumentException();
        }
    }

}

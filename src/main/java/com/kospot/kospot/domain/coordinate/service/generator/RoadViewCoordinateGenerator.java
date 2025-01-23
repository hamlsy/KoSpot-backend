package com.kospot.kospot.domain.coordinate.service.generator;

import com.kospot.kospot.domain.coordinate.dto.response.RandomCoordinateResponse;
import com.kospot.kospot.domain.coordinate.dto.response.kakao.KakaoPanoResponse;
import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.repository.CoordinateRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Service
public class RoadViewCoordinateGenerator {

    private WebClient webClient;
    // todo @Value("${kakao.api.key}")
    private String kakaoApiKey;

    private static final String KAKAO_PANO_API_URL = "https://dapi.kakao.com/v2/local/geo/coord2panoinfo";
    private static final String KAKAO_ADDRESS_API_URL = "https://dapi.kakao.com/v2/local/geo/coord2address";
    private static final String KAKAO_API_URL = "A";
    private static final int MAX_DISTANCE = 150;
    private static final int DISTANCE_INCREMENT = 50;

    public RandomCoordinateResponse getRandomCoordinate() {
        return null;
    }

    private Optional<Coordinate> findNearestPanoId(Coordinate coordinate, int distance){
        if (distance >= MAX_DISTANCE) {
            return Optional.empty();
        }

        try {
            KakaoPanoResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(KAKAO_API_URL)
                            .queryParam("x", coordinate.getLat())
                            .queryParam("y", coordinate.getLng())
                            .queryParam("radius", distance)
                            .build())
                    .header("Authorization", "KakaoAK " + kakaoApiKey)
                    .retrieve()
                    .bodyToMono(KakaoPanoResponse.class)
                    .block();

            if (response != null && response.getPanoId() != null) {
                return Optional.of(new Coordinate(
                        response.getLatitude(),
                        response.getLongitude()
                ));
            }

            return findNearestPanoId(coordinate, distance + DISTANCE_INCREMENT);

        } catch (Exception e) {
            log.error("Failed to fetch pano info for coordinates: {}, {}",
                    coordinate.getLatitude(), coordinate.getLongitude(), e);
            return Optional.empty();
        }
    }

}

package com.kospot.kospot.domain.coordinate.service.generator;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.repository.CoordinateRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class RoadViewCoordinateGenerator {

    private WebClient webClient;
    // todo @Value("${kakao.api.key}")
    private String kakaoApiKey;

    private static final String KAKAO_PANO_API_URL = "https://dapi.kakao.com/v2/local/geo/coord2panoinfo";
    private static final String KAKAO_ADDRESS_API_URL = "https://dapi.kakao.com/v2/local/geo/coord2address";
    private static final int MAX_DISTANCE = 150;
    private static final int DISTANCE_INCREMENT = 50;

    private Coordinate generateRandomCoordinate() {

    }

}

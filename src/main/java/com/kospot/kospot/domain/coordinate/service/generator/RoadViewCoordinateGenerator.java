package com.kospot.kospot.domain.coordinate.service.generator;

import com.kospot.kospot.domain.coordinate.repository.CoordinateRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RoadViewCoordinateGenerator {

    // todo @Value("${kakao.api.key}")

    private String kakaoApiKey;

}

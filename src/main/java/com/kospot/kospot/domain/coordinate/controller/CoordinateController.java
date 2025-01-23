package com.kospot.kospot.domain.coordinate.controller;

import com.kospot.kospot.domain.coordinate.dto.response.RandomCoordinateResponse;
import com.kospot.kospot.domain.coordinate.dto.response.kakao.KakaoPanoResponse;
import com.kospot.kospot.domain.coordinate.service.generator.RoadViewCoordinateGenerator;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CoordinateController {
    private final RoadViewCoordinateGenerator roadViewCoordinateGenerator;


    /**
     * 랜덤 좌표
     * Test 용도
     */
    @GetMapping("/randomCoord")
    public ApiResponseDto<RandomCoordinateResponse> getRandomCoord(){
        return ApiResponseDto.onSuccess(
                roadViewCoordinateGenerator.getRandomCoordinate()
        );
    }
}

package com.kospot.kospot.domain.coordinate.controller;

import com.kospot.kospot.domain.coordinate.dto.response.RandomCoordinateResponse;
import com.kospot.kospot.domain.coordinate.service.generator.RoadViewCoordinateGenerator;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@Slf4j
@RequiredArgsConstructor
public class CoordinateController {
    private final RoadViewCoordinateGenerator roadViewCoordinateGenerator;


    /**
     * 랜덤 좌표
     * Test 용도
     */
    @GetMapping("/randomCoord")
    public ApiResponseDto<RandomCoordinateResponse> getRandomCoord(){
        log.info("Controller method called");
        return ApiResponseDto.onSuccess(
                roadViewCoordinateGenerator.getRandomCoordinate()
        );
    }

}

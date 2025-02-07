package com.kospot.kospot.domain.coordinate.controller;

import com.kospot.kospot.domain.coordinate.dto.response.CoordinateResponse;
import com.kospot.kospot.domain.coordinate.dto.response.RandomCoordinateResponse;
import com.kospot.kospot.domain.coordinate.service.CoordinateService;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@Slf4j
@RequiredArgsConstructor
public class CoordinateController {

    private final CoordinateService coordinateService;

    /**
     * 랜덤 좌표
     * Test 용도
     */
    @GetMapping("/randomCoord/{sido}")
    public ApiResponseDto<CoordinateResponse> getRandomCoordBySido(@PathVariable("sido") String sido) {
        log.info("Controller method called");
        return ApiResponseDto.onSuccess(CoordinateResponse.from(
                coordinateService.getRandomCoordinateBySido(sido)
        ));
    }

    @GetMapping("/randomCoord")
    public ApiResponseDto<CoordinateResponse> getRandomCoord() {
        log.info("Controller method called");
        return ApiResponseDto.onSuccess(CoordinateResponse.from(
                coordinateService.getAllRandomCoordinate()
                )
        );
    }



}

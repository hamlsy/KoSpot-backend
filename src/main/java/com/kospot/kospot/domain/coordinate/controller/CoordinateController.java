package com.kospot.kospot.domain.coordinate.controller;

import com.kospot.kospot.domain.coordinate.dto.response.CoordinateResponse;
import com.kospot.kospot.domain.coordinate.service.CoordinateExcelService;
import com.kospot.kospot.domain.coordinate.service.CoordinateService;
import com.kospot.kospot.exception.payload.code.SuccessStatus;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
@Slf4j
@RequiredArgsConstructor
public class CoordinateController {

    private final CoordinateService coordinateService;
    private final CoordinateExcelService coordinateExcelService;

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

    /**
     * todo refactoring, 테스트용
     * 추후 관리지 권한으로 전환
     * @param fileName
     * @return
     */

    @GetMapping("/importFromExcel")
    public ApiResponseDto<?> importFromExcel(@RequestParam("fileName") String fileName) {
        log.info("Controller method called");
        coordinateExcelService.importCoordinatesFromExcel(fileName);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

}

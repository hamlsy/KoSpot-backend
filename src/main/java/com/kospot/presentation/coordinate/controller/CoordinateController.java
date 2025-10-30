package com.kospot.presentation.coordinate.controller;

import com.kospot.application.coordinate.ImportCoordinateUseCase;
import com.kospot.domain.coordinate.adaptor.CoordinateAdaptor;
import com.kospot.domain.coordinate.entity.Sido;
import com.kospot.presentation.coordinate.dto.response.CoordinateResponse;
import com.kospot.infrastructure.exception.payload.code.SuccessStatus;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class CoordinateController {

    private final CoordinateAdaptor coordinateAdaptor;
    private final ImportCoordinateUseCase importCoordinateUseCase;

    /**
     * 랜덤 좌표
     * Test 용도
     */
    @GetMapping("/randomCoord/{sido}")
    public ApiResponseDto<CoordinateResponse> getRandomCoordBySido(@PathVariable("sido") String sido) {
        return ApiResponseDto.onSuccess(CoordinateResponse.from(
                coordinateAdaptor.getRandomCoordinateBySido(Sido.fromKey(sido))
        ));
    }

    /**
     * todo refactoring, 테스트용
     * 추후 관리지 권한으로 전환
     * @return
     */

//    @GetMapping("/importFromExcel")
//    public ApiResponseDto<?> importFromExcel(@RequestParam("fileName") String fileName) {
//        importCoordinateUseCase.execute(fileName);
//        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
//    }

}

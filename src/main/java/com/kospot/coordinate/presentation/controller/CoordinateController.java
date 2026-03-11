package com.kospot.coordinate.presentation.controller;

import com.kospot.coordinate.application.usecase.ImportCoordinateUseCase;
import com.kospot.coordinate.application.adaptor.CoordinateAdaptor;
import com.kospot.coordinate.domain.entity.Sido;
import com.kospot.coordinate.presentation.response.CoordinateResponse;
import com.kospot.common.exception.payload.dto.ApiResponseDto;
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

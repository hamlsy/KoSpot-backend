package com.kospot.kospot.presentation.point;

import com.kospot.kospot.domain.point.adaptor.PointHistoryAdaptor;
import com.kospot.kospot.domain.point.dto.response.PointHistoryResponse;
import com.kospot.kospot.domain.point.service.PointHistoryService;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/pointHistory")
public class PointHistoryController {

    private final PointHistoryAdaptor adaptor;
    private final PointHistoryService service;

    /**
     * -------------TEST--------------
     */

    @GetMapping("/my")
    public ApiResponseDto<List<PointHistoryResponse>> findAllMyPointHistory(Long memberId) { // todo refactor
        return ApiResponseDto.onSuccess(service.findAllHistoryByMemberId(memberId));
    }

    /**
     ---------------------------------
     */


}

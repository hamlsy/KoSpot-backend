package com.kospot.kospot.domain.point.controller;

import com.kospot.kospot.domain.point.adaptor.PointHistoryAdaptor;
import com.kospot.kospot.domain.point.service.PointService;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/point")
public class PointController {

    private final PointHistoryAdaptor historyAdaptor;
    private final PointService pointService;


    @GetMapping("/myPointHistory")
    public ApiResponseDto<?> findMyPointHistory(Long memberId){
        return ApiResponseDto.onSuccess(historyAdaptor.queryAllHistoryByMemberId(memberId));
    }



}

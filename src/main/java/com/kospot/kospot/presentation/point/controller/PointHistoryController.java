package com.kospot.kospot.presentation.point.controller;

import com.kospot.kospot.application.point.FindAllPointHistoryPaging;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.adaptor.PointHistoryAdaptor;
import com.kospot.kospot.presentation.point.dto.response.PointHistoryResponse;
import com.kospot.kospot.domain.point.service.PointHistoryService;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "PointHistory Api", description = "포인트 기록 API")
@RequestMapping("/api/pointHistory")
public class PointHistoryController {

    private final PointHistoryAdaptor adaptor;
    private final PointHistoryService service;

    private final FindAllPointHistoryPaging findAllPointHistoryPaging;

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

    @GetMapping("/{page}/{size}")
    public ApiResponseDto<List<PointHistoryResponse>> findAllByPointHistoryPaging(Member member,
                                                                                  @PathVariable("page") int page, @PathVariable("size") int size){
        return ApiResponseDto.onSuccess(
                findAllPointHistoryPaging.execute(member, page, size)
        );
    }


}

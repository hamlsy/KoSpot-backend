package com.kospot.presentation.point.controller;

import com.kospot.application.point.FindAllPointHistoryPagingUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.point.adaptor.PointHistoryAdaptor;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.presentation.point.dto.response.PointHistoryResponse;
import com.kospot.domain.point.service.PointHistoryService;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "PointHistory Api", description = "포인트 기록 API")
@RequestMapping("/pointHistory")
public class PointHistoryController {

    private final PointHistoryAdaptor adaptor;
    private final PointHistoryService service;

    private final FindAllPointHistoryPagingUseCase findAllPointHistoryPaging;

    /**
     * -------------TEST--------------
     */

    @GetMapping("/my")
    public ApiResponseDto<List<PointHistoryResponse>> findAllMyPointHistory(Long memberId) { // todo refactor
        return ApiResponseDto.onSuccess(service.findAllHistoryByMemberId(memberId));
    }

    /**
     * ---------------------------------
     */

    @GetMapping("/")
    public ApiResponseDto<List<PointHistoryResponse>> findAllByPointHistoryPaging(@CurrentMember Member member,
                                                                                  @RequestParam(value = "page", defaultValue = "0") int page) {
        return ApiResponseDto.onSuccess(findAllPointHistoryPaging.execute(member, page));
    }


}

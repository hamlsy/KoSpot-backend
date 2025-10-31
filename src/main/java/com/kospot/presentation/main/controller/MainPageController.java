package com.kospot.presentation.main.controller;

import com.kospot.application.main.FindMainPageInfoUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.presentation.main.dto.response.MainPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Main Page Api", description = "메인 페이지 API")
@RequestMapping("/main")
public class MainPageController {

    private final FindMainPageInfoUseCase findMainPageInfoUseCase;

    @Operation(
            summary = "메인 페이지 정보 조회",
            description = "메인 페이지에 필요한 모든 정보를 한 번에 조회합니다. " +
                    "(활성화된 게임 모드, 최근 공지사항 3개, 활성화된 배너, 관리자 여부)"
    )
    @GetMapping
    public ApiResponseDto<MainPageResponse.MainPageInfo> getMainPageInfo(
            @CurrentMember Member member
    ) {
        MainPageResponse.MainPageInfo mainPageInfo = findMainPageInfoUseCase.execute(member);
        return ApiResponseDto.onSuccess(mainPageInfo);
    }
}


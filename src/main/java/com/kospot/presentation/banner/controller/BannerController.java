package com.kospot.presentation.banner.controller;

import com.kospot.application.banner.FindActiveBannersUseCase;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.presentation.banner.dto.response.BannerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Banner Api", description = "배너 API")
@RequestMapping("/banners")
public class BannerController {

    private final FindActiveBannersUseCase findActiveBannersUseCase;

    @Operation(summary = "활성화된 배너 목록 조회", description = "메인 페이지에 노출될 활성화된 배너 목록을 조회합니다.")
    @GetMapping
    public ApiResponseDto<List<BannerResponse.BannerInfo>> findActiveBanners() {
        List<BannerResponse.BannerInfo> banners = findActiveBannersUseCase.execute();
        return ApiResponseDto.onSuccess(banners);
    }
}


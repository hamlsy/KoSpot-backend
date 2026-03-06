package com.kospot.admin.presentation.controller;

import com.kospot.admin.application.usecase.banner.*;
import com.kospot.common.exception.payload.code.SuccessStatus;
import com.kospot.common.exception.payload.dto.ApiResponseDto;
import com.kospot.common.security.aop.CurrentMember;
import com.kospot.admin.presentation.dto.request.AdminBannerRequest;
import com.kospot.admin.presentation.dto.response.AdminBannerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Admin Banner Api", description = "관리자 - 배너 관리 API")
@RequestMapping("/admin/banners")
public class AdminBannerController {

    private final CreateBannerUseCase createBannerUseCase;
    private final UpdateBannerUseCase updateBannerUseCase;
    private final DeleteBannerUseCase deleteBannerUseCase;
    private final FindAllBannersUseCase findAllBannersUseCase;
    private final ActivateBannerUseCase activateBannerUseCase;
    private final DeactivateBannerUseCase deactivateBannerUseCase;

    @Operation(summary = "배너 생성", description = "관리자가 새로운 배너를 생성합니다. (이미지 파일 업로드)")
    @PostMapping(consumes = "multipart/form-data")
    public ApiResponseDto<Long> createBanner(
            @CurrentMember Long adminId,
            @Valid @ModelAttribute AdminBannerRequest.Create request
    ) {
        Long bannerId = createBannerUseCase.execute(adminId, request);
        return ApiResponseDto.onSuccess(bannerId);
    }

    @Operation(summary = "배너 수정", description = "관리자가 배너 정보를 수정합니다. (이미지 파일 선택적 업로드)")
    @PutMapping(value = "/{bannerId}", consumes = "multipart/form-data")
    public ApiResponseDto<?> updateBanner(
            @CurrentMember Long adminId,
            @PathVariable("bannerId") Long bannerId,
            @Valid @ModelAttribute AdminBannerRequest.Update request
    ) {
        updateBannerUseCase.execute(adminId, bannerId, request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "배너 삭제", description = "관리자가 배너를 삭제합니다.")
    @DeleteMapping("/{bannerId}")
    public ApiResponseDto<?> deleteBanner(
            @CurrentMember Long adminId,
            @PathVariable("bannerId") Long bannerId
    ) {
        deleteBannerUseCase.execute(adminId, bannerId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "배너 목록 조회", description = "관리자가 전체 배너 목록을 조회합니다.")
    @GetMapping
    public ApiResponseDto<List<AdminBannerResponse.BannerInfo>> findAllBanners(
            @CurrentMember Long adminId
    ) {
        List<AdminBannerResponse.BannerInfo> banners = findAllBannersUseCase.execute(adminId);
        return ApiResponseDto.onSuccess(banners);
    }

    @Operation(summary = "배너 활성화", description = "관리자가 배너를 활성화합니다.")
    @PutMapping("/{bannerId}/activate")
    public ApiResponseDto<?> activateBanner(
            @CurrentMember Long adminId,
            @PathVariable("bannerId") Long bannerId
    ) {
        activateBannerUseCase.execute(adminId, bannerId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "배너 비활성화", description = "관리자가 배너를 비활성화합니다.")
    @PutMapping("/{bannerId}/deactivate")
    public ApiResponseDto<?> deactivateBanner(
            @CurrentMember Long adminId,
            @PathVariable("bannerId") Long bannerId
    ) {
        deactivateBannerUseCase.execute(adminId, bannerId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }
}


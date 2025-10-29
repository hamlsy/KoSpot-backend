package com.kospot.presentation.admin.controller;

import com.kospot.application.admin.gameconfig.*;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.payload.code.SuccessStatus;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.presentation.admin.dto.request.AdminGameConfigRequest;
import com.kospot.presentation.admin.dto.response.AdminGameConfigResponse;
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
@Tag(name = "Admin Game Config Api", description = "관리자 - 게임 설정 관리 API")
@RequestMapping("/admin/game-configs")
public class AdminGameConfigController {

    private final CreateGameConfigUseCase createGameConfigUseCase;
    private final ActivateGameConfigUseCase activateGameConfigUseCase;
    private final DeactivateGameConfigUseCase deactivateGameConfigUseCase;
    private final FindAllGameConfigsUseCase findAllGameConfigsUseCase;
    private final DeleteGameConfigUseCase deleteGameConfigUseCase;

    @Operation(summary = "게임 설정 생성", description = "관리자가 새로운 게임 모드 설정을 생성합니다. (싱글/멀티, 로드뷰/포토, 개인전/팀전)")
    @PostMapping
    public ApiResponseDto<Long> createGameConfig(
            @CurrentMember Member admin,
            @Valid @RequestBody AdminGameConfigRequest.Create request
    ) {
        Long configId = createGameConfigUseCase.execute(admin, request);
        return ApiResponseDto.onSuccess(configId);
    }

    @Operation(summary = "게임 설정 활성화", description = "관리자가 특정 게임 모드를 활성화합니다.")
    @PutMapping("/{configId}/activate")
    public ApiResponseDto<?> activateGameConfig(
            @CurrentMember Member admin,
            @PathVariable("configId") Long configId
    ) {
        activateGameConfigUseCase.execute(admin, configId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "게임 설정 비활성화", description = "관리자가 특정 게임 모드를 비활성화합니다.")
    @PutMapping("/{configId}/deactivate")
    public ApiResponseDto<?> deactivateGameConfig(
            @CurrentMember Member admin,
            @PathVariable("configId") Long configId
    ) {
        deactivateGameConfigUseCase.execute(admin, configId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "게임 설정 목록 조회", description = "관리자가 전체 게임 모드 설정 목록을 조회합니다.")
    @GetMapping
    public ApiResponseDto<List<AdminGameConfigResponse.GameConfigInfo>> findAllGameConfigs(
            @CurrentMember Member admin
    ) {
        List<AdminGameConfigResponse.GameConfigInfo> configs = findAllGameConfigsUseCase.execute(admin);
        return ApiResponseDto.onSuccess(configs);
    }

    @Operation(summary = "게임 설정 삭제", description = "관리자가 게임 모드 설정을 삭제합니다.")
    @DeleteMapping("/{configId}")
    public ApiResponseDto<?> deleteGameConfig(
            @CurrentMember Member admin,
            @PathVariable("configId") Long configId
    ) {
        deleteGameConfigUseCase.execute(admin, configId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }
}


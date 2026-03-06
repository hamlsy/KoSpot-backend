package com.kospot.admin.presentation.controller;

import com.kospot.admin.application.usecase.access.ValidateAdminUseCase;
import com.kospot.common.exception.payload.code.SuccessStatus;
import com.kospot.common.exception.payload.dto.ApiResponseDto;
import com.kospot.common.security.aop.CurrentMember;
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
@Tag(name = "Admin Access Api", description = "관리자 접근 권한 API")
@RequestMapping("/admin/access")
public class AdminAccessController {

    private final ValidateAdminUseCase validateAdminUseCase;

    @Operation(summary = "관리자 접근 권한 검증", description = "관리자 접근 권한을 검증합니다.")
    @GetMapping
    public ApiResponseDto<?> validateAdminAccess(@CurrentMember Long memberId) {
        validateAdminUseCase.execute(memberId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

}

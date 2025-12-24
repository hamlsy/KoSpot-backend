package com.kospot.presentation.admin.controller;

import com.kospot.application.admin.access.ValidateAdminUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.payload.code.SuccessStatus;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    public ApiResponseDto<?> validateAdminAccess(@CurrentMember Member member) {
        validateAdminUseCase.execute(member);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

}

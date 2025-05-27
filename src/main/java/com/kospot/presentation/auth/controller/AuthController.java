package com.kospot.presentation.auth.controller;

import com.kospot.application.auth.LogoutUseCase;
import com.kospot.application.auth.ReIssueRefreshTokenUseCase;
import com.kospot.global.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.dto.JwtToken;
import com.kospot.presentation.auth.dto.AuthRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Auth Api", description = "인증 API")
@RequestMapping("/auth")
public class AuthController {

    private final ReIssueRefreshTokenUseCase reIssueRefreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    @Operation(summary = "토큰 재발급", description = "토큰 재발급")
    @PostMapping("/reIssue")
    public ApiResponseDto<JwtToken> reIssueRefreshToken(@RequestBody AuthRequest.ReIssue request) {
        return ApiResponseDto.onSuccess(reIssueRefreshTokenUseCase.execute(request));
    }

    @Operation(summary = "로그아웃", description = "로그아웃")
    @PostMapping("/logout")
    public ApiResponseDto<?> logout(@RequestBody AuthRequest.Logout request) {
        return ApiResponseDto.onSuccess(logoutUseCase);
    }

}

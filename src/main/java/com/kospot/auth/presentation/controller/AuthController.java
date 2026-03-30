package com.kospot.auth.presentation.controller;

import com.kospot.auth.application.usecase.LocalLoginUseCase;
import com.kospot.auth.application.usecase.LogoutUseCase;
import com.kospot.auth.application.usecase.ReIssueRefreshTokenUseCase;
import com.kospot.auth.application.usecase.TestTempLoginUseCase;
import com.kospot.common.exception.payload.code.SuccessStatus;
import com.kospot.common.exception.payload.dto.ApiResponseDto;
import com.kospot.common.security.dto.JwtToken;
import com.kospot.common.security.service.TokenService;
import com.kospot.member.application.usecase.SignUpUseCase;
import com.kospot.auth.presentation.dto.request.AuthRequest;
import com.kospot.auth.presentation.dto.response.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    private final SignUpUseCase signUpUseCase;
    private final LocalLoginUseCase localLoginUseCase;
    private final TokenService tokenService;

    //test
    private final TestTempLoginUseCase testTempLoginUseCase;

    /**
     *  Test
     */
    @Operation(summary = "테스트용 임시 로그인", description = "테스트용 임시 로그인")
    @GetMapping("/tempLogin/{username}")
    public ApiResponseDto<AuthResponse.TempLogin> tempLogin(@PathVariable("username") String username) {
        return ApiResponseDto.onSuccess(testTempLoginUseCase.testLogin(username));
    }

    @Operation(summary = "토큰 재발급", description = "토큰 재발급")
    @PostMapping("/reIssue")
    public ApiResponseDto<JwtToken> reIssueRefreshToken(@RequestBody AuthRequest.ReIssue request) {
        return ApiResponseDto.onSuccess(reIssueRefreshTokenUseCase.execute(request));
    }

    @Operation(summary = "로그아웃", description = "로그아웃")
    @PostMapping("/logout")
    public ApiResponseDto<?> logout(@RequestBody AuthRequest.Logout request) {
        logoutUseCase.execute(request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "이메일 회원가입", description = "이메일/비밀번호로 회원가입 후 JWT 즉시 발급")
    @PostMapping("/signup")
    public ApiResponseDto<AuthResponse.SignUpResult> signUp(@RequestBody @Valid AuthRequest.SignUp request) {
        JwtToken token = signUpUseCase.execute(request.getEmail(), request.getNickname(), request.getPassword());
        Long memberId = tokenService.getMemberIdFromToken(token.getAccessToken());
        return ApiResponseDto.onSuccess(AuthResponse.SignUpResult.from(memberId, token));
    }

    @Operation(summary = "이메일 로그인", description = "이메일/비밀번호로 로그인")
    @PostMapping("/login")
    public ApiResponseDto<AuthResponse.LoginResult> login(@RequestBody @Valid AuthRequest.LocalLogin request) {
        JwtToken token = localLoginUseCase.execute(request.getEmail(), request.getPassword());
        return ApiResponseDto.onSuccess(AuthResponse.LoginResult.from(token));
    }

}

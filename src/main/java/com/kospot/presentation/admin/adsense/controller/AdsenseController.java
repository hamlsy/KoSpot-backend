package com.kospot.presentation.admin.adsense.controller;

import com.kospot.application.admin.adsense.LoginAdsenseUseCase;
import com.kospot.application.admin.adsense.RegisterAdsenseBotUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.payload.code.SuccessStatus;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.infrastructure.security.dto.JwtToken;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;

@Slf4j
@Controller
@RequestMapping("/adsense")
@RequiredArgsConstructor
public class AdsenseController {

    private final LoginAdsenseUseCase loginAdsenseUseCase;
    private final RegisterAdsenseBotUseCase registerAdsenseBotUseCase;

    @Operation(summary = "애드센스 크롤러 봇 로그인", description = "애드센스 크롤러 봇 전용 로그인합니다.")
    @PostMapping("/login")
    public ResponseEntity<Void> adsenseLogin(@RequestParam("user_id") String username,
                                       @RequestParam("password") String password) {
        JwtToken token = loginAdsenseUseCase.execute(username, password);
        ResponseCookie cookie = ResponseCookie.from("accessToken", token.getAccessToken())
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(30))
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @Operation
    @PostMapping("/register")
    public ApiResponseDto<?> adsenseRegister(@RequestParam("username") String username, @CurrentMember Member admin) {
        registerAdsenseBotUseCase.execute(username ,admin);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

}

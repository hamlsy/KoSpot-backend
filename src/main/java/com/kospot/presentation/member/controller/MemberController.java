package com.kospot.presentation.member.controller;

import com.kospot.application.member.TestTempLoginUseCase;
import com.kospot.global.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.dto.JwtToken;
import com.kospot.infrastructure.security.service.TokenService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Member Api", description = "회원 API")
@RequestMapping("/member")
public class MemberController {

    private final TestTempLoginUseCase testTempLoginUseCase;

    /**
     *  Test
     */
    @GetMapping("/tempLogin/{username}")
    public ApiResponseDto<JwtToken> tempLogin(@PathVariable("username") String username) {
        return ApiResponseDto.onSuccess(testTempLoginUseCase.testLogin(username));
    }

}

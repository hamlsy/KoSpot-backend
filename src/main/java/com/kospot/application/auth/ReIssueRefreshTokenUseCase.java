package com.kospot.application.auth;

import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.security.dto.JwtToken;
import com.kospot.infrastructure.security.service.TokenService;
import com.kospot.presentation.auth.dto.AuthRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ReIssueRefreshTokenUseCase {

    private final TokenService tokenService;

    public JwtToken execute(AuthRequest.ReIssue request) {
        return tokenService.issueTokens(request.getRefreshToken());
    }

}

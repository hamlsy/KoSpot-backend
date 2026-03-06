package com.kospot.application.auth;

import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.security.service.TokenService;
import com.kospot.presentation.auth.dto.request.AuthRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class LogoutUseCase {

    private final TokenService tokenService;

    public void execute(AuthRequest.Logout request) {
        tokenService.logout(request.getRefreshToken());
    }
}

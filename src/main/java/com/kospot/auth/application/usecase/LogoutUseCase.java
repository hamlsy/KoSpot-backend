package com.kospot.auth.application.usecase;

import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.security.service.TokenService;
import com.kospot.auth.presentation.dto.request.AuthRequest;
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

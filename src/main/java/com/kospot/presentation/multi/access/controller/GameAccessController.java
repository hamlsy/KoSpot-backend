package com.kospot.presentation.multi.access.controller;

import com.kospot.application.multi.access.service.GameAccessService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.adsense.BotSuccess;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.presentation.multi.access.dto.response.GameAccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
public class GameAccessController {

    private final GameAccessService gameAccessService;

    @GetMapping("/{roomId}/access")
    @BotSuccess
    public ApiResponseDto<GameAccessResponse> checkGameAccess(
            @PathVariable("roomId") String roomId,
            @CurrentMember Member member
    ) {
        GameAccessResponse response = gameAccessService.checkAccess(member, roomId);
        return ApiResponseDto.onSuccess(response);
    }

}

package com.kospot.multi.common.access.presentation.controller;

import com.kospot.multi.common.access.application.service.GameAccessService;
import com.kospot.common.annotation.adsense.BotSuccess;
import com.kospot.common.exception.payload.dto.ApiResponseDto;
import com.kospot.common.security.aop.CurrentMember;
import com.kospot.multi.common.access.presentation.response.GameAccessResponse;
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
            @CurrentMember Long memberId
    ) {
        GameAccessResponse response = gameAccessService.checkAccess(memberId, roomId);
        return ApiResponseDto.onSuccess(response);
    }

}

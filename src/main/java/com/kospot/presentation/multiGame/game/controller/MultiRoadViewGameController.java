package com.kospot.presentation.multiGame.game.controller;

import com.kospot.application.multiGame.game.StartMultiRoadViewGameUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.exception.payload.dto.ApiResponseDto;
import com.kospot.presentation.multiGame.game.dto.request.MultiGameRequest;
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
@Tag(name = "MultiRoadViewGame Api", description = "멀티 로드뷰 게임 API")
@RequestMapping("/api/multiRoadView/")
public class MultiRoadViewGameController {

    private final StartMultiRoadViewGameUseCase startMultiRoadViewGameUseCase;

    @Operation(summary = "멀티 게임 시작", description = "멀티 게임을 시작합니다.")
    @PostMapping("/")
    public ApiResponseDto<?> startGame(Member member, @RequestBody MultiGameRequest request) {
        return ApiResponseDto.onSuccess(null);
    }

}

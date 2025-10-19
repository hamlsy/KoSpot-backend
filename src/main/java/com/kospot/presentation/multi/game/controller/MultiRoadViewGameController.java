package com.kospot.presentation.multi.game.controller;

import com.kospot.application.multi.round.roadview.solo.EndRoadViewSoloRoundUseCase;
import com.kospot.application.multi.round.roadview.NextRoadViewRoundUseCase;
import com.kospot.application.multi.round.roadview.solo.StartRoadViewSoloRoundUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.presentation.multi.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
import com.kospot.presentation.multi.round.dto.response.RoadViewRoundResponse;
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
@RequestMapping("/rooms/{roomId}/roadview/games")
public class MultiRoadViewGameController {

    private final StartRoadViewSoloRoundUseCase startRoadViewSoloRoundUseCase;
    private final NextRoadViewRoundUseCase nextRoadViewRoundUseCase;
    private final EndRoadViewSoloRoundUseCase endRoadViewSoloRoundUseCase;

    @Operation(summary = "멀티 로드뷰 개인전 게임 시작", description = "멀티 로드뷰 개인전 게임을 시작합니다.")
    @PostMapping("/solo")
    public ApiResponseDto<MultiRoadViewGameResponse.StartPlayerGame> startSoloGame(
            @PathVariable("roomId") String roomId,
            @CurrentMember Member member, 
            @RequestBody MultiGameRequest.Start request) {
        return ApiResponseDto.onSuccess(startRoadViewSoloRoundUseCase.execute(member, request));
    }

    @Operation(summary = "멀티 로드뷰 팀 게임 시작", description = "멀티 로드뷰 팀 게임을 시작합니다.")
    @PostMapping("/team")
    public ApiResponseDto<MultiRoadViewGameResponse.StartPlayerGame> startTeamGame(
            @PathVariable("roomId") String roomId,
            @CurrentMember Member member, 
            @RequestBody MultiGameRequest.Start request) {
        // TODO: 팀 모드 구현 필요
        return ApiResponseDto.onSuccess(startRoadViewSoloRoundUseCase.execute(member, request));
    }

    @Operation(summary = "멀티 로드뷰 다음 라운드", description = "멀티 로드뷰 게임의 다음 라운드를 시작합니다.")
    @PostMapping("/{gameId}/rounds")
    public ApiResponseDto<MultiRoadViewGameResponse.NextRound> createNextRound(
            @PathVariable("roomId") String roomId,
            @PathVariable("gameId") Long gameId) {
        return ApiResponseDto.onSuccess(nextRoadViewRoundUseCase.execute(Long.parseLong(roomId), gameId));
    }
}

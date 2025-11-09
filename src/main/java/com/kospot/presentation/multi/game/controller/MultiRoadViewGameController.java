package com.kospot.presentation.multi.game.controller;

import com.kospot.application.multi.game.usecase.StartRoadViewSoloGameUseCase;
import com.kospot.application.multi.round.roadview.NextRoadViewRoundUseCase;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
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

    @SuppressWarnings("unused")
    private final StartRoadViewSoloGameUseCase startRoadViewSoloGameUseCase;
    private final NextRoadViewRoundUseCase nextRoadViewRoundUseCase;

    // 자동 시작
//    @Operation(summary = "멀티 로드뷰 개인전 게임 시작", description = "멀티 로드뷰 개인전 게임을 시작합니다.")
//    @PostMapping("/{gameId}/solo")
//    public ApiResponseDto<MultiRoadViewGameResponse.StartPlayerGame> startSoloGame(
//            @PathVariable("roomId") Long roomId,
//            @PathVariable("gameId") Long gameId) {
//        return ApiResponseDto.onSuccess(startRoadViewSoloGameUseCase.execute(roomId, gameId));
//    }

//    @Operation(summary = "멀티 로드뷰 팀 게임 시작", description = "멀티 로드뷰 팀 게임을 시작합니다.")
//    @PostMapping("/team")
//    public ApiResponseDto<MultiGameResponse.StartGame> startTeamGame(
//            @PathVariable("roomId") String roomId,
//            @CurrentMember Member member,
//            @RequestBody MultiGameRequest.Start request) {
//        // TODO: 팀 모드 구현 필요
//        request.setGameRoomId(Long.parseLong(roomId));
//        request.setGameModeKey(GameMode.ROADVIEW.name());
//        request.setPlayerMatchTypeKey(PlayerMatchType.TEAM.name());
//        return ApiResponseDto.onSuccess(notifyStartGameUseCase.execute(member, request));
//    }

    // 자동 시작
//    @Operation(summary = "멀티 로드뷰 다음 라운드", description = "멀티 로드뷰 게임의 다음 라운드를 시작합니다.")
//    @PostMapping("/{gameId}/rounds")
//    public ApiResponseDto<MultiRoadViewGameResponse.NextRound> createNextRound(
//            @PathVariable("roomId") String roomId,
//            @PathVariable("gameId") Long gameId) {
//        return ApiResponseDto.onSuccess(nextRoadViewRoundUseCase.execute(Long.parseLong(roomId), gameId));
//    }

    @Operation(summary = "로드뷰 라운드 재발행", description = "좌표 로딩 실패 시 새 문제를 재발행합니다.")
    @PostMapping("/{gameId}/rounds/{roundId}/reIssue")
    public ApiResponseDto<?> reissueRound(@PathVariable("roomId") Long roomId,
                                          @PathVariable("gameId") Long gameId,
                                          @PathVariable("roundId") Long roundId) {
        Object preview = nextRoadViewRoundUseCase.reissueRound(roomId, gameId, roundId);
        return ApiResponseDto.onSuccess(preview);
    }
}

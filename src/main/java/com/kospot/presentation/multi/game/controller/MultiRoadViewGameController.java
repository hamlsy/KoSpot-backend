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

    private final NextRoadViewRoundUseCase nextRoadViewRoundUseCase;

    @Operation(summary = "로드뷰 라운드 재발행", description = "좌표 로딩 실패 시 새 문제를 재발행합니다.")
    @PostMapping("/{gameId}/rounds/{roundId}/reIssue")
    public ApiResponseDto<?> reissueRound(@PathVariable("roomId") Long roomId,
                                          @PathVariable("gameId") Long gameId,
                                          @PathVariable("roundId") Long roundId) {
        return ApiResponseDto.onSuccess(nextRoadViewRoundUseCase.reissueRound(roomId, gameId, roundId));
    }
}

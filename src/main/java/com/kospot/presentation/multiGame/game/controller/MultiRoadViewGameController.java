package com.kospot.presentation.multiGame.game.controller;

import com.kospot.application.multiGame.game.NextRoundRoadViewUseCase;
import com.kospot.application.multiGame.game.StartMultiRoadViewGameUseCase;
import com.kospot.application.multiGame.submission.SubmitRoadViewAnswerUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.exception.payload.code.SuccessStatus;
import com.kospot.exception.payload.dto.ApiResponseDto;
import com.kospot.presentation.multiGame.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multiGame.game.dto.response.MultiRoadViewGameResponse;
import com.kospot.presentation.multiGame.round.dto.request.GameRoundRequest;
import com.kospot.presentation.multiGame.submission.dto.request.SubmissionRequest;
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
@RequestMapping("/multiRoadView")
public class MultiRoadViewGameController {

    private final StartMultiRoadViewGameUseCase startMultiRoadViewGameUseCase;
    private final SubmitRoadViewAnswerUseCase submitRoadViewAnswerUseCase;
    private final NextRoundRoadViewUseCase nextRoundRoadViewUseCase;

    @Operation(summary = "멀티 로드뷰 게임 시작", description = "멀티 로드뷰 게임을 시작합니다.")
    @PostMapping("/")
    public ApiResponseDto<MultiRoadViewGameResponse.Start> startGame(Member member, @RequestBody MultiGameRequest.Start request) {
        return ApiResponseDto.onSuccess(startMultiRoadViewGameUseCase.execute(member, request));
    }

    @Operation(summary = "멀티 로드뷰 정답 제출", description = "로드뷰 게임 정답을 제출합니다.")
    @PostMapping("/{multiGameId}/rounds/{currentRound}/submissions")
    public ApiResponseDto<?> submitGuess(Member member, 
                                         @PathVariable("multiGameId") Long multiGameId,
                                         @PathVariable("currentRound") Integer currentRound,
                                         @RequestBody SubmissionRequest.RoadView request) {
        // TODO: SubmitRoadViewAnswerUseCase.execute() 메소드 구현 필요
        // 현재는 파라미터가 없는 상태
        submitRoadViewAnswerUseCase.execute();
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "멀티 로드뷰 라운드 종료", description = "멀티 로드뷰 게임의 라운드를 종료합니다.")
    @PostMapping("/{multiGameId}/rounds/{currentRound}/end")
    public ApiResponseDto<?> endRound(
            @PathVariable("multiGameId") Long multiGameId,
            @PathVariable("currentRound") Integer currentRound) {
        
        GameRoundRequest.EndRound request = GameRoundRequest.EndRound.builder()
                .multiGameId(multiGameId)
                .currentRound(currentRound)
                .build();
                
        // TODO: EndRoundRoadViewUseCase 구현 필요
        // 현재 MultiRoadViewGameResponse.EndRound 클래스가 없음
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    //todo 순위 정보를 end game response로 옮김
    @Operation(summary = "멀티 로드뷰 다음 라운드", description = "멀티 로드뷰 게임의 다음 라운드를 시작합니다.")
    @PostMapping("/{multiGameId}/rounds/{currentRound}/next")
    public ApiResponseDto<MultiRoadViewGameResponse.NextRound> nextRound(
            @PathVariable("multiGameId") Long multiGameId,
            @PathVariable("currentRound") Integer currentRound) {
        
        GameRoundRequest.NextRound request = GameRoundRequest.NextRound.builder()
                .multiGameId(multiGameId)
                .currentRound(currentRound)
                .build();
                
        return ApiResponseDto.onSuccess(nextRoundRoadViewUseCase.execute(request));
    }
}

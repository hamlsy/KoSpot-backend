package com.kospot.presentation.multiGame.game.controller;

import com.kospot.application.multiGame.game.EndPlayerRoundRoadViewUseCase;
import com.kospot.application.multiGame.game.NextRoundRoadViewUseCase;
import com.kospot.application.multiGame.game.StartMultiRoadViewGameUseCase;
import com.kospot.application.multiGame.submission.SubmitRoadViewPlayerAnswerUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.exception.payload.code.SuccessStatus;
import com.kospot.exception.payload.dto.ApiResponseDto;
import com.kospot.presentation.multiGame.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multiGame.game.dto.response.MultiRoadViewGameResponse;
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
    private final SubmitRoadViewPlayerAnswerUseCase submitRoadViewPlayerAnswerUseCase;
    private final NextRoundRoadViewUseCase nextRoundRoadViewUseCase;
    private final EndPlayerRoundRoadViewUseCase endPlayerRoundRoadViewUseCase;

    @Operation(summary = "멀티 로드뷰 게임 시작", description = "멀티 로드뷰 게임을 시작합니다.")
    @PostMapping("/")
    public ApiResponseDto<MultiRoadViewGameResponse.Start> startGame(Member member, @RequestBody MultiGameRequest.Start request) {
        return ApiResponseDto.onSuccess(startMultiRoadViewGameUseCase.execute(member, request));
    }

    @Operation(summary = "멀티 로드뷰 개인 정답 제출", description = "로드뷰 게임 개인 정답을 제출합니다.")
    @PostMapping("/rounds/{roundId}/player-submissions")
    public ApiResponseDto<?> submitPlayerAnswer(
            @PathVariable("roundId") Long roundId,
            @RequestBody SubmissionRequest.RoadViewPlayer request) {
        submitRoadViewPlayerAnswerUseCase.execute(roundId, request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "멀티 로드뷰 팀 정답 제출", description = "로드뷰 게임 팀 정답을 제출합니다.")
    @PostMapping("/rounds/{roundId}/team-submissions")
    public ApiResponseDto<?> submitTeamAnswer(
            @PathVariable("roundId") Long roundId,
            @RequestBody SubmissionRequest.RoadViewPlayer request) {
        //todo implement
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "멀티 로드뷰 개인 라운드 종료", description = "멀티 로드뷰 게임 개인 라운드를 종료합니다.")
    @PostMapping("/{multiGameId}/rounds/{roundId}/endPlayerRound")
    public ApiResponseDto<?> endPlayerRound(
            @PathVariable("multiGameId") Long multiGameId,
            @PathVariable("roundId") Long roundId) {

        // TODO: EndRoundRoadViewUseCase 구현 필요
        // 현재 MultiRoadViewGameResponse.EndRound 클래스가 없음
        endPlayerRoundRoadViewUseCase.execute(roundId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "멀티 로드뷰 팀 라운드 종료", description = "멀티 로드뷰 게임 팀 라운드를 종료합니다.")
    @PostMapping("/{multiGameId}/rounds/{roundId}/endTeamRound")
    public ApiResponseDto<?> endTeamRound(
            @PathVariable("multiGameId") Long multiGameId,
            @PathVariable("roundId") Long roundId) {

        // TODO: EndRoundRoadViewUseCase 구현 필요
        // 현재 MultiRoadViewGameResponse.EndRound 클래스가 없음
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    //todo 순위 정보를 end game response로 옮김
    @Operation(summary = "멀티 로드뷰 다음 라운드", description = "멀티 로드뷰 게임의 다음 라운드를 시작합니다.")
    @PostMapping("/{multiGameId}/rounds/nextRound/next")
    public ApiResponseDto<MultiRoadViewGameResponse.NextRound> nextRound(
            @PathVariable("multiGameId") Long multiGameId,
            @PathVariable("nextRound") int nextRound) {
        return ApiResponseDto.onSuccess(nextRoundRoadViewUseCase.execute(multiGameId, nextRound));
    }
}

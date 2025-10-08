package com.kospot.presentation.multi.game.controller;

import com.kospot.application.multi.round.EndRoadViewSoloRoundUseCase;
import com.kospot.application.multi.round.NextRoadViewRoundUseCase;
import com.kospot.application.multi.round.StartRoadViewSoloRoundUseCase;
import com.kospot.application.multi.submission.SubmitRoadViewPlayerAnswerUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.payload.code.SuccessStatus;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.presentation.multi.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
import com.kospot.presentation.multi.round.dto.response.RoadViewRoundResponse;
import com.kospot.presentation.multi.submission.dto.request.SubmissionRequest;
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
@RequestMapping("/rooms/{gameRoomId}/roadview") //todo team mode 구현
public class MultiRoadViewGameController {

    private final StartRoadViewSoloRoundUseCase startRoadViewSoloRoundUseCase;
    private final SubmitRoadViewPlayerAnswerUseCase submitRoadViewPlayerAnswerUseCase;
    private final NextRoadViewRoundUseCase nextRoadViewRoundUseCase;
    private final EndRoadViewSoloRoundUseCase endRoadViewSoloRoundUseCase;

    @Operation(summary = "멀티 로드뷰 개인전 게임 시작", description = "멀티 로드뷰 개인전 게임을 시작합니다.")
    @PostMapping("/games/solo/start")
    public ApiResponseDto<MultiRoadViewGameResponse.StartPlayerGame> startPlayerGame(@CurrentMember Member member, @RequestBody MultiGameRequest.Start request) {
        return ApiResponseDto.onSuccess(startRoadViewSoloRoundUseCase.execute(member, request));
    }

    @Operation(summary = "멀티 로드뷰 팀 게임 시작", description = "멀티 로드뷰 팀 게임을 시작합니다.")
    @PostMapping("/games/team/start")
    //todo team 구현
    public ApiResponseDto<MultiRoadViewGameResponse.StartPlayerGame> startTeamGame(@CurrentMember Member member, @RequestBody MultiGameRequest.Start request) {
        return ApiResponseDto.onSuccess(startRoadViewSoloRoundUseCase.execute(member, request));
    }

    @Operation(summary = "멀티 로드뷰 개인 정답 제출", description = "로드뷰 게임 개인 정답을 제출합니다.")
    @PostMapping("/rounds/{roundId}/solo/submissions")
    public ApiResponseDto<?> submitPlayerAnswer(
            @PathVariable("roundId") Long roundId,
            @RequestBody SubmissionRequest.RoadViewPlayer request) {
        submitRoadViewPlayerAnswerUseCase.execute(roundId, request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "멀티 로드뷰 팀 정답 제출", description = "로드뷰 게임 팀 정답을 제출합니다.")
    @PostMapping("/rounds/{roundId}/team/submissions")
    public ApiResponseDto<?> submitTeamAnswer(
            @PathVariable("roundId") Long roundId,
            @RequestBody SubmissionRequest.RoadViewPlayer request) {
        //todo implement
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "멀티 로드뷰 개인 라운드 종료", description = "멀티 로드뷰 게임 개인 라운드를 종료합니다.")
    @PostMapping("/games/{gameId}/rounds/{roundId}/solo/end")
    public ApiResponseDto<RoadViewRoundResponse.PlayerResult> endPlayerRound(
            @PathVariable("gameId") Long gameId,
            @PathVariable("roundId") Long roundId) {
        return ApiResponseDto.onSuccess(endRoadViewSoloRoundUseCase.execute(gameId, roundId));
    }

    @Operation(summary = "멀티 로드뷰 팀 라운드 종료", description = "멀티 로드뷰 게임 팀 라운드를 종료합니다.")
    @PostMapping("/games/{gameId}/rounds/{roundId}/team/end")
    public ApiResponseDto<?> endTeamRound(
            @PathVariable("gameId") Long gameId,
            @PathVariable("roundId") Long roundId) {

        // TODO: EndRoundRoadViewUseCase 구현 필요
        // 현재 MultiRoadViewGameResponse.EndRound 클래스가 없음
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "멀티 로드뷰 다음 라운드", description = "멀티 로드뷰 게임의 다음 라운드를 시작합니다.")
    @PostMapping("/games/{gameId}/rounds/next")
    public ApiResponseDto<MultiRoadViewGameResponse.NextRound> nextRound(
            @PathVariable("gameRoomId") Long gameRoomId,
            @PathVariable("gameId") Long gameId) {
        return ApiResponseDto.onSuccess(nextRoadViewRoundUseCase.execute(gameRoomId, gameId));
    }
}

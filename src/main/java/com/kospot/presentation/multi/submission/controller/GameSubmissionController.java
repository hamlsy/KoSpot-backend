package com.kospot.presentation.multi.submission.controller;

import com.kospot.application.multi.submission.http.usecase.SubmitRoadViewPlayerAnswerUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.payload.code.SuccessStatus;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.presentation.multi.submission.dto.request.SubmitRoadViewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Game Submission Api", description = "멀티 게임 정답 제출 API")
@RequestMapping("/rooms/{roomId}/games/{gameId}/rounds/{roundId}/submissions")
public class GameSubmissionController {

    private final SubmitRoadViewPlayerAnswerUseCase submitRoadViewPlayerAnswerUseCase;

    @Operation(summary = "로드뷰 개인전 정답 제출", description = "로드뷰 개인전 게임에서 플레이어가 정답을 제출합니다.")
    @PostMapping("/player")
    public ApiResponseDto<?> submitPlayerAnswer(
            @PathVariable("roomId") String roomId,
            @PathVariable("gameId") Long gameId,
            @PathVariable("roundId") Long roundId,
            @CurrentMember Member member,
            @Valid @RequestBody SubmitRoadViewRequest.Player request) {
        submitRoadViewPlayerAnswerUseCase.execute(member, roomId, gameId, roundId, request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "로드뷰 팀전 정답 제출", description = "로드뷰 팀전 게임에서 팀이 정답을 제출합니다.")
    @PostMapping("/team")
    public ApiResponseDto<?> submitTeamAnswer(
            @PathVariable("roomId") String roomId,
            @PathVariable("gameId") Long gameId,
            @PathVariable("roundId") Long roundId,
            @CurrentMember Member member,
            @Valid @RequestBody SubmitRoadViewRequest.Team request) {
        // TODO: 팀 모드 UseCase 구현 필요
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

}


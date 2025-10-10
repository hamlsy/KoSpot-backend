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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Submission Api", description = "멀티 게임 정답 제출 API")
@RequestMapping("/rooms/{gameRoomId}/roadview")
public class RoadViewSubmissionController {

    private final SubmitRoadViewPlayerAnswerUseCase submitRoadViewPlayerAnswerUseCase;

    @Operation(summary = "로드뷰 개인전 정답 제출", description = "로드뷰 개인전 게임에서 플레이어가 정답을 제출합니다.")
    @PostMapping("/submit")
    public ApiResponseDto<?> submitRoadViewAnswer(@CurrentMember Member member, //todo member 필요성?
                                                  @PathVariable("gameRoomId") Long gameRoomId,
                                                  @RequestBody SubmitRoadViewRequest.Player request)   {
        submitRoadViewPlayerAnswerUseCase.execute();
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

}

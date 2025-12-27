package com.kospot.presentation.rank.controller;

import com.kospot.application.rank.usecase.GetRankingUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "GameRank Api", description = "게임 랭크 API")
@RequestMapping("/ranks")
@RequiredArgsConstructor
public class GameRankController {

    private final GetRankingUseCase getRankingUseCase;

    @Operation(summary = "게임 랭킹 조회", description = "특정 게임 모드와 랭크 티어에 해당하는 플레이어들의 랭킹 정보를 페이지 단위로 조회합니다.")
    @GetMapping
    public ApiResponseDto<?> getRanking(
            @CurrentMember Member member,
            @RequestParam String gameMode,
            @RequestParam String rankTier,
            @RequestParam int page) {

        return ApiResponseDto.onSuccess(getRankingUseCase.execute(
                member,
                gameMode,
                rankTier,
                page
        ));
    }

}

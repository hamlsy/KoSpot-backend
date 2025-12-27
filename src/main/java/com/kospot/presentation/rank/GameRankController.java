package com.kospot.presentation.rank;

import com.kospot.application.rank.usecase.GetRankingUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/ranks")
@RequiredArgsConstructor
public class GameRankController {

    private final GetRankingUseCase getRankingUseCase;

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

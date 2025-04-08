package com.kospot.presentation.gameRank.controller;

import com.kospot.domain.gameRank.util.RatingScoreCalculator;
import com.kospot.exception.payload.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "GameRank Api", description = "게임 랭크 API")
@RequestMapping("/gameRank/")
public class GameRankController {

    /**
     *  Test
     */
    @GetMapping("/{currentRatingPoint}/{gameScore}")
    public ApiResponseDto<?> testRatingPoint(@PathVariable("currentRatingPoint") int currentRatingPoint, @PathVariable("gameScore") int gameScore) {
        return ApiResponseDto.onSuccess(RatingScoreCalculator.calculateRatingChange(gameScore, currentRatingPoint));
    }

}

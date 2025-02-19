package com.kospot.kospot.domain.game.controller;

import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.dto.response.StartGameResponse;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.domain.game.service.AESService;
import com.kospot.kospot.domain.game.util.ScoreCalculator;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/roadViewGame")
public class RoadViewGameController {

    private final AESService aesService;
    private final RoadViewGameService service;

    /**
     * -----------------TEST------------------
     */

    @GetMapping("/scoreTest/{distance}")
    public ApiResponseDto<?> testScore(@PathVariable("distance") double distance) {
        return ApiResponseDto.onSuccess(ScoreCalculator.calculateScore(distance));
    }

    @GetMapping("/encrypt/{lat}")
    public ApiResponseDto<?> testEncrypt(@PathVariable("lat") String lat) throws Exception{
        return ApiResponseDto.onSuccess(aesService.encrypt(lat));
    }

    /**
     * -----------------PRACTICE------------------
     */

    @PostMapping("/practice/start")
    public ApiResponseDto<StartGameResponse.RoadView> startPracticeGame(@RequestParam("sido") String sidoKey) {
        StartGameResponse.RoadView response = service.startPracticeGame(sidoKey);
        return ApiResponseDto.onSuccess(response);
    }

    @PostMapping("/practice/end")
    public ApiResponseDto<?> endPracticeGame(Long memberId, @RequestBody EndGameRequest.RoadView request) {
        EndGameResponse.RoadViewPractice response = service.endPracticeGame(memberId, request);
        return ApiResponseDto.onSuccess(response);
    }

    /**
     *  ------------------------------------------
     */

    /**
     *  -----------------RANK------------------
     */
    @PostMapping("/rank/start")
    public ApiResponseDto<StartGameResponse.RoadView> startRankGame() {
        StartGameResponse.RoadView response = service.startRankGame();
        return ApiResponseDto.onSuccess(response);
    }

    @PostMapping("/rank/end")
    public ApiResponseDto<?> endRankGame(@RequestBody EndGameRequest.RoadView request) {
        EndGameResponse.RoadViewRank response = service.endRankGame(request);
        return ApiResponseDto.onSuccess(response);
    }


    /**
     *  ------------------------------------------
     */

}

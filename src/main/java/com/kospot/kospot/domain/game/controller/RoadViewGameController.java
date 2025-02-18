package com.kospot.kospot.domain.game.controller;

import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.dto.response.StartGameResponse;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.domain.game.util.ScoreCalculator;
import com.kospot.kospot.exception.payload.code.SuccessStatus;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/roadViewGame")
public class RoadViewGameController {

    private final RoadViewGameService service;

    /**
     * -----------------TEST------------------
     */

    @GetMapping("/scoreTest/{distance}")
    public ApiResponseDto<?> testScore(@PathVariable("distance") double distance) {
        return ApiResponseDto.onSuccess(ScoreCalculator.calculateScore(distance));
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
    public ApiResponseDto<?> endRoadViewGame(@RequestBody EndGameRequest.RoadViewPractice request) {
        EndGameResponse.RoadViewPractice response = service.endPracticeGame(request);
        return ApiResponseDto.onSuccess(response);
    }

    /**
     *  ------------------------------------------
     */

    /**
     *  -----------------RANK------------------
     */


    /**
     *  ------------------------------------------
     */

}

package com.kospot.kospot.presentation.game;

import com.kospot.kospot.application.game.roadView.practice.EndRoadViewPracticeUseCase;
import com.kospot.kospot.application.game.roadView.practice.StartRoadViewPracticeUseCase;
import com.kospot.kospot.application.game.roadView.rank.EndRoadViewRankUseCase;
import com.kospot.kospot.application.game.roadView.rank.StartRoadViewRankUseCase;
import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.dto.response.StartGameResponse;
import com.kospot.kospot.domain.game.service.AESService;
import com.kospot.kospot.domain.game.util.ScoreCalculator;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/roadViewGame")
public class RoadViewGameController {

    private final StartRoadViewPracticeUseCase startRoadViewPracticeUseCase;
    private final StartRoadViewRankUseCase startRoadViewRankUseCase;
    private final EndRoadViewRankUseCase endRoadViewRankUseCase;
    private final EndRoadViewPracticeUseCase endRoadViewPracticeUseCase;

    private final AESService aesService;

    /**
     * -----------------TEST------------------
     */

    @GetMapping("/scoreTest/{distance}")
    public ApiResponseDto<?> testScore(@PathVariable("distance") double distance) {
        return ApiResponseDto.onSuccess(ScoreCalculator.calculateScore(distance));
    }

    @GetMapping("/encrypt/{lat}")
    public ApiResponseDto<?> testEncrypt(@PathVariable("lat") String lat) throws Exception {
        return ApiResponseDto.onSuccess(aesService.encrypt(lat));
    }

    /**
     * -----------------PRACTICE------------------
     */

    @PostMapping("/practice/start")
    public ApiResponseDto<StartGameResponse.RoadView> startPracticeGame(Member member, @RequestParam("sido") String sidoKey) {
        return ApiResponseDto.onSuccess(startRoadViewPracticeUseCase.execute(member, sidoKey));
    }

    @PostMapping("/practice/end")
    public ApiResponseDto<?> endPracticeGame(Member member, @RequestBody EndGameRequest.RoadView request) {
        return ApiResponseDto.onSuccess(endRoadViewPracticeUseCase.execute(member, request));
    }

    /**
     *  ------------------------------------------
     */

    /**
     * -----------------RANK------------------
     */
    @PostMapping("/rank/start")
    public ApiResponseDto<StartGameResponse.RoadView> startRankGame(Member member) {
        return ApiResponseDto.onSuccess(startRoadViewRankUseCase.execute(member));
    }

    @PostMapping("/rank/end")
    public ApiResponseDto<EndGameResponse.RoadViewRank> endRankGame(Member member, @RequestBody EndGameRequest.RoadView request) {
        return ApiResponseDto.onSuccess(endRoadViewRankUseCase.execute(member, request));
    }

    /**
     *  ------------------------------------------
     */

}

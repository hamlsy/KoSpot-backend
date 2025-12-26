package com.kospot.presentation.game.controller;

import com.kospot.application.game.roadview.ReIssueRoadViewCoordinateUseCase;
import com.kospot.application.game.roadview.history.usecase.GetAllRoadViewGamesUseCase;
import com.kospot.application.game.roadview.history.usecase.GetRecentThreeRoadViewGamesUseCase;
import com.kospot.application.game.roadview.practice.usecase.EndRoadViewPracticeUseCase;
import com.kospot.application.game.roadview.practice.usecase.StartRoadViewPracticeUseCase;
import com.kospot.application.game.roadview.rank.usecase.EndRoadViewRankUseCase;
import com.kospot.application.game.roadview.rank.usecase.StartRoadViewRankUseCase;
import com.kospot.infrastructure.annotation.adsense.BotSuccess;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.presentation.game.dto.request.EndGameRequest;
import com.kospot.presentation.game.dto.response.EndGameResponse;
import com.kospot.presentation.game.dto.response.RoadViewGameHistoryResponse;
import com.kospot.presentation.game.dto.response.StartGameResponse;
import com.kospot.domain.game.service.AESService;
import com.kospot.domain.game.util.ScoreCalculator;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "RoadViewGame Api", description = "로드뷰 게임 API")
@RequestMapping("/roadView")
public class RoadViewGameController {

    private final StartRoadViewPracticeUseCase startRoadViewPracticeUseCase;
    private final StartRoadViewRankUseCase startRoadViewRankUseCase;
    private final EndRoadViewRankUseCase endRoadViewRankUseCase;
    private final EndRoadViewPracticeUseCase endRoadViewPracticeUseCase;
    private final GetRecentThreeRoadViewGamesUseCase getRecentThreeRoadViewGamesUseCase;
    private final GetAllRoadViewGamesUseCase getAllRoadViewGamesUseCase;
    private final ReIssueRoadViewCoordinateUseCase reIssueRoadViewCoordinateUseCase;

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

    @Operation(summary = "로드뷰 연습 게임 시작", description = "로드뷰 연습 게임을 시작합니다.")
    @BotSuccess
    @PostMapping("/practice/start")
    public ApiResponseDto<StartGameResponse.RoadView> startPracticeGame(@CurrentMember Member member, @RequestParam("sido") String sidoKey) {
        return ApiResponseDto.onSuccess(startRoadViewPracticeUseCase.execute(member, sidoKey));
    }

    @Operation(summary = "로드뷰 연습 게임 종료", description = "로드뷰 연습 게임을 종료합니다.")
    @PostMapping("/practice/end")
    public ApiResponseDto<EndGameResponse.RoadViewPractice> endPracticeGame(@CurrentMember Member member, @RequestBody EndGameRequest.RoadView request) {
        return ApiResponseDto.onSuccess(endRoadViewPracticeUseCase.execute(member, request));
    }

    /**
     *  ------------------------------------------
     */

    /**
     * -----------------RANK------------------
     */
    @Operation(summary = "로드뷰 랭크 게임 시작", description = "로드뷰 랭크 게임을 시작합니다.")
    @BotSuccess
    @PostMapping("/rank/start")
    public ApiResponseDto<StartGameResponse.RoadView> startRankGame(@CurrentMember Member member) {
        return ApiResponseDto.onSuccess(startRoadViewRankUseCase.execute(member));
    }

    @Operation(summary = "로드뷰 랭크 게임 종료", description = "로드뷰 랭크 게임을 종료합니다.")
    @PostMapping("/rank/end")
    public ApiResponseDto<EndGameResponse.RoadViewRank> endRankGame(@CurrentMember Member member, @RequestBody EndGameRequest.RoadView request) {
        return ApiResponseDto.onSuccess(endRoadViewRankUseCase.execute(member, request));
    }

    /**
     *  ------------------------------------------
     */

    @Operation(summary = "로드뷰 좌표 재발급", description = "로드뷰 연습 게임에서 좌표를 재발급합니다.")
    @PostMapping("/{gameId}/reissue-coordinate")
    public ApiResponseDto<StartGameResponse.ReIssue> reissuePracticeCoordinate(@CurrentMember Member member, @PathVariable("gameId") Long gameId) {
        return ApiResponseDto.onSuccess(reIssueRoadViewCoordinateUseCase.execute(member, gameId));
    }


    /**
     * -----------------HISTORY------------------
     */
    @Operation(
            summary = "로드뷰 메인 페이지 조회",
            description = "로드뷰 메인 페이지에 필요한 정보를 조회합니다. " +
                    "현재 랭크 정보(티어, 레벨, 레이팅 점수, 상위 퍼센트), " +
                    "통계 정보(총 플레이 수, 최고 점수), " +
                    "최근 3개 게임 기록을 포함합니다."
    )
    @GetMapping("/history/recent")
    @BotSuccess
    public ApiResponseDto<RoadViewGameHistoryResponse.RecentThree> getRecentThreeGames(@CurrentMember Member member) {
        return ApiResponseDto.onSuccess(getRecentThreeRoadViewGamesUseCase.execute(member));
    }

    @Operation(summary = "로드뷰 게임 전체 기록 조회", description = "로드뷰 게임의 전체 완료된 기록을 페이지네이션으로 조회합니다.")
    @GetMapping("/history")
    public ApiResponseDto<RoadViewGameHistoryResponse.All> getAllGames(
            @CurrentMember Member member,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponseDto.onSuccess(getAllRoadViewGamesUseCase.execute(member, pageable));
    }

    /**
     *  ------------------------------------------
     */

}

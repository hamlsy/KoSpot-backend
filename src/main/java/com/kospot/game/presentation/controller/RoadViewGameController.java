package com.kospot.game.presentation.controller;

import com.kospot.game.application.usecase.rank.EndRoadViewRankUseCase;
import com.kospot.game.application.usecase.rank.ReIssueRoadViewCoordinateUseCase;
import com.kospot.game.application.usecase.history.GetAllRoadViewGamesUseCase;
import com.kospot.game.application.usecase.history.GetRecentThreeRoadViewGamesUseCase;
import com.kospot.game.application.usecase.practice.usecase.EndRoadViewPracticeUseCase;
import com.kospot.game.application.usecase.practice.usecase.StartRoadViewPracticeUseCase;

import com.kospot.common.annotation.adsense.BotSuccess;
import com.kospot.common.security.aop.CurrentMember;
import com.kospot.common.security.aop.CurrentMemberOrNull;
import com.kospot.game.application.usecase.rank.StartRoadViewRankUseCase;
import com.kospot.game.presentation.dto.request.EndGameRequest;
import com.kospot.game.presentation.dto.response.EndGameResponse;
import com.kospot.game.presentation.dto.response.RoadViewGameHistoryResponse;
import com.kospot.game.presentation.dto.response.StartGameResponse;
import com.kospot.game.application.service.AESService;
import com.kospot.game.common.utils.ScoreCalculator;
import com.kospot.common.exception.payload.dto.ApiResponseDto;
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

    @Operation(summary = "로드뷰 연습 게임 시작",
               description = "로드뷰 연습 게임을 시작합니다. 비로그인 사용자는 응답의 practiceToken을 저장하여 end/reissue 요청에 사용해야 합니다.")
    @BotSuccess
    @PostMapping("/practice/start")
    public ApiResponseDto<StartGameResponse.RoadView> startPracticeGame(@CurrentMemberOrNull Long memberId, @RequestParam("sido") String sidoKey) {
        return ApiResponseDto.onSuccess(startRoadViewPracticeUseCase.execute(memberId, sidoKey));
    }

    @Operation(summary = "로드뷰 연습 게임 종료",
               description = "로드뷰 연습 게임을 종료합니다. 비로그인 사용자는 X-Practice-Token 헤더에 시작 시 발급된 토큰을 포함해야 합니다.")
    @PostMapping("/practice/end")
    public ApiResponseDto<EndGameResponse.RoadViewPractice> endPracticeGame(
            @CurrentMemberOrNull Long memberId,
            @RequestHeader(value = "X-Practice-Token", required = false) String practiceToken,
            @RequestBody EndGameRequest.RoadView request) {
        return ApiResponseDto.onSuccess(endRoadViewPracticeUseCase.execute(memberId, request, practiceToken));
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
    public ApiResponseDto<StartGameResponse.RoadView> startRankGame(@CurrentMember Long memberId) {
        return ApiResponseDto.onSuccess(startRoadViewRankUseCase.execute(memberId));
    }

    @Operation(summary = "로드뷰 랭크 게임 종료", description = "로드뷰 랭크 게임을 종료합니다.")
    @PostMapping("/rank/end")
    public ApiResponseDto<EndGameResponse.RoadViewRank> endRankGame(@CurrentMember Long memberId, @RequestBody EndGameRequest.RoadView request) {
        return ApiResponseDto.onSuccess(endRoadViewRankUseCase.execute(memberId, request));
    }

    /**
     *  ------------------------------------------
     */

    @Operation(summary = "로드뷰 좌표 재발급", description = "로드뷰 연습 게임에서 좌표를 재발급합니다. 비로그인 사용자는 X-Practice-Token 헤더에 시작 시 발급된 토큰을 포함해야 합니다.")
    @PostMapping("/{gameId}/reissue-coordinate")
    public ApiResponseDto<StartGameResponse.ReIssue> reissuePracticeCoordinate(
            @CurrentMemberOrNull Long memberId,
            @RequestHeader(value = "X-Practice-Token", required = false) String practiceToken,
            @PathVariable("gameId") Long gameId) {
        return ApiResponseDto.onSuccess(reIssueRoadViewCoordinateUseCase.execute(memberId, gameId, practiceToken));
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
    public ApiResponseDto<RoadViewGameHistoryResponse.RecentThree> getRecentThreeGames(@CurrentMember Long memberId) {
        return ApiResponseDto.onSuccess(getRecentThreeRoadViewGamesUseCase.execute(memberId));
    }

    @Operation(summary = "로드뷰 게임 전체 기록 조회", description = "로드뷰 게임의 전체 완료된 기록을 페이지네이션으로 조회합니다.")
    @GetMapping("/history")
    public ApiResponseDto<RoadViewGameHistoryResponse.All> getAllGames(
            @CurrentMember Long memberId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponseDto.onSuccess(getAllRoadViewGamesUseCase.execute(memberId, pageable));
    }

    /**
     *  ------------------------------------------
     */

}

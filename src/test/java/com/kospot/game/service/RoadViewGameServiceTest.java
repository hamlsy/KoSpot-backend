package com.kospot.game.service;

import com.kospot.application.game.roadView.practice.usecase.EndRoadViewPracticeUseCase;
import com.kospot.application.game.roadView.rank.usecase.EndRoadViewRankUseCase;
import com.kospot.presentation.game.dto.request.EndGameRequest;
import com.kospot.presentation.game.dto.response.EndGameResponse;
import com.kospot.domain.game.entity.GameType;
import com.kospot.domain.game.entity.GameStatus;
import com.kospot.domain.game.entity.GameMode;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.repository.RoadViewGameRepository;
import com.kospot.domain.game.service.RoadViewGameService;
import com.kospot.domain.gameRank.entity.GameRank;
import com.kospot.domain.gameRank.entity.RankTier;
import com.kospot.domain.gameRank.repository.GameRankRepository;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.point.service.PointHistoryService;
import com.kospot.domain.point.service.PointService;
import com.kospot.domain.point.util.PointCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RoadViewGameServiceTest {
    private static final Logger log = LoggerFactory.getLogger(RoadViewGameServiceTest.class);

    @Autowired
    private EndRoadViewRankUseCase endRoadViewRankUseCase;

    @Autowired
    private EndRoadViewPracticeUseCase endRoadViewPracticeUseCase;

    @Autowired
    private RoadViewGameService roadViewGameService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RoadViewGameRepository roadViewGameRepository;

    @Autowired
    private GameRankRepository gameRankRepository;

    @Autowired
    private PointService pointService;

    @Autowired
    private PointHistoryService pointHistoryService;

    @Test
    @DisplayName("연습 게임 종료 테스트")
    void end_practice_game_test() {
        //given
        Member member = memberRepository.save(Member.builder()
                .username("mem1")
                .nickname("nick1")
                .build());
        RoadViewGame game = roadViewGameRepository.save(RoadViewGame.builder()
                .gameMode(GameMode.ROADVIEW)
                .member(member)
                .gameType(GameType.PRACTICE).build());

        EndGameRequest.RoadView request = EndGameRequest.RoadView.builder()
                .gameId(game.getId())
                .answerDistance(200)
                .build();

        // when
        EndGameResponse.RoadViewPractice response = endRoadViewPracticeUseCase.execute(member, request);

        // then
        int expectedPoint = PointCalculator.getPracticePoint(game.getScore());
        int actualPoint = member.getPoint();
        assertEquals(expectedPoint, actualPoint);
        System.out.println("expectedPoint: " + expectedPoint + " actualPoint: " + actualPoint);
        assertEquals(game.getScore(), response.getScore());
        System.out.println("game.getScore(): " + game.getScore() + " response.getScore(): " + response.getScore());
        assertEquals(GameStatus.COMPLETED, game.getGameStatus());
        System.out.println("game.getGameStatus(): " + game.getGameStatus());
    }

    @Test
    @DisplayName("랭크 게임 종료 테스트")
    void end_rank_game_test() {
        //given
        Member member = memberRepository.save(Member.builder()
                .username("mem1")
                .nickname("nick1")
                .build());
        RoadViewGame game = roadViewGameRepository.save(RoadViewGame.builder()
                .gameMode(GameMode.ROADVIEW)
                .member(member)
                .gameType(GameType.RANK)
                .gameStatus(GameStatus.ABANDONED)
                .build());

        GameRank gameRank = gameRankRepository.save(GameRank.builder()
                .member(member)
                .gameMode(GameMode.ROADVIEW)
                .rankTier(RankTier.SILVER)
                .build());

        EndGameRequest.RoadView request = EndGameRequest.RoadView.builder()
                .gameId(game.getId())
                .answerDistance(200)
                .build();

        // when
        EndGameResponse.RoadViewRank response = endRoadViewRankUseCase.execute(member, request);

        // then
        int expectedPoint = PointCalculator.getRankPoint(gameRank.getRankTier(), game.getScore());
        int actualPoint = member.getPoint();

        System.out.println("expectedPoint: " + expectedPoint + " actualPoint: " + actualPoint);
        assertEquals(game.getScore(), response.getScore());
        System.out.println("game.getScore(): " + game.getScore() + " response.getScore(): " + response.getScore());
        assertEquals(GameStatus.COMPLETED, game.getGameStatus());
        System.out.println("game.getGameStatus(): " + game.getGameStatus());
    }

}

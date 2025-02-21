package com.kospot.kospot.game.service;

import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.entity.GameMode;
import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.repository.RoadViewGameRepository;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.gameRank.entity.RankTier;
import com.kospot.kospot.domain.gameRank.repository.GameRankRepository;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.member.repository.MemberRepository;
import com.kospot.kospot.domain.point.service.PointHistoryService;
import com.kospot.kospot.domain.point.service.PointService;
import com.kospot.kospot.domain.point.util.PointCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RoadViewGameServiceTest {
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
    void end_practice_game_test(){
        //given
        Member member = memberRepository.save(Member.builder()
                        .username("mem1")
                        .nickname("nick1")
                .build());
        RoadViewGame game = roadViewGameRepository.save(RoadViewGame.builder()
                        .gameType(GameType.ROADVIEW)
                        .member(member)
                        .gameMode(GameMode.PRACTICE).build());

        GameRank gameRank = gameRankRepository.save(GameRank.builder()
                        .member(member)
                        .gameType(GameType.ROADVIEW)
                        .rankTier(RankTier.BRONZE)
                .build());

        EndGameRequest.RoadView request = EndGameRequest.RoadView.builder()
                .gameId(game.getId())
                .answerDistance(200)
                .build();

        // when
        EndGameResponse.RoadViewRank response = roadViewGameService.endRankGame(member, request);

        // then
        int expectedPoint = PointCalculator.getRankPoint(gameRank.getRankTier(), game.getScore());
        int actualPoint = member.getPoint();
        assertEquals(expectedPoint, actualPoint);
        assertEquals(game.getScore(), response.getScore());

    }


}

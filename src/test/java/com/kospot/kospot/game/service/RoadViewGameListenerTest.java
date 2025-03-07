package com.kospot.kospot.game.service;

import com.kospot.kospot.application.game.roadView.rank.usecase.EndRoadViewRankUseCase;
import com.kospot.kospot.application.game.roadView.rank.usecase.EndRoadViewRankUseCaseV2;
import com.kospot.kospot.application.game.roadView.rank.listener.EndRoadViewRankEventListener;
import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.repository.RoadViewGameRepository;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.gameRank.repository.GameRankRepository;
import com.kospot.kospot.domain.gameRank.util.RatingScoreCalculator;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.member.repository.MemberRepository;
import com.kospot.kospot.domain.point.adaptor.PointHistoryAdaptor;
import com.kospot.kospot.domain.point.entity.PointHistory;
import com.kospot.kospot.domain.point.repository.PointHistoryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.ArrayList;
import java.util.List;


import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@RecordApplicationEvents
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RoadViewGameListenerTest {

    @Autowired
    private EndRoadViewRankUseCase endRoadViewRankUseCase;

    @Autowired
    private EndRoadViewRankUseCaseV2 endRoadViewRankUseCaseV2;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRankRepository gameRankRepository;

    @Autowired
    private RoadViewGameService gameService;

    @Autowired
    private RoadViewGameRepository roadViewGameRepository;

    @Autowired
    private PointHistoryAdaptor pointHistoryAdaptor;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private EndRoadViewRankEventListener endRoadViewRankEventListener;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private EntityManager em;

    private List<Member> members = new ArrayList<>();
    private List<GameRank> gameRanks = new ArrayList<>();
    private List<RoadViewGame> games = new ArrayList<>();
    private List<EndGameRequest.RoadView> requests = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // given
        for (int i = 0; i < 100; i++) {
            Member member = Member.builder()
                    .nickname("nickname" + i)
                    .username("username" + i)
                    .build();
            memberRepository.save(member);
            members.add(member);
            GameRank gameRank = GameRank.create(member, GameType.ROADVIEW);
            gameRankRepository.save(gameRank);
            gameRanks.add(gameRank);
            RoadViewGame game = roadViewGameRepository.save(
                    RoadViewGame.builder()
                            .member(member)
                            .build()
            );
            games.add(game);
            requests.add(
                    EndGameRequest.RoadView.builder()
                            .gameId(game.getId())
                            .answerDistance((int) (Math.random() * 301))
                            .build()
            );
        }

    }

    @Test
    @DisplayName("기존 게임 종료 로직 시간 측정")
    void testGameEnd() {
        //given
        long startTime = System.currentTimeMillis();

        //when
        for (int i = 0; i < 100; i++) {
            Member member = members.get(i);
            endRoadViewRankUseCase.execute(member, requests.get(i));

        }
        // then
        long endTime = System.currentTimeMillis();
        System.out.println("기존 게임 종료 로직 시간: " + (endTime - startTime) + "ms");

        //persist
        Member member = memberRepository.findById(members.get(0).getId()).orElseThrow();

        int countPointHistory = (int) pointHistoryRepository.count();
        assertEquals(100, countPointHistory);

        int memberPoint = member.getPoint();
        assertNotEquals(0, memberPoint);

        int ratingScore = gameRankRepository.findByMemberAndGameType(member, GameType.ROADVIEW).getRatingScore();
        assertNotEquals(0, ratingScore);
    }

    @Test
    @DisplayName("게임 종료 이벤트 리스너 시간 측정")
    void testGameEndEventListener() throws InterruptedException {
        //given
        long startTime = System.currentTimeMillis();

        //when
        for (int i = 0; i < 100; i++) {
            Member member = members.get(i);
            endRoadViewRankUseCaseV2.execute(member, requests.get(i));
        }

        //then
        long endTime = System.currentTimeMillis();
        System.out.println("게임 종료 이벤트 리스너 시간: " + (endTime - startTime) + "ms");

        Thread.sleep(1000);

        //persist
        Member member = memberRepository.findById(members.get(0).getId()).orElseThrow();

        int countPointHistory = (int) pointHistoryRepository.count();
        assertEquals(100, countPointHistory);

        int memberPoint = member.getPoint();
        assertNotEquals(0, memberPoint);

        int ratingScore = gameRankRepository.findByMemberAndGameType(member, GameType.ROADVIEW).getRatingScore();
        assertNotEquals(0, ratingScore);

    }
}

package com.kospot.kospot.game.service;

import com.kospot.kospot.application.game.roadView.rank.EndRoadViewRankUseCase;
import com.kospot.kospot.application.game.roadView.rank.EndRoadViewRankUseCaseV2;
import com.kospot.kospot.application.game.roadView.rank.listener.EndRoadViewRankEventListener;
import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.event.RoadViewGameEvent;
import com.kospot.kospot.domain.game.repository.RoadViewGameRepository;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.gameRank.repository.GameRankRepository;
import com.kospot.kospot.domain.gameRank.util.RatingScoreCalculator;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.member.repository.MemberRepository;
import com.kospot.kospot.domain.point.adaptor.PointHistoryAdaptor;
import com.kospot.kospot.domain.point.entity.PointHistory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@RecordApplicationEvents
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
        // given
        long startTime = System.currentTimeMillis();

        // when
        List<EndGameResponse.RoadViewRank> responses = new ArrayList<>();

        //when
        for (int i = 0; i < 100; i++) {
            responses.add(endRoadViewRankUseCase.execute(members.get(i), requests.get(i)));

        }

        // then
        long endTime = System.currentTimeMillis();
        System.out.println("기존 게임 종료 로직 시간: " + (endTime - startTime) + "ms");

        for ( int i = 0 ; i < 100; i++) {
            Member member = members.get(i);
            System.out.println("gameId: " + games.get(i).getId());
            System.out.println("gameRanks.get(i).getRatingScore(): " + gameRanks.get(i).getRatingScore());
            System.out.println("responses.get(i).getratingPointChange(): " + responses.get(i).getRatingScoreChange());
            System.out.println("games.get(i).getScore(): " + games.get(i).getScore());
            int ratingScoreChange = RatingScoreCalculator.calculateRatingChange(games.get(i).getScore(), 0);
            System.out.println("RatingScoreCalculator.calculateRatingChange(games.get(i).getScore(), gameRanks.get(i).getRatingScore()): " + ratingScoreChange);
            assertEquals(ratingScoreChange, responses.get(i).getRatingScoreChange());

            //point history
            List<PointHistory> pointHistories = pointHistoryAdaptor.queryAllHistoryByMemberId(member.getId());
            System.out.println("point history change amount: " + pointHistories.get(0).getChangeAmount());
            System.out.println("point history " + pointHistories.get(0).getPointHistoryType());

            //member point
            assertEquals(pointHistories.get(0).getChangeAmount(), member.getPoint());

        }
    }

    @Test
    @DisplayName("게임 종료 이벤트 리스너 시간 측정")
    void testGameEndEventListener() throws InterruptedException {
        //given
        long startTime = System.currentTimeMillis();
        List<EndGameResponse.RoadViewRank> responses = new ArrayList<>();
        //when
        for (int i = 0; i < 100; i++) {
            responses.add(endRoadViewRankUseCaseV2.execute(members.get(i), requests.get(i)));
            eventPublisher.publishEvent(new RoadViewGameEvent(members.get(i), games.get(i), gameRanks.get(i)));
        }

        //then
        long endTime = System.currentTimeMillis();
        System.out.println("게임 종료 이벤트 리스너 시간: " + (endTime - startTime) + "ms");

//        Thread.sleep(5000);

        for ( int i = 0 ; i < 100; i++) {
            Member member = members.get(i);

            System.out.println("gameId: " + games.get(i).getId());
            System.out.println("gameRanks.get(i).getRatingScore(): " + gameRanks.get(i).getRatingScore());
            System.out.println("responses.get(i).getratingPointChange(): " + responses.get(i).getRatingScoreChange());
            System.out.println("games.get(i).getScore(): " + games.get(i).getScore());
            int ratingScoreChange = RatingScoreCalculator.calculateRatingChange(games.get(i).getScore(), 0);
            System.out.println("RatingScoreCalculator.calculateRatingChange(games.get(i).getScore(), gameRanks.get(i).getRatingScore()): " + ratingScoreChange);
            assertEquals(ratingScoreChange, responses.get(i).getRatingScoreChange());

            //point history
//            List<PointHistory> pointHistories = pointHistoryAdaptor.queryAllHistoryByMemberId(member.getId());
//            System.out.println("point history change amount: " + pointHistories.get(0).getChangeAmount());
//            System.out.println("point history " + pointHistories.get(0).getPointHistoryType());
//
//            //member point
//            assertEquals(pointHistories.get(0).getChangeAmount(), member.getPoint());
        }


    }
}

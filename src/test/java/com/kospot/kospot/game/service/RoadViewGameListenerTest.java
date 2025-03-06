package com.kospot.kospot.game.service;

import com.kospot.kospot.application.game.roadView.rank.EndRoadViewRankUseCase;
import com.kospot.kospot.application.game.roadView.rank.EndRoadViewRankUseCaseV2;
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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@RecordApplicationEvents
@Transactional
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
            members.add(member);
            GameRank gameRank = GameRank.create(member, GameType.ROADVIEW);
            gameRankRepository.save(gameRank);
            gameRanks.add(gameRank);
            RoadViewGame game = roadViewGameRepository.save(
                    RoadViewGame.builder()
                            .member(member)
                            .build()
            );
            roadViewGameRepository.save(game);
            games.add(game);
            requests.add(
                    EndGameRequest.RoadView.builder()
                            .gameId(game.getId())
                            .answerDistance((int) (Math.random() * 301))
                            .build()
            );
        }
        memberRepository.saveAll(members);

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
            System.out.println("gameRanks.get(i).getRatingScore(): " + gameRanks.get(i).getRatingScore());
            System.out.println("responses.get(i).getratingPointChange(): " + responses.get(i).getRatingScoreChange());
            assertEquals( gameRanks.get(i).getRatingScore() + 100, responses.get(i).getCurrentRatingPoint());
            assertEquals(RatingScoreCalculator.calculateRatingChange(games.get(i).getScore(), gameRanks.get(i).getRatingScore()), responses.get(i).getRatingScoreChange());
        }
    }

    @Test
    @DisplayName("게임 종료 이벤트 리스너 시간 측정")
    void testGameEndEventListener() {
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

        em.flush();
        em.clear();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // 여기에서 리스너 내부에서 수행된 동작 검증 (예: 서비스 메서드가 실행되었는지 확인)
//            verify(roadViewGameService).endRankGameV2(any(), any(), any());
        });

        for ( int i = 0 ; i < 100; i++) {
            assertEquals( gameRanks.get(i).getRatingScore() + 100, responses.get(i).getCurrentRatingPoint());
            assertEquals(RatingScoreCalculator.calculateRatingChange(games.get(i).getScore(), gameRanks.get(i).getRatingScore()), responses.get(i).getRatingScoreChange());
        }


    }
}

package com.kospot.game.service;

import com.kospot.application.game.roadview.rank.event.UpdatePointAndRankEvent;
import com.kospot.application.game.roadview.rank.usecase.EndRoadViewRankUseCase;
import com.kospot.application.game.roadview.rank.listener.EndRoadViewRankEventListener;
import com.kospot.presentation.game.dto.request.EndGameRequest;
import com.kospot.domain.game.vo.GameStatus;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.repository.RoadViewGameRepository;
import com.kospot.domain.game.service.RoadViewGameService;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.gamerank.vo.RankTier;
import com.kospot.domain.gamerank.repository.GameRankRepository;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.point.adaptor.PointHistoryAdaptor;
import com.kospot.domain.point.repository.PointHistoryRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@Slf4j
@SpringBootTest
@RecordApplicationEvents
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RoadViewGameListenerTest {

    @Autowired
    private EndRoadViewRankUseCase endRoadViewRankUseCase;

    @Autowired
    private EndRoadViewRankUseCase endRoadViewRankUseCaseV2;

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

    @Mock
    private UpdatePointAndRankEvent updatePointAndRankEvent;

    private List<Member> members = new ArrayList<>();
    private List<GameRank> gameRanks = new ArrayList<>();
    private List<RoadViewGame> games = new ArrayList<>();
    private List<EndGameRequest.RoadView> requests = new ArrayList<>();

    private static int TEST_SIZE = 150;

    @BeforeEach
    void setUp() {
        // given
        for (int i = 0; i < TEST_SIZE; i++) {
            Member member = Member.builder()
                    .nickname("nickname" + i)
                    .username("username" + i)
                    .build();
            memberRepository.save(member);
            members.add(member);
            GameRank gameRank = GameRank.create(member, GameMode.ROADVIEW);
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
                            .build()
            );
        }

    }

    @Test
    @DisplayName("기존 게임 종료 로직 시간 측정")
    void testGameEnd() {
        //given
        long startTime = System.currentTimeMillis();

        // when - CompletableFuture로 병렬 실행
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < TEST_SIZE; i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                Member member = members.get(index);
                endRoadViewRankUseCaseV2.execute(member, requests.get(index));
            });
            futures.add(future);
        }

        // 모든 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long endTime = System.currentTimeMillis();

        //persist
        Member member = memberRepository.findById(members.get(0).getId()).orElseThrow();

        int countPointHistory = (int) pointHistoryRepository.count();
        assertEquals(TEST_SIZE, countPointHistory);

        int memberPoint = member.getPoint();
        assertNotEquals(0, memberPoint);

        int ratingScore = gameRankRepository.findByMemberAndGameMode(member, GameMode.ROADVIEW).getRatingScore();
        assertNotEquals(0, ratingScore);

        log.info("기존 게임 종료 로직 시간: {}ms", (endTime - startTime));
    }

    @Test
    @DisplayName("게임 종료 이벤트 리스너 시간 측정")
    void testGameEndEventListener() throws InterruptedException {
        //given
        long startTime = System.currentTimeMillis();

        // when - CompletableFuture로 병렬 실행
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < TEST_SIZE; i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                Member member = members.get(index);
                endRoadViewRankUseCaseV2.execute(member, requests.get(index));
            });
            futures.add(future);
        }
        long endTime = System.currentTimeMillis();

        // 모든 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        //persist
        Member member = memberRepository.findById(members.get(0).getId()).orElseThrow();

        int countPointHistory = (int) pointHistoryRepository.count();
        assertEquals(TEST_SIZE, countPointHistory);

        int memberPoint = member.getPoint();
        assertNotEquals(0, memberPoint);

        int ratingScore = gameRankRepository.findByMemberAndGameMode(member, GameMode.ROADVIEW).getRatingScore();
        assertNotEquals(0, ratingScore);

        log.info("기존 게임 종료 로직 시간: {}ms", (endTime - startTime));
    }

    @Test
    @DisplayName("게임 종료 이벤트 리스너 롤백 테스트")
    void testGameEndEventListenerRollback() throws InterruptedException {
        //given
        Member member = members.get(0);
        EndGameRequest.RoadView request = requests.get(0);

        // when
        doThrow(new RuntimeException("이벤트 리스너 강제 예외"))
                .when(updatePointAndRankEvent)
                .updatePointAndRank(any(Member.class), any(RoadViewGame.class), any(RankTier.class));

        endRoadViewRankUseCaseV2.execute(member, request);

        Thread.sleep(1000);
        //then
        RoadViewGame retrievedGame = roadViewGameRepository.findById(member.getId()).orElseThrow();
        GameRank gameRank = gameRankRepository.findByMemberAndGameMode(member, GameMode.ROADVIEW);
        assertEquals(GameStatus.COMPLETED, retrievedGame.getGameStatus());
        Member persistMember = gameRank.getMember();
        assertEquals(0, persistMember.getPoint());
        assertNotEquals(0, gameRank.getRatingScore());
        log.info("game status :" + retrievedGame.getGameStatus());
        log.info("member point :" + persistMember.getPoint());
        log.info("member ratingpoint :" + gameRank.getRatingScore());
        log.info("게임 종료 이벤트 리스너 롤백 테스트 성공");
    }
}

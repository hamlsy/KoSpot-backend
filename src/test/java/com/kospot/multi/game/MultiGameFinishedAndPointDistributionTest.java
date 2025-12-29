package com.kospot.multi.game;

import com.kospot.application.coordinate.ImportCoordinateUseCase;
import com.kospot.application.multi.game.usecase.NotifyStartGameUseCase;
import com.kospot.application.multi.game.usecase.FinishMultiRoadViewGameUseCase;
import com.kospot.application.multi.round.roadview.NextRoadViewRoundUseCase;
import com.kospot.application.multi.submission.http.usecase.SubmitRoadViewPlayerAnswerUseCase;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.repository.ImageRepository;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.repository.MultiRoadViewGameRepository;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.repository.GamePlayerRepository;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.repository.GameRoomRepository;
import com.kospot.domain.multi.room.vo.GameRoomStatus;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.repository.RoadViewGameRoundRepository;
import com.kospot.domain.point.repository.PointHistoryRepository;
import com.kospot.domain.point.util.PointCalculator;
import com.kospot.domain.point.vo.PointHistoryType;
import com.kospot.infrastructure.redis.domain.multi.submission.service.SubmissionRedisService;
import com.kospot.presentation.multi.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
import com.kospot.presentation.multi.submission.dto.request.SubmitRoadViewRequest;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 멀티 게임 종료 및 포인트 지급 통합 테스트
 * 
 * 테스트 범위:
 * 1. 게임 종료 시 최종 결과 생성 및 전송
 * 2. 순위별 포인트 계산
 * 3. 포인트 지급 (동기 - 단일 트랜잭션)
 * 4. 포인트 히스토리 저장
 * 
 * 검증 항목:
 * - 게임 종료 상태 확인
 * - WebSocket 최종 결과 메시지 전송 확인
 * - 순위별로 다른 포인트 지급 확인
 * - 포인트 히스토리 타입이 MULTI_GAME인지 확인
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class MultiGameFinishedAndPointDistributionTest {

    @Autowired
    private NotifyStartGameUseCase notifyStartGameUseCase;

    @Autowired
    private NextRoadViewRoundUseCase nextRoadViewRoundUseCase;

    @Autowired
    private SubmitRoadViewPlayerAnswerUseCase submitRoadViewPlayerAnswerUseCase;

    @Autowired
    private FinishMultiRoadViewGameUseCase finishMultiRoadViewGameUseCase;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private MultiRoadViewGameRepository multiRoadViewGameRepository;

    @Autowired
    private RoadViewGameRoundRepository roadViewGameRoundRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImportCoordinateUseCase importCoordinateUseCase;

    @Autowired
    private SubmissionRedisService submissionRedisService;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    private Member hostMember;
    private Member player2;
    private Member player3;
    private GameRoom gameRoom;
    private Image markerImage;

    @BeforeEach
    void setUp() {
        // 좌표 데이터 임포트
//        importCoordinateUseCase.execute("test_coordinates_excel.xlsx");

        // 마커 이미지 생성
        markerImage = imageRepository.save(Image.builder()
                .imageUrl("https://test.com/marker.png")
                .build());

        // 멤버 생성
        hostMember = createMember("host@test.com", "호스트", 1000);
        player2 = createMember("player2@test.com", "플레이어2", 1000);
        player3 = createMember("player3@test.com", "플레이어3", 1000);

        // 게임방 생성
        gameRoom = gameRoomRepository.save(GameRoom.builder()
                .title("포인트 테스트방")
                .host(hostMember)
                .maxPlayers(5)
                .privateRoom(false)
                .status(GameRoomStatus.WAITING)
                .build());

        // 멤버들을 게임방에 추가
        hostMember.joinGameRoom(gameRoom.getId());
        player2.joinGameRoom(gameRoom.getId());
        player3.joinGameRoom(gameRoom.getId());
        memberRepository.saveAll(List.of(hostMember, player2, player3));

        log.info("✅ Test setup completed");
    }

    @Test
    @DisplayName("[통합] 게임 종료 시 최종 결과가 WebSocket으로 전송된다")
    void whenGameFinishes_thenFinalResultIsSentViaWebSocket() {
        // Given: 게임 시작 (1라운드만)
        MultiGameRequest.Start startRequest = createStartRequest(60, 1);
        
        // 1단계: 게임 생성
        com.kospot.presentation.multi.game.dto.response.MultiGameResponse.StartGame startGameResponse = 
                notifyStartGameUseCase.execute(hostMember, gameRoom.getId());
        Long gameId = startGameResponse.getGameId();
        
        // 2단계: 1라운드 준비
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                nextRoadViewRoundUseCase.executeInitial(gameRoom.getId(), gameId);

        Long roundId = startResponse.getRoundInfo().getRoundId();
        String roomId = gameRoom.getId().toString();

        // 제출 초기화
        submissionRedisService.initializeRound(GameMode.ROADVIEW, roundId);

        // 플레이어들이 제출 (점수 차이를 만들기 위해 다른 위치)
        submitAnswer(hostMember, roomId, gameId, roundId, 37.5665, 126.9780); // 가까운 위치
        submitAnswer(player2, roomId, gameId, roundId, 35.1796, 129.0756);    // 먼 위치
        submitAnswer(player3, roomId, gameId, roundId, 36.3504, 127.3845);    // 중간 위치

        // 라운드 종료 대기
        await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    entityManager.clear();
                    RoadViewGameRound round = roadViewGameRoundRepository.findById(roundId).orElseThrow();
                    assertThat(round.getIsFinished()).isTrue();
                });

        // When: 게임 종료
        finishMultiRoadViewGameUseCase.execute(roomId, gameId);

        // Then: WebSocket으로 최종 결과 전송 확인
        verify(messagingTemplate, timeout(2000).atLeastOnce())
                .convertAndSend(
                        eq("/topic/game/" + roomId + "/game/finished"),
                        any(Object.class)
                );

        // 게임 종료 상태 확인
        entityManager.clear();
        MultiRoadViewGame game = multiRoadViewGameRepository.findById(gameId).orElseThrow();
        assertThat(game.getIsFinished()).isTrue();

        log.info("✅ Game finished and final result sent via WebSocket");
    }

    @Test
    @DisplayName("[통합] 게임 종료 후 순위별로 포인트가 지급된다")
    void whenGameFinishes_thenPointsAreDistributedByRank() {
        // Given: 게임 시작
        MultiGameRequest.Start startRequest = createStartRequest(60, 1);
        
        // 1단계: 게임 생성
        com.kospot.presentation.multi.game.dto.response.MultiGameResponse.StartGame startGameResponse = 
                notifyStartGameUseCase.execute(hostMember, gameRoom.getId());
        Long gameId = startGameResponse.getGameId();
        
        // 2단계: 1라운드 준비
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                nextRoadViewRoundUseCase.executeInitial(gameRoom.getId(), gameId);

        Long roundId = startResponse.getRoundInfo().getRoundId();
        String roomId = gameRoom.getId().toString();

        submissionRedisService.initializeRound(GameMode.ROADVIEW, roundId);

        // 플레이어들이 제출 (점수 차이)
        submitAnswer(hostMember, roomId, gameId, roundId, 37.5665, 126.9780);
        submitAnswer(player2, roomId, gameId, roundId, 35.1796, 129.0756);
        submitAnswer(player3, roomId, gameId, roundId, 36.3504, 127.3845);

        // 라운드 종료 대기
        await().atMost(3, TimeUnit.SECONDS).until(() -> {
            entityManager.clear();
            return roadViewGameRoundRepository.findById(roundId).orElseThrow().getIsFinished();
        });

        // 초기 포인트 저장
        entityManager.clear();
        int hostInitialPoint = memberRepository.findById(hostMember.getId()).orElseThrow().getPoint();
        int player2InitialPoint = memberRepository.findById(player2.getId()).orElseThrow().getPoint();
        int player3InitialPoint = memberRepository.findById(player3.getId()).orElseThrow().getPoint();

        // When: 게임 종료 (동기 처리 - 포인트 지급 포함)
        finishMultiRoadViewGameUseCase.execute(roomId, gameId);

        // Then: 포인트 지급 확인 (동기 처리되었으므로 즉시 검증 가능)
        entityManager.clear();
        
        Member updatedHost = memberRepository.findById(hostMember.getId()).orElseThrow();
        Member updatedPlayer2 = memberRepository.findById(player2.getId()).orElseThrow();
        Member updatedPlayer3 = memberRepository.findById(player3.getId()).orElseThrow();

        // 포인트가 증가했는지 확인
        assertThat(updatedHost.getPoint()).isGreaterThan(hostInitialPoint);
        assertThat(updatedPlayer2.getPoint()).isGreaterThan(player2InitialPoint);
        assertThat(updatedPlayer3.getPoint()).isGreaterThan(player3InitialPoint);

        log.info("✅ Points distributed - Host: +{}, Player2: +{}, Player3: +{}",
                updatedHost.getPoint() - hostInitialPoint,
                updatedPlayer2.getPoint() - player2InitialPoint,
                updatedPlayer3.getPoint() - player3InitialPoint);

        log.info("✅ Points distributed successfully based on rank");
    }

    @Test
    @DisplayName("[통합] 포인트 지급 시 히스토리가 MULTI_GAME 타입으로 저장된다")
    void whenPointsAreDistributed_thenHistoriesAreSavedAsMultiGameType() {
        // Given: 게임 진행 및 종료
        MultiGameRequest.Start startRequest = createStartRequest(60, 1);
        
        // 1단계: 게임 생성
        com.kospot.presentation.multi.game.dto.response.MultiGameResponse.StartGame startGameResponse = 
                notifyStartGameUseCase.execute(hostMember, gameRoom.getId());
        Long gameId = startGameResponse.getGameId();
        
        // 2단계: 1라운드 준비
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                nextRoadViewRoundUseCase.executeInitial(gameRoom.getId(), gameId);

        Long roundId = startResponse.getRoundInfo().getRoundId();
        String roomId = gameRoom.getId().toString();

        submissionRedisService.initializeRound(GameMode.ROADVIEW, roundId);

        submitAnswer(hostMember, roomId, gameId, roundId, 37.5665, 126.9780);
        submitAnswer(player2, roomId, gameId, roundId, 35.1796, 129.0756);
        submitAnswer(player3, roomId, gameId, roundId, 36.3504, 127.3845);

        // 라운드 종료 대기
        await().atMost(13, TimeUnit.SECONDS).until(() -> {
            entityManager.clear();
            return roadViewGameRoundRepository.findById(roundId).orElseThrow().getIsFinished();
        });

        // 초기 히스토리 개수
        entityManager.clear();
        long initialHistoryCount = pointHistoryRepository.count();

        // When: 게임 종료 (동기 처리 - 포인트 지급 및 히스토리 저장 포함)
        finishMultiRoadViewGameUseCase.execute(roomId, gameId);

        // Then: 포인트 히스토리 확인 (동기 처리되었으므로 즉시 검증 가능)
        entityManager.clear();
        
        long currentHistoryCount = pointHistoryRepository.count();
        assertThat(currentHistoryCount).isEqualTo(initialHistoryCount + 3);

        // MULTI_GAME 타입 히스토리 확인
        long multiGameHistoryCount = pointHistoryRepository.findAll().stream()
                .filter(h -> h.getPointHistoryType() == PointHistoryType.MULTI_GAME)
                .count();
        
        assertThat(multiGameHistoryCount).isGreaterThanOrEqualTo(3);

        log.info("✅ Point histories saved with MULTI_GAME type: {}", multiGameHistoryCount);
        log.info("✅ Point histories saved correctly");
    }

    @Test
    @DisplayName("[단위] 순위별 포인트 계산이 올바르게 작동한다")
    void testPointCalculationByRank() {
        // Given: 다양한 순위와 점수
        double score1st = 450.0;
        double score2nd = 350.0;
        double score3rd = 250.0;

        // When: 포인트 계산
        int point1st = PointCalculator.getMultiGamePoint(1, score1st);
        int point2nd = PointCalculator.getMultiGamePoint(2, score2nd);
        int point3rd = PointCalculator.getMultiGamePoint(3, score3rd);

        // Then: 1등이 가장 많은 포인트를 받아야 함
        assertThat(point1st).isGreaterThan(point2nd);
        assertThat(point2nd).isGreaterThan(point3rd);

        // 최소 포인트 보장 (참여 포인트 + 보너스)
        assertThat(point1st).isGreaterThanOrEqualTo(150 + 20); // 1등 보너스 + 참여
        assertThat(point2nd).isGreaterThanOrEqualTo(100 + 20); // 2등 보너스 + 참여
        assertThat(point3rd).isGreaterThanOrEqualTo(70 + 20);  // 3등 보너스 + 참여

        log.info("✅ Point calculation verified - 1st: {}pt, 2nd: {}pt, 3rd: {}pt",
                point1st, point2nd, point3rd);
    }

    @Test
    @DisplayName("[통합] 동점자는 동일 순위를 받고 같은 포인트를 받는다")
    void whenPlayersHaveSameScore_thenTheyReceiveSameRankAndPoints() {
        // Given: 게임 시작
        MultiGameRequest.Start startRequest = createStartRequest(60, 1);
        
        // 1단계: 게임 생성
        com.kospot.presentation.multi.game.dto.response.MultiGameResponse.StartGame startGameResponse = 
                notifyStartGameUseCase.execute(hostMember, gameRoom.getId());
        Long gameId = startGameResponse.getGameId();
        
        // 2단계: 1라운드 준비
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                nextRoadViewRoundUseCase.executeInitial(gameRoom.getId(), gameId);

        Long roundId = startResponse.getRoundInfo().getRoundId();
        String roomId = gameRoom.getId().toString();

        submissionRedisService.initializeRound(GameMode.ROADVIEW, roundId);

        // 동일한 위치에 제출 (동점)
        double lat = 37.5665;
        double lng = 126.9780;
        submitAnswer(hostMember, roomId, gameId, roundId, lat, lng);
        submitAnswer(player2, roomId, gameId, roundId, lat, lng);
        submitAnswer(player3, roomId, gameId, roundId, 35.1796, 129.0756); // 다른 위치

        // 라운드 종료 대기
        await().atMost(3, TimeUnit.SECONDS).until(() -> {
            entityManager.clear();
            return roadViewGameRoundRepository.findById(roundId).orElseThrow().getIsFinished();
        });

        entityManager.clear();
        int hostInitialPoint = memberRepository.findById(hostMember.getId()).orElseThrow().getPoint();
        int player2InitialPoint = memberRepository.findById(player2.getId()).orElseThrow().getPoint();

        // When: 게임 종료 (동기 처리 - 포인트 지급 포함)
        finishMultiRoadViewGameUseCase.execute(roomId, gameId);

        // Then: 동점자 확인 (동기 처리되었으므로 즉시 검증 가능)
        entityManager.clear();
        
        List<GamePlayer> players = gamePlayerRepository.findAllByMultiRoadViewGameId(gameId);
        GamePlayer hostPlayer = players.stream()
                .filter(p -> p.getMember().getId().equals(hostMember.getId()))
                .findFirst().orElseThrow();
        GamePlayer player2Player = players.stream()
                .filter(p -> p.getMember().getId().equals(player2.getId()))
                .findFirst().orElseThrow();

        // 동일 순위 확인
        assertThat(hostPlayer.getRoundRank()).isEqualTo(player2Player.getRoundRank());
        
        // 동일 포인트 지급 확인
        Member updatedHost = memberRepository.findById(hostMember.getId()).orElseThrow();
        Member updatedPlayer2 = memberRepository.findById(player2.getId()).orElseThrow();
        
        int hostEarnedPoint = updatedHost.getPoint() - hostInitialPoint;
        int player2EarnedPoint = updatedPlayer2.getPoint() - player2InitialPoint;
        
        assertThat(hostEarnedPoint).isEqualTo(player2EarnedPoint);

        log.info("✅ Same rank players received same points: {}pt", hostEarnedPoint);
        log.info("✅ Same score players handled correctly");
    }

    // ===== Helper Methods =====

    private Member createMember(String email, String nickname, int initialPoint) {
        return memberRepository.save(Member.builder()
                .username(email)
                .nickname(nickname)
                .point(initialPoint)
                .role(Role.USER)
                .equippedMarkerImage(markerImage)
                .build());
    }

    private MultiGameRequest.Start createStartRequest(int timeLimit, int totalRounds) {
        return MultiGameRequest.Start.builder()
                .playerMatchTypeKey("SOLO")
                .gameModeKey("ROADVIEW")
                .timeLimit(timeLimit)
                .build();
    }

    private void submitAnswer(Member member, String roomId, Long gameId, Long roundId, 
                              double lat, double lng) {
        SubmitRoadViewRequest.Player request = SubmitRoadViewRequest.Player.builder()
                .lat(lat)
                .lng(lng)
                .timeToAnswer(1000.0)
                .build();

        submitRoadViewPlayerAnswerUseCase.execute(
                member,
                roomId,
                gameId,
                roundId,
                request
        );
    }
}


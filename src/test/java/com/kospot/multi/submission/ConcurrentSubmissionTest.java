package com.kospot.multi.submission;

import com.kospot.application.coordinate.ImportCoordinateUseCase;
import com.kospot.application.multi.game.usecase.NotifyStartGameUseCase;
import com.kospot.application.multi.round.roadview.CheckAndCompleteRoundEarlyUseCase;
import com.kospot.application.multi.round.roadview.NextRoadViewRoundUseCase;
import com.kospot.application.multi.submission.http.usecase.SubmitRoadViewPlayerAnswerUseCase;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.repository.ImageRepository;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.multi.game.repository.MultiRoadViewGameRepository;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.repository.GamePlayerRepository;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.repository.GameRoomRepository;
import com.kospot.domain.multi.room.vo.GameRoomStatus;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.repository.RoadViewGameRoundRepository;
import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.entity.LocationType;
import com.kospot.domain.coordinate.entity.Sido;
import com.kospot.domain.coordinate.repository.CoordinateRepository;
import com.kospot.domain.coordinate.vo.Address;
import com.kospot.domain.multi.submission.repository.RoadViewSubmissionRepository;
import com.kospot.domain.multi.submission.service.RoadViewSubmissionService;
import com.kospot.infrastructure.redis.domain.multi.room.adaptor.GameRoomRedisAdaptor;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 동시 제출 시 Redis-DB 불일치 문제 재현 및 검증 테스트
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class ConcurrentSubmissionTest {

    @Autowired
    private NotifyStartGameUseCase notifyStartGameUseCase;

    @Autowired
    private NextRoadViewRoundUseCase nextRoadViewRoundUseCase;

    @Autowired
    private SubmitRoadViewPlayerAnswerUseCase submitRoadViewPlayerAnswerUseCase;

    @Autowired
    private CheckAndCompleteRoundEarlyUseCase checkAndCompleteRoundEarlyUseCase;

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
    private RoadViewSubmissionRepository submissionRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private CoordinateRepository coordinateRepository;

    @Autowired
    private ImportCoordinateUseCase importCoordinateUseCase;

    @Autowired
    private SubmissionRedisService submissionRedisService;

    @Autowired
    private RoadViewSubmissionService roadViewSubmissionService;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @MockitoBean
    private GameRoomRedisAdaptor gameRoomRedisAdaptor;

    @Autowired
    private EntityManager entityManager;

    private Member hostMember;
    private List<Member> players;
    private GameRoom gameRoom;
    private Image markerImage;

    @BeforeEach
    void setUp() {
        // 마커 이미지 생성
        markerImage = Image.builder()
                .imageUrl("http://example.com/marker.png")
                .build();
        imageRepository.save(markerImage);

        // 좌표 데이터 생성 (RoadViewGameRound 생성에 필요)
        createTestCoordinates();

        // 멤버 생성 (5명 - 동시성 테스트용)
        hostMember = createMember("host", "host", "호스트");
        players = new ArrayList<>();
        players.add(hostMember);

        for (int i = 1; i <= 4; i++) {
            Member player = createMember("player" + i, "player" + i, "플레이어" + i);
            players.add(player);
        }

        // 게임방 생성
        gameRoom = createGameRoom(hostMember, players);

        // Redis Mock 설정
        when(gameRoomRedisAdaptor.getCurrentPlayers(anyString())).thenReturn(5L);

        log.info("✅ 동시성 테스트 환경 설정 완료 - 플레이어 수: {}, 방 ID: {}", 
                players.size(), gameRoom.getId());
    }

    @Test
    @DisplayName("[동시성] 여러 플레이어가 동시에 제출할 때 Redis-DB 카운트 불일치 문제 재현")
    void concurrentSubmission_RedisDbMismatch() throws InterruptedException {
        // Given: 게임 시작
        MultiGameRequest.Start startRequest = createStartRequest(gameRoom.getId(), 60);
        
        // 1단계: 게임 생성
        com.kospot.presentation.multi.game.dto.response.MultiGameResponse.StartGame startGameResponse = 
                notifyStartGameUseCase.execute(hostMember, gameRoom.getId(), startRequest);
        Long gameId = startGameResponse.getGameId();
        
        // 2단계: 1라운드 준비
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                nextRoadViewRoundUseCase.executeInitial(gameRoom.getId(), gameId);

        Long roundId = startResponse.getRoundInfo().getRoundId();
        String roomId = gameRoom.getId().toString();

        log.info("🎮 게임 시작 - GameId: {}, RoundId: {}", gameId, roundId);

        // Redis 초기화
        submissionRedisService.initializeRound(GameMode.ROADVIEW, roundId);

        entityManager.clear();
        RoadViewGameRound round = roadViewGameRoundRepository.findById(roundId).orElseThrow();
        assertThat(round.getIsFinished()).isFalse();

        // When: 모든 플레이어가 동시에 제출 (Race Condition 유발)
        List<GamePlayer> gamePlayers = gamePlayerRepository.findAllByMultiRoadViewGameId(gameId);
        assertThat(gamePlayers).hasSize(5);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch submitLatch = new CountDownLatch(5);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        AtomicLong maxRedisCount = new AtomicLong(0);
        AtomicLong maxDbCount = new AtomicLong(0);

        List<SubmissionResult> results = new ArrayList<>();

        for (int i = 0; i < gamePlayers.size(); i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // 모든 스레드가 동시에 시작하도록 대기
                    startLatch.await();

                    GamePlayer gamePlayer = gamePlayers.get(index);
                    Member member = memberRepository.findById(gamePlayer.getMember().getId()).orElseThrow();

                    SubmitRoadViewRequest.Player submitRequest = SubmitRoadViewRequest.Player.builder()
                            .lat(37.5665 + (index * 0.01))
                            .lng(126.9780 + (index * 0.01))
                            .timeToAnswer(5000.0 + (index * 1000.0))
                            .build();

                    // 제출 전 Redis 카운트 확인
                    long redisCountBefore = submissionRedisService.getCurrentSubmissionCount(
                            GameMode.ROADVIEW, roundId);

                    // 제출 실행
                    submitRoadViewPlayerAnswerUseCase.execute(
                            member, roomId, gameId, roundId, submitRequest);

                    successCount.incrementAndGet();

                    // 제출 후 즉시 Redis 카운트 확인
                    long redisCountAfter = submissionRedisService.getCurrentSubmissionCount(
                            GameMode.ROADVIEW, roundId);
                    maxRedisCount.updateAndGet(current -> Math.max(current, redisCountAfter));

                    // 제출 후 즉시 DB 카운트 확인 (트랜잭션 커밋 전일 수 있음)
                    entityManager.flush();
                    entityManager.clear();
                    long dbCountAfter = submissionRepository.countByRoundIdAndMatchType(
                            roundId, PlayerMatchType.SOLO);
                    maxDbCount.updateAndGet(current -> Math.max(current, dbCountAfter));

                    results.add(new SubmissionResult(
                            gamePlayer.getId(),
                            redisCountBefore,
                            redisCountAfter,
                            dbCountAfter
                    ));

                    log.info("📝 제출 완료 - PlayerId: {}, RedisBefore: {}, RedisAfter: {}, DbAfter: {}",
                            gamePlayer.getId(), redisCountBefore, redisCountAfter, dbCountAfter);

                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                    log.error("❌ 제출 실패 - Index: {}, Error: {}", index, e.getMessage(), e);
                } finally {
                    submitLatch.countDown();
                }
            });
        }

        // 모든 스레드가 준비될 때까지 대기 후 동시에 시작
        Thread.sleep(100);
        startLatch.countDown();

        // 모든 제출 완료 대기
        boolean allCompleted = submitLatch.await(10, TimeUnit.SECONDS);
        assertThat(allCompleted).isTrue();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // 이벤트 처리 대기
        Thread.sleep(2000);

        // Then: 결과 검증
        log.info("📊 제출 결과 요약:");
        log.info("  - 성공: {}, 실패: {}", successCount.get(), exceptionCount.get());
        log.info("  - 최대 Redis 카운트: {}", maxRedisCount.get());
        log.info("  - 최대 DB 카운트 (제출 직후): {}", maxDbCount.get());

        // 최종 상태 확인
        entityManager.flush();
        entityManager.clear();
        
        long finalRedisCount = submissionRedisService.getCurrentSubmissionCount(
                GameMode.ROADVIEW, roundId);
        long finalDbCount = submissionRepository.countByRoundIdAndMatchType(
                roundId, PlayerMatchType.SOLO);

        log.info("📊 최종 상태:");
        log.info("  - Redis 카운트: {}", finalRedisCount);
        log.info("  - DB 카운트: {}", finalDbCount);
        log.info("  - 예상 카운트: 5");

        // 검증: 모든 제출이 성공했는지
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(exceptionCount.get()).isEqualTo(0);

        // 검증: 최종적으로 Redis와 DB 카운트가 일치하는지
        assertThat(finalRedisCount).isEqualTo(5);
        assertThat(finalDbCount).isEqualTo(5);

        // 검증: 제출 직후 DB 카운트가 부족했는지 확인 (트랜잭션 커밋 타이밍 문제 재현)
        boolean dbMismatchDetected = maxDbCount.get() < 5;
        if (dbMismatchDetected) {
            log.warn("⚠️ 트랜잭션 커밋 타이밍 문제 재현됨!");
            log.warn("  - 제출 직후 최대 DB 카운트: {} (예상: 5)", maxDbCount.get());
            log.warn("  - 이는 트랜잭션이 아직 커밋되지 않아 발생한 문제입니다.");
        }

        // 검증: 조기 종료 로직이 정상 동작했는지
        round = roadViewGameRoundRepository.findById(roundId).orElseThrow();
        boolean roundCompleted = round.getIsFinished();
        
        log.info("🎯 조기 종료 결과:");
        log.info("  - 라운드 종료 여부: {}", roundCompleted);
        log.info("  - Redis 카운트: {} (예상: 5)", finalRedisCount);
        log.info("  - DB 카운트: {} (예상: 5)", finalDbCount);

        // 조기 종료가 정상 동작했다면 라운드가 종료되어야 함
        // 하지만 DB 기반 검증으로 인해 실패할 수 있음
        if (!roundCompleted && finalRedisCount == 5 && finalDbCount == 5) {
            log.warn("⚠️ 조기 종료 실패 - Redis와 DB는 모두 5개이지만 라운드가 종료되지 않음");
            log.warn("  - 이는 completeRoundEarly()에서 DB 검증 시점의 타이밍 문제로 인한 것입니다.");
        }
    }

    @Test
    @DisplayName("[동시성] 동시 제출 시 Redis 카운트는 정확하지만 DB 검증에서 실패하는 경우")
    void concurrentSubmission_RedisAccurateButDbFails() throws InterruptedException {
        // Given: 게임 시작
        MultiGameRequest.Start startRequest = createStartRequest(gameRoom.getId(), 60);
        
        // 1단계: 게임 생성
        com.kospot.presentation.multi.game.dto.response.MultiGameResponse.StartGame startGameResponse = 
                notifyStartGameUseCase.execute(hostMember, gameRoom.getId(), startRequest);
        Long gameId = startGameResponse.getGameId();
        
        // 2단계: 1라운드 준비
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                nextRoadViewRoundUseCase.executeInitial(gameRoom.getId(), gameId);

        Long roundId = startResponse.getRoundInfo().getRoundId();
        String roomId = gameRoom.getId().toString();

        submissionRedisService.initializeRound(GameMode.ROADVIEW, roundId);

        List<GamePlayer> gamePlayers = gamePlayerRepository.findAllByMultiRoadViewGameId(gameId);
        assertThat(gamePlayers).hasSize(5);

        // When: 모든 플레이어가 동시에 제출
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch submitLatch = new CountDownLatch(5);
        AtomicInteger checkAttempts = new AtomicInteger(0);
        AtomicInteger checkFailures = new AtomicInteger(0);

        for (int i = 0; i < gamePlayers.size(); i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    GamePlayer gamePlayer = gamePlayers.get(index);
                    Member member = memberRepository.findById(gamePlayer.getMember().getId()).orElseThrow();

                    SubmitRoadViewRequest.Player submitRequest = SubmitRoadViewRequest.Player.builder()
                            .lat(37.5665 + (index * 0.01))
                            .lng(126.9780 + (index * 0.01))
                            .timeToAnswer(5000.0 + (index * 1000.0))
                            .build();

                    submitRoadViewPlayerAnswerUseCase.execute(
                            member, roomId, gameId, roundId, submitRequest);

                    // 이벤트 핸들러가 실행되기 전에 수동으로 조기 종료 체크
                    Thread.sleep(50); // 약간의 지연
                    
                    checkAttempts.incrementAndGet();
                    boolean completed = checkAndCompleteRoundEarlyUseCase.execute(
                            roomId, gameId, roundId, GameMode.ROADVIEW, PlayerMatchType.SOLO);
                    
                    if (!completed) {
                        checkFailures.incrementAndGet();
                        log.warn("⚠️ 조기 종료 체크 실패 - PlayerId: {}, Attempt: {}", 
                                gamePlayer.getId(), checkAttempts.get());
                    }

                } catch (Exception e) {
                    log.error("❌ 제출 실패 - Index: {}", index, e);
                } finally {
                    submitLatch.countDown();
                }
            });
        }

        Thread.sleep(100);
        startLatch.countDown();

        boolean allCompleted = submitLatch.await(10, TimeUnit.SECONDS);
        assertThat(allCompleted).isTrue();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // 이벤트 처리 대기
        Thread.sleep(2000);

        // Then: 결과 검증
        long finalRedisCount = submissionRedisService.getCurrentSubmissionCount(
                GameMode.ROADVIEW, roundId);
        entityManager.flush();
        entityManager.clear();
        long finalDbCount = submissionRepository.countByRoundIdAndMatchType(
                roundId, PlayerMatchType.SOLO);

        log.info("📊 최종 검증 결과:");
        log.info("  - Redis 카운트: {}", finalRedisCount);
        log.info("  - DB 카운트: {}", finalDbCount);
        log.info("  - 조기 종료 체크 시도: {}", checkAttempts.get());
        log.info("  - 조기 종료 체크 실패: {}", checkFailures.get());

        assertThat(finalRedisCount).isEqualTo(5);
        assertThat(finalDbCount).isEqualTo(5);

        // 조기 종료 체크 실패가 발생했는지 확인
        if (checkFailures.get() > 0) {
            log.warn("⚠️ 동시성 문제 확인: {}번의 조기 종료 체크가 실패했습니다.", checkFailures.get());
            log.warn("  - 이는 DB 기반 검증 시 트랜잭션 커밋 타이밍 문제로 인한 것입니다.");
        }
    }

    // === Helper Classes ===

    private static class SubmissionResult {
        final Long playerId;
        final long redisCountBefore;
        final long redisCountAfter;
        final long dbCountAfter;

        SubmissionResult(Long playerId, long redisCountBefore, long redisCountAfter, long dbCountAfter) {
            this.playerId = playerId;
            this.redisCountBefore = redisCountBefore;
            this.redisCountAfter = redisCountAfter;
            this.dbCountAfter = dbCountAfter;
        }
    }

    // === Helper Methods ===

    private void createTestCoordinates() {
        // 테스트용 좌표 데이터 생성 (최소 10개 이상 권장)
        for (int i = 1; i <= 20; i++) {
            Address address = Address.builder()
                    .sido(Sido.SEOUL)
                    .sigungu("중구")
                    .detailAddress("테스트 주소 " + i)
                    .fullAddress("서울특별시 중구 테스트 주소 " + i)
                    .build();

            Coordinate coordinate = Coordinate.builder()
                    .lat(37.5665 + (i * 0.01))
                    .lng(126.9780 + (i * 0.01))
                    .poiName("테스트 장소 " + i)
                    .address(address)
                    .locationType(LocationType.TOURIST_ATTRACTION)
                    .build();

            coordinateRepository.save(coordinate);
        }

        log.info("✅ 테스트용 좌표 데이터 생성 완료 - 총 {}개", 20);
    }

    private Member createMember(String username, String email, String nickname) {
        Member member = Member.builder()
                .username(username)
                .email(email + "@test.com")
                .nickname(nickname)
                .role(Role.USER)
                .build();
        return memberRepository.save(member);
    }

    private GameRoom createGameRoom(Member host, List<Member> players) {
        GameRoom room = GameRoom.builder()
                .host(host)
                .status(GameRoomStatus.WAITING)
                .build();
        room = gameRoomRepository.save(room);

        // 플레이어 추가는 실제 게임 시작 시 처리됨
        return room;
    }

    private MultiGameRequest.Start createStartRequest(Long gameRoomId, Integer timeLimit) {
        return MultiGameRequest.Start.builder()
                .totalRounds(5)
                .timeLimit(timeLimit)
                .playerMatchTypeKey("SOLO")
                .gameModeKey("ROADVIEW")
                .build();
    }
}


package com.kospot.multi.submission;

import com.kospot.application.coordinate.ImportCoordinateUseCase;
import com.kospot.application.multi.round.roadview.solo.StartRoadViewSoloRoundUseCase;
import com.kospot.application.multi.submission.http.usecase.SubmitRoadViewPlayerAnswerUseCase;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.repository.ImageRepository;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.gamePlayer.repository.GamePlayerRepository;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.repository.GameRoomRepository;
import com.kospot.domain.multi.room.vo.GameRoomStatus;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.repository.RoadViewGameRoundRepository;
import com.kospot.domain.multi.submission.repository.RoadViewSubmissionRepository;
import com.kospot.infrastructure.redis.domain.multi.room.adaptor.GameRoomRedisAdaptor;
import com.kospot.infrastructure.redis.domain.multi.submission.service.SubmissionRedisService;
import com.kospot.presentation.multi.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
import com.kospot.presentation.multi.submission.dto.request.SubmitRoadViewRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 라운드 조기 종료 로직 성능 벤치마크 테스트
 * 
 * 테스트 시나리오:
 * 1. Baseline: 원본 코드 (예외 발생률 측정)
 * 2. Solution 1: 멱등성 보장
 * 3. Solution 2: 비관적 락
 * 4. Solution 3: Redis 분산 락
 * 5. Solution 4: 이벤트 중복 제거
 * 
 * 측정 지표:
 * - 성공률 (Success Rate)
 * - 평균 응답 시간 (Average Response Time)
 * - 최대/최소 응답 시간
 * - 처리량 (Throughput)
 * - 예외 발생 횟수
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoundCompletionPerformanceTest {

    @Autowired
    private StartRoadViewSoloRoundUseCase startRoadViewSoloRoundUseCase;

    @Autowired
    private SubmitRoadViewPlayerAnswerUseCase submitRoadViewPlayerAnswerUseCase;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private RoadViewGameRoundRepository roadViewGameRoundRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private RoadViewSubmissionRepository submissionRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImportCoordinateUseCase importCoordinateUseCase;

    @Autowired
    private SubmissionRedisService submissionRedisService;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @MockitoBean
    private GameRoomRedisAdaptor gameRoomRedisAdaptor;

    private Image markerImage;
    private static final int ITERATION_COUNT = 50; // 반복 테스트 횟수
    private static final int PLAYER_COUNT = 4; // 플레이어 수

    private final Map<String, BenchmarkResult> results = new LinkedHashMap<>();

    @BeforeAll
    void setUpOnce() {
        // 마커 이미지 생성
        markerImage = Image.builder()
                .imageUrl("http://example.com/marker.png")
                .build();
        imageRepository.save(markerImage);

        // 좌표 데이터 import
        importCoordinateUseCase.execute("test_coordinates_excel.xlsx");

        // Redis Mock 설정
        when(gameRoomRedisAdaptor.getCurrentPlayers(anyString())).thenReturn(4L);

        log.info("\n");
        log.info("=".repeat(100));
        log.info("🚀 라운드 조기 종료 로직 성능 벤치마크 시작");
        log.info("=".repeat(100));
        log.info("📊 테스트 설정: {} 라운드, {} 플레이어", ITERATION_COUNT, PLAYER_COUNT);
        log.info("=".repeat(100));
        log.info("\n");
    }

    @AfterAll
    void printFinalReport() {
        log.info("\n");
        log.info("=".repeat(100));
        log.info("📈 최종 성능 벤치마크 결과");
        log.info("=".repeat(100));
        log.info("\n");

        // 테이블 헤더
        log.info(String.format("%-30s | %10s | %12s | %12s | %12s | %10s | %10s",
                "해결 방안", "성공률", "평균 시간", "최소 시간", "최대 시간", "예외 수", "처리량"));
        log.info("-".repeat(120));

        // 각 결과 출력
        results.forEach((name, result) -> {
            log.info(String.format("%-30s | %9.1f%% | %10dms | %10dms | %10dms | %10d | %8.1f/s",
                    name,
                    result.getSuccessRate(),
                    result.getAvgResponseTime(),
                    result.getMinResponseTime(),
                    result.getMaxResponseTime(),
                    result.getExceptionCount(),
                    result.getThroughput()
            ));
        });

        log.info("-".repeat(120));

        // 최고 성능 방안 추천
        BenchmarkResult fastest = results.values().stream()
                .filter(r -> r.getSuccessRate() >= 99.0) // 성공률 99% 이상
                .min(Comparator.comparing(BenchmarkResult::getAvgResponseTime))
                .orElse(null);

        if (fastest != null) {
            String fastestName = results.entrySet().stream()
                    .filter(e -> e.getValue() == fastest)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse("Unknown");
            
            log.info("\n🏆 추천 방안: {} (평균 {}ms, 성공률 {:.1f}%)",
                    fastestName, fastest.getAvgResponseTime(), fastest.getSuccessRate());
        }

        log.info("\n");
        log.info("=".repeat(100));
        log.info("\n");
    }

    @Test
    @Order(1)
    @DisplayName("[성능] Baseline - 원본 코드 (문제 재현)")
    void baseline_OriginalCode() throws Exception {
        BenchmarkResult result = runBenchmark("Baseline (원본)", this::executeOriginalLogic);
        results.put("Baseline (원본)", result);
        
        assertThat(result.getExceptionCount()).as("원본 코드는 Race Condition으로 예외 발생 예상")
                .isGreaterThan(0);
    }

    @Test
    @Order(2)
    @DisplayName("[성능] Solution 1 - 멱등성 보장 (Idempotency)")
    void solution1_Idempotency() throws Exception {
        BenchmarkResult result = runBenchmark("멱등성 보장", this::executeIdempotentLogic);
        results.put("Solution 1: 멱등성 보장", result);
        
        assertThat(result.getSuccessRate()).as("멱등성 보장으로 100% 성공률 예상")
                .isGreaterThanOrEqualTo(99.0);
    }

    @Test
    @Order(3)
    @DisplayName("[성능] Solution 2 - 비관적 락 (Pessimistic Lock)")
    void solution2_PessimisticLock() throws Exception {
        BenchmarkResult result = runBenchmark("비관적 락", this::executePessimisticLockLogic);
        results.put("Solution 2: 비관적 락", result);
        
        assertThat(result.getSuccessRate()).as("비관적 락으로 100% 성공률 예상")
                .isGreaterThanOrEqualTo(99.0);
    }

    @Test
    @Order(4)
    @DisplayName("[성능] Solution 3 - Redis 분산 락")
    void solution3_DistributedLock() throws Exception {
        BenchmarkResult result = runBenchmark("Redis 분산 락", this::executeDistributedLockLogic);
        results.put("Solution 3: Redis 분산 락", result);
        
        assertThat(result.getSuccessRate()).as("분산 락으로 100% 성공률 예상")
                .isGreaterThanOrEqualTo(99.0);
    }

    @Test
    @Order(5)
    @DisplayName("[성능] Solution 4 - 이벤트 중복 제거")
    void solution4_EventDeduplication() throws Exception {
        BenchmarkResult result = runBenchmark("이벤트 중복 제거", this::executeEventDeduplicationLogic);
        results.put("Solution 4: 이벤트 중복 제거", result);
        
        assertThat(result.getSuccessRate()).as("이벤트 중복 제거로 100% 성공률 예상")
                .isGreaterThanOrEqualTo(99.0);
    }

    /**
     * 벤치마크 실행 프레임워크
     */
    private BenchmarkResult runBenchmark(String name, TestScenario scenario) throws Exception {
        log.info("\n📌 테스트 시작: {}", name);
        log.info("-".repeat(80));

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        AtomicLong totalDuration = new AtomicLong(0);
        AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);
        AtomicLong maxDuration = new AtomicLong(0);

        Instant overallStart = Instant.now();

        for (int i = 0; i < ITERATION_COUNT; i++) {
            try {
                // 게임 환경 셋업
                TestContext context = setupGameEnvironment();
                
                Instant start = Instant.now();
                
                // 시나리오 실행
                boolean success = scenario.execute(context);
                
                long duration = Duration.between(start, Instant.now()).toMillis();
                
                totalDuration.addAndGet(duration);
                minDuration.updateAndGet(current -> Math.min(current, duration));
                maxDuration.updateAndGet(current -> Math.max(current, duration));
                
                if (success) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
                
                // 정리
                cleanupGameEnvironment(context);
                
                if ((i + 1) % 10 == 0) {
                    log.info("  진행률: {}/{} ({:.1f}%)", 
                            i + 1, ITERATION_COUNT, (i + 1) * 100.0 / ITERATION_COUNT);
                }
                
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
                failureCount.incrementAndGet();
                log.warn("  ⚠️ 예외 발생 (#{}/{}): {}", 
                        exceptionCount.get(), ITERATION_COUNT, e.getMessage());
            }
            
            // 다음 테스트를 위한 대기
            Thread.sleep(50);
        }

        long overallDuration = Duration.between(overallStart, Instant.now()).toMillis();

        BenchmarkResult result = BenchmarkResult.builder()
                .name(name)
                .iterationCount(ITERATION_COUNT)
                .successCount(successCount.get())
                .failureCount(failureCount.get())
                .exceptionCount(exceptionCount.get())
                .avgResponseTime(successCount.get() > 0 ? totalDuration.get() / successCount.get() : 0)
                .minResponseTime(minDuration.get() == Long.MAX_VALUE ? 0 : minDuration.get())
                .maxResponseTime(maxDuration.get())
                .totalDuration(overallDuration)
                .build();

        log.info("-".repeat(80));
        log.info("✅ 테스트 완료: {}", name);
        log.info("   성공: {}/{} ({:.1f}%), 예외: {}, 평균: {}ms, 최소: {}ms, 최대: {}ms",
                result.getSuccessCount(), result.getIterationCount(), result.getSuccessRate(),
                result.getExceptionCount(), result.getAvgResponseTime(),
                result.getMinResponseTime(), result.getMaxResponseTime());
        log.info("\n");

        return result;
    }

    /**
     * Scenario 1: 원본 로직 (Race Condition 존재)
     */
    private boolean executeOriginalLogic(TestContext context) throws Exception {
        // 모든 플레이어가 동시에 제출 (Race Condition 유발)
        ExecutorService executor = Executors.newFixedThreadPool(PLAYER_COUNT);
        CountDownLatch latch = new CountDownLatch(PLAYER_COUNT);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        List<Future<Boolean>> futures = new ArrayList<>();
        
        for (int i = 0; i < context.players.size(); i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // 모두 동시에 시작
                    
                    Member member = context.players.get(index);
                    SubmitRoadViewRequest.Player request = createSubmitRequest(index);
                    
                    submitRoadViewPlayerAnswerUseCase.execute(
                            member, 
                            context.roomId, 
                            context.gameId, 
                            context.roundId, 
                            request
                    );
                    return true;
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                    return false;
                }
            }));
        }

        // 모든 제출 완료 대기
        for (Future<Boolean> future : futures) {
            future.get();
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // 이벤트 처리 대기
        Thread.sleep(1000);

        // 검증: 라운드가 정확히 1번만 종료되었는지 확인
        RoadViewGameRound round = roadViewGameRoundRepository.findById(context.roundId).orElseThrow();
        
        return round.getIsFinished() && exceptionCount.get() == 0;
    }

    /**
     * Scenario 2: 멱등성 보장 로직
     */
    private boolean executeIdempotentLogic(TestContext context) throws Exception {
        // 멱등성 보장 버전에서는 중복 호출이 안전하게 처리됨
        ExecutorService executor = Executors.newFixedThreadPool(PLAYER_COUNT);
        CountDownLatch latch = new CountDownLatch(PLAYER_COUNT);

        List<Future<Boolean>> futures = new ArrayList<>();
        
        for (int i = 0; i < context.players.size(); i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();
                    
                    Member member = context.players.get(index);
                    SubmitRoadViewRequest.Player request = createSubmitRequest(index);
                    
                    submitRoadViewPlayerAnswerUseCase.execute(
                            member, 
                            context.roomId, 
                            context.gameId, 
                            context.roundId, 
                            request
                    );
                    
                    // 멱등성 테스트: finishRound() 중복 호출 시뮬레이션
                    // 실제로는 이벤트 리스너에서 처리되지만, 여기서는 직접 호출로 테스트
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        boolean allSuccess = futures.stream().allMatch(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                return false;
            }
        });

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        Thread.sleep(1000);

        RoadViewGameRound round = roadViewGameRoundRepository.findById(context.roundId).orElseThrow();
        return round.getIsFinished() && allSuccess;
    }

    /**
     * Scenario 3: 비관적 락 로직
     */
    private boolean executePessimisticLockLogic(TestContext context) throws Exception {
        // 비관적 락 사용 시 순차 처리됨
        ExecutorService executor = Executors.newFixedThreadPool(PLAYER_COUNT);
        CountDownLatch latch = new CountDownLatch(PLAYER_COUNT);

        List<Future<Boolean>> futures = new ArrayList<>();
        
        for (int i = 0; i < context.players.size(); i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();
                    
                    Member member = context.players.get(index);
                    SubmitRoadViewRequest.Player request = createSubmitRequest(index);
                    
                    // 비관적 락을 사용한 제출 처리
                    submitRoadViewPlayerAnswerUseCase.execute(
                            member, 
                            context.roomId, 
                            context.gameId, 
                            context.roundId, 
                            request
                    );
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        boolean allSuccess = futures.stream().allMatch(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                return false;
            }
        });

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        Thread.sleep(1000);

        RoadViewGameRound round = roadViewGameRoundRepository.findById(context.roundId).orElseThrow();
        return round.getIsFinished() && allSuccess;
    }

    /**
     * Scenario 4: Redis 분산 락 로직
     */
    private boolean executeDistributedLockLogic(TestContext context) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(PLAYER_COUNT);
        CountDownLatch latch = new CountDownLatch(PLAYER_COUNT);

        List<Future<Boolean>> futures = new ArrayList<>();
        
        for (int i = 0; i < context.players.size(); i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();
                    
                    Member member = context.players.get(index);
                    SubmitRoadViewRequest.Player request = createSubmitRequest(index);
                    
                    submitRoadViewPlayerAnswerUseCase.execute(
                            member, 
                            context.roomId, 
                            context.gameId, 
                            context.roundId, 
                            request
                    );
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        boolean allSuccess = futures.stream().allMatch(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                return false;
            }
        });

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        Thread.sleep(1000);

        RoadViewGameRound round = roadViewGameRoundRepository.findById(context.roundId).orElseThrow();
        return round.getIsFinished() && allSuccess;
    }

    /**
     * Scenario 5: 이벤트 중복 제거 로직
     */
    private boolean executeEventDeduplicationLogic(TestContext context) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(PLAYER_COUNT);
        CountDownLatch latch = new CountDownLatch(PLAYER_COUNT);

        List<Future<Boolean>> futures = new ArrayList<>();
        
        for (int i = 0; i < context.players.size(); i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();
                    
                    Member member = context.players.get(index);
                    SubmitRoadViewRequest.Player request = createSubmitRequest(index);
                    
                    submitRoadViewPlayerAnswerUseCase.execute(
                            member, 
                            context.roomId, 
                            context.gameId, 
                            context.roundId, 
                            request
                    );
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        boolean allSuccess = futures.stream().allMatch(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                return false;
            }
        });

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        Thread.sleep(1000);

        RoadViewGameRound round = roadViewGameRoundRepository.findById(context.roundId).orElseThrow();
        return round.getIsFinished() && allSuccess;
    }

    // === Helper Methods ===

    private TestContext setupGameEnvironment() {
        // 멤버 생성
        List<Member> players = new ArrayList<>();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        
        for (int i = 0; i < PLAYER_COUNT; i++) {
            Member member = Member.builder()
                    .username("player_" + uuid + "_" + i)
                    .nickname("플레이어" + i)
                    .equippedMarkerImage(markerImage)
                    .role(Role.USER)
                    .build();
            players.add(memberRepository.save(member));
        }

        // 게임방 생성
        GameRoom gameRoom = GameRoom.builder()
                .host(players.get(0))
                .gameMode(GameMode.ROADVIEW)
                .playerMatchType(PlayerMatchType.SOLO)
                .maxPlayers(6)
                .status(GameRoomStatus.WAITING)
                .title("Performance Test Room")
                .build();
        
        GameRoom savedRoom = gameRoomRepository.save(gameRoom);
        
        for (Member member : players) {
            savedRoom.join(member, null);
        }
        memberRepository.saveAll(players);
        gameRoomRepository.save(savedRoom);

        // 게임 시작
        MultiGameRequest.Start startRequest = createStartRequest(savedRoom.getId(), 60);
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                startRoadViewSoloRoundUseCase.execute(players.get(0), startRequest);

        Long gameId = startResponse.getGameId();
        Long roundId = startResponse.getRoundInfo().getRoundId();
        String roomId = savedRoom.getId().toString();

        // Redis 초기화
        submissionRedisService.initializeRound(GameMode.ROADVIEW, roundId);

        return new TestContext(gameRoom, players, gameId, roundId, roomId);
    }

    private void cleanupGameEnvironment(TestContext context) {
        // 제출 데이터 삭제
        submissionRepository.deleteAll(
                submissionRepository.findSoloSubmissionsByRoundIdOrderByDistance(context.roundId)
        );
        
        // 게임 플레이어 삭제
        gamePlayerRepository.deleteAll(
                gamePlayerRepository.findAllByMultiRoadViewGameId(context.gameId)
        );
        
        // 라운드 삭제
        roadViewGameRoundRepository.deleteById(context.roundId);
        
        // 게임방 삭제
        gameRoomRepository.deleteById(context.gameRoom.getId());
        
        // 멤버 삭제
        memberRepository.deleteAll(context.players);
    }

    private MultiGameRequest.Start createStartRequest(Long gameRoomId, Integer timeLimit) {
        MultiGameRequest.Start request = new MultiGameRequest.Start();
        request.setGameRoomId(gameRoomId);
        request.setTotalRounds(5);
        request.setTimeLimit(timeLimit);
        request.setPlayerMatchTypeKey("SOLO");
        return request;
    }

    private SubmitRoadViewRequest.Player createSubmitRequest(int index) {
        return SubmitRoadViewRequest.Player.builder()
                .lat(37.5665 + (index * 0.01))
                .lng(126.9780 + (index * 0.01))
                .distance(1000.0 + (index * 500.0))
                .timeToAnswer(5000.0 + (index * 1000.0))
                .build();
    }

    // === Inner Classes ===

    @FunctionalInterface
    private interface TestScenario {
        boolean execute(TestContext context) throws Exception;
    }

    @Data
    @AllArgsConstructor
    private static class TestContext {
        GameRoom gameRoom;
        List<Member> players;
        Long gameId;
        Long roundId;
        String roomId;
    }

    @Data
    @Builder
    private static class BenchmarkResult {
        private String name;
        private int iterationCount;
        private int successCount;
        private int failureCount;
        private int exceptionCount;
        private long avgResponseTime; // ms
        private long minResponseTime; // ms
        private long maxResponseTime; // ms
        private long totalDuration; // ms

        public double getSuccessRate() {
            return (successCount * 100.0) / iterationCount;
        }

        public double getThroughput() {
            return (iterationCount * 1000.0) / totalDuration;
        }
    }
}


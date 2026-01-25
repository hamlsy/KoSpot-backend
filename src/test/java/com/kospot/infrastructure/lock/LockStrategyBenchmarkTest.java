package com.kospot.infrastructure.lock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.repository.ImageRepository;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.repository.GameRoomRepository;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.domain.multi.room.vo.GameRoomStatus;
import com.kospot.infrastructure.lock.strategy.HostAssignmentLockStrategy;
import com.kospot.infrastructure.lock.strategy.LuaScriptStrategy;
import com.kospot.infrastructure.lock.strategy.RedisTransactionStrategy;
import com.kospot.infrastructure.lock.strategy.RedissonLockStrategy;
import com.kospot.infrastructure.lock.vo.HostAssignmentResult;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.redisson.api.RedissonClient;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lock Strategy 성능 벤치마크 테스트
 * 
 * 3가지 동시성 제어 방식의 성능을 비교:
 * - Redisson RLock (비관적 락)
 * - Redis Transaction (낙관적 락)
 * - Lua Script (원자적 실행)
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LockStrategyBenchmarkTest {

    @Autowired
    private RedissonLockStrategy redissonLockStrategy;

    @Autowired
    private RedisTransactionStrategy redisTransactionStrategy;

    @Autowired
    private LuaScriptStrategy luaScriptStrategy;

    @Autowired
    private GameRoomRedisService gameRoomRedisService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ImageRepository imageRepository;

    private static final int WARMUP_ITERATIONS = 10;
    private static final int BENCHMARK_ITERATIONS = 100;
    private static final int CONCURRENT_THREADS = 10;

    private final List<BenchmarkResult> results = new ArrayList<>();

    // Cleanup tracking
    private final List<Member> testMembers = new ArrayList<>();
    private final List<Image> testImages = new ArrayList<>();
    private GameRoom testGameRoom;
    private String testRoomId;

    @AfterEach
    void tearDown() {
        if (testRoomId != null) {
            gameRoomRedisService.deleteRoomData(testRoomId);
        }

        for (Member member : testMembers) {
            try {
                Member fresh = memberRepository.findById(member.getId()).orElse(null);
                if (fresh != null) {
                    fresh.leaveGameRoom();
                    memberRepository.save(fresh);
                }
            } catch (Exception ignored) {
            }
        }

        if (testGameRoom != null) {
            try {
                gameRoomRepository.deleteById(testGameRoom.getId());
            } catch (Exception ignored) {
            }
        }

        for (Member member : testMembers) {
            try {
                memberRepository.deleteById(member.getId());
            } catch (Exception ignored) {
            }
        }
        for (Image image : testImages) {
            try {
                imageRepository.deleteById(image.getId());
            } catch (Exception ignored) {
            }
        }

        testMembers.clear();
        testImages.clear();
        testGameRoom = null;
        testRoomId = null;
    }

    @Test
    @Order(1)
    @DisplayName("1. Redisson RLock 성능 벤치마크")
    void benchmarkRedissonLock() {
        BenchmarkResult result = runBenchmark(redissonLockStrategy, "Redisson RLock");
        results.add(result);
        printResult(result);
    }

    @Test
    @Order(2)
    @DisplayName("2. Redis Transaction 성능 벤치마크")
    void benchmarkRedisTransaction() {
        BenchmarkResult result = runBenchmark(redisTransactionStrategy, "Redis Transaction");
        results.add(result);
        printResult(result);
    }

    @Test
    @Order(3)
    @DisplayName("3. Lua Script 성능 벤치마크")
    void benchmarkLuaScript() {
        BenchmarkResult result = runBenchmark(luaScriptStrategy, "Lua Script");
        results.add(result);
        printResult(result);
    }

    @Test
    @Order(4)
    @DisplayName("4. 동시성 Race Condition 테스트 - Redisson")
    void concurrencyTestRedisson() {
        ConcurrencyResult result = runConcurrencyTest(redissonLockStrategy, "Redisson RLock");
        assertThat(result.duplicateHostCount).isZero();
        assertThat(result.successCount).isGreaterThan(0);
    }

    @Test
    @Order(5)
    @DisplayName("5. 동시성 Race Condition 테스트 - Transaction")
    void concurrencyTestTransaction() {
        ConcurrencyResult result = runConcurrencyTest(redisTransactionStrategy, "Redis Transaction");
        assertThat(result.duplicateHostCount).isZero();
    }

    @Test
    @Order(6)
    @DisplayName("6. 동시성 Race Condition 테스트 - Lua Script")
    void concurrencyTestLuaScript() {
        ConcurrencyResult result = runConcurrencyTest(luaScriptStrategy, "Lua Script");
        assertThat(result.duplicateHostCount).isZero();
    }

    private BenchmarkResult runBenchmark(HostAssignmentLockStrategy strategy, String name) {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            runSingleOperation(strategy);
        }

        // Benchmark
        long[] times = new long[BENCHMARK_ITERATIONS];
        int successCount = 0;
        int failureCount = 0;

        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            setupTestData(String.valueOf(i));

            long start = System.nanoTime();
            HostAssignmentResult result = executeStrategy(strategy);
            long end = System.nanoTime();

            times[i] = end - start;
            if (result != null && result.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
            }

            cleanupTestData();
        }

        Arrays.sort(times);

        return new BenchmarkResult(
                name,
                calculateAverage(times),
                times[0],
                times[times.length - 1],
                times[(int) (times.length * 0.5)], // p50
                times[(int) (times.length * 0.95)], // p95
                times[(int) (times.length * 0.99)], // p99
                successCount,
                failureCount);
    }

    private ConcurrencyResult runConcurrencyTest(HostAssignmentLockStrategy strategy, String name) {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        // 방장 + 후보 + survivor 생성
        Member host = createMember("host_" + uniqueId);
        Member candidate = createMember("candidate_" + uniqueId);
        Member survivor = createMember("survivor_" + uniqueId);

        testGameRoom = createGameRoom(host);
        testRoomId = testGameRoom.getId().toString();

        setupRedisPlayers(testRoomId, host, candidate, survivor);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 방장과 후보가 동시에 퇴장 시도
        executor.submit(() -> {
            try {
                startLatch.await();
                HostAssignmentResult result = strategy.executeWithLock(
                        testRoomId, host.getId(), () -> performLeaveOperation(host.getId()));
                if (result != null && result.isSuccess())
                    successCount.incrementAndGet();
                else
                    failureCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                finishLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                HostAssignmentResult result = strategy.executeWithLock(
                        testRoomId, candidate.getId(), () -> performLeaveOperation(candidate.getId()));
                if (result != null && result.isSuccess())
                    successCount.incrementAndGet();
                else
                    failureCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                finishLatch.countDown();
            }
        });

        try {
            startLatch.countDown();
            finishLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdown();

        // 검증: 남은 플레이어 확인
        List<GameRoomPlayerInfo> remaining = gameRoomRedisService.getRoomPlayers(testRoomId);
        int hostCount = (int) remaining.stream().filter(GameRoomPlayerInfo::isHost).count();

        System.out.printf("[%s] Concurrent Test - Success: %d, Failure: %d, Remaining: %d, HostCount: %d%n",
                name, successCount.get(), failureCount.get(), remaining.size(), hostCount);

        return new ConcurrencyResult(name, successCount.get(), failureCount.get(),
                remaining.size(), hostCount > 1 ? hostCount - 1 : 0);
    }

    private HostAssignmentResult performLeaveOperation(Long memberId) {
        GameRoomPlayerInfo playerInfo = gameRoomRedisService.removePlayerFromRoom(testRoomId, memberId);
        if (playerInfo == null) {
            return HostAssignmentResult.failure("Player not found");
        }
        return HostAssignmentResult.normalLeave(memberId, playerInfo);
    }

    private void runSingleOperation(HostAssignmentLockStrategy strategy) {
        String warmupRoomId = "warmup-" + UUID.randomUUID().toString().substring(0, 8);
        String roomKey = "game:room:" + warmupRoomId + ":players";

        try {
            GameRoomPlayerInfo player = GameRoomPlayerInfo.builder()
                    .memberId(1L)
                    .nickname("warmup")
                    .isHost(true)
                    .joinedAt(System.currentTimeMillis())
                    .build();

            String json = objectMapper.writeValueAsString(player);
            redisTemplate.opsForHash().put(roomKey, "1", json);

            strategy.executeWithLock(warmupRoomId, 1L,
                    () -> HostAssignmentResult.normalLeave(1L, player));
        } catch (Exception ignored) {
        } finally {
            redisTemplate.delete(roomKey);
        }
    }

    private void setupTestData(String suffix) {
        testRoomId = "bench-" + suffix + "-" + System.nanoTime();
        String roomKey = "game:room:" + testRoomId + ":players";

        try {
            for (int i = 1; i <= 3; i++) {
                GameRoomPlayerInfo player = GameRoomPlayerInfo.builder()
                        .memberId((long) i)
                        .nickname("player" + i)
                        .isHost(i == 1)
                        .joinedAt(System.currentTimeMillis() + i * 100)
                        .build();

                String json = objectMapper.writeValueAsString(player);
                redisTemplate.opsForHash().put(roomKey, String.valueOf(i), json);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HostAssignmentResult executeStrategy(HostAssignmentLockStrategy strategy) {
        return strategy.executeWithLock(testRoomId, 1L, () -> {
            GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.builder()
                    .memberId(1L)
                    .nickname("player1")
                    .isHost(true)
                    .joinedAt(System.currentTimeMillis())
                    .build();
            return HostAssignmentResult.normalLeave(1L, playerInfo);
        });
    }

    private void cleanupTestData() {
        if (testRoomId != null) {
            String roomKey = "game:room:" + testRoomId + ":players";
            redisTemplate.delete(roomKey);
            testRoomId = null;
        }
    }

    private void setupRedisPlayers(String roomId, Member... members) {
        long baseTime = System.currentTimeMillis();
        for (int i = 0; i < members.length; i++) {
            Member member = members[i];
            GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.builder()
                    .memberId(member.getId())
                    .nickname(member.getNickname())
                    .isHost(i == 0)
                    .joinedAt(baseTime + (i * 100))
                    .build();
            gameRoomRedisService.savePlayerToRoom(roomId, playerInfo);
        }
    }

    private Member createMember(String username) {
        Image image = Image.builder()
                .imageUrl("http://example.com/marker/" + username + ".png")
                .build();
        imageRepository.save(image);
        testImages.add(image);

        Member member = Member.builder()
                .username(username)
                .nickname(username)
                .role(Role.USER)
                .point(1000)
                .equippedMarkerImage(image)
                .build();

        Member saved = memberRepository.save(member);
        testMembers.add(saved);
        return saved;
    }

    private GameRoom createGameRoom(Member host) {
        GameRoom gameRoom = GameRoom.builder()
                .title("벤치마크 테스트 방")
                .gameMode(GameMode.ROADVIEW)
                .playerMatchType(PlayerMatchType.SOLO)
                .privateRoom(false)
                .maxPlayers(4)
                .teamCount(1)
                .status(GameRoomStatus.WAITING)
                .host(host)
                .build();
        return gameRoomRepository.save(gameRoom);
    }

    private double calculateAverage(long[] times) {
        long sum = 0;
        for (long t : times)
            sum += t;
        return sum / (double) times.length;
    }

    private void printResult(BenchmarkResult result) {
        System.out.println("\n" + "=".repeat(60));
        System.out.printf("📊 %s 벤치마크 결과%n", result.strategyName);
        System.out.println("=".repeat(60));
        System.out.printf("  평균 실행 시간: %.2f ms%n", result.avgTimeNs / 1_000_000.0);
        System.out.printf("  최소 실행 시간: %.2f ms%n", result.minTimeNs / 1_000_000.0);
        System.out.printf("  최대 실행 시간: %.2f ms%n", result.maxTimeNs / 1_000_000.0);
        System.out.printf("  P50: %.2f ms%n", result.p50TimeNs / 1_000_000.0);
        System.out.printf("  P95: %.2f ms%n", result.p95TimeNs / 1_000_000.0);
        System.out.printf("  P99: %.2f ms%n", result.p99TimeNs / 1_000_000.0);
        System.out.printf("  성공: %d, 실패: %d%n", result.successCount, result.failureCount);
        System.out.println("=".repeat(60));
    }

    record BenchmarkResult(
            String strategyName,
            double avgTimeNs,
            long minTimeNs,
            long maxTimeNs,
            long p50TimeNs,
            long p95TimeNs,
            long p99TimeNs,
            int successCount,
            int failureCount) {
    }

    record ConcurrencyResult(
            String strategyName,
            int successCount,
            int failureCount,
            int remainingPlayers,
            int duplicateHostCount) {
    }
}

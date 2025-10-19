package com.kospot.multi.submission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시성 제어 전략 비교 테스트
 * 
 * 원본 코드를 수정하지 않고, 각 전략의 성능과 정확성을 비교합니다.
 * 
 * 테스트 방법:
 * 1. 간단한 카운터를 사용하여 Race Condition 시뮬레이션
 * 2. 각 전략을 적용하여 동시성 제어 효과 측정
 * 3. 성능 지표 수집 및 비교
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConcurrencyStrategyComparisonTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private static final int CONCURRENT_THREADS = 10; // 동시 실행 스레드 수
    private static final int ITERATIONS = 100; // 각 전략별 반복 횟수

    private final Map<String, StrategyResult> results = new LinkedHashMap<>();

    @BeforeAll
    void setUp() {
        log.info("\n");
        log.info("=".repeat(100));
        log.info("🔬 동시성 제어 전략 비교 테스트");
        log.info("=".repeat(100));
        log.info("📊 설정: {} 동시 스레드, {} 반복", CONCURRENT_THREADS, ITERATIONS);
        log.info("=".repeat(100));
        log.info("\n");
    }

    @AfterAll
    void printReport() {
        log.info("\n");
        log.info("=".repeat(100));
        log.info("📊 최종 비교 결과");
        log.info("=".repeat(100));
        log.info("\n");

        // 테이블 헤더
        log.info(String.format("%-35s | %10s | %12s | %12s | %12s | %12s",
                "전략", "성공률", "평균시간", "최소시간", "최대시간", "Race발생"));
        log.info("-".repeat(110));

        // 각 결과 출력
        results.forEach((name, result) -> {
            log.info(String.format("%-35s | %9.1f%% | %10.2fms | %10.2fms | %10.2fms | %10d회",
                    name,
                    result.getSuccessRate(),
                    result.getAvgDuration(),
                    result.getMinDuration(),
                    result.getMaxDuration(),
                    result.getRaceConditionCount()
            ));
        });

        log.info("-".repeat(110));

        // 분석
        StrategyResult mostAccurate = results.values().stream()
                .max(Comparator.comparing(StrategyResult::getSuccessRate))
                .orElse(null);

        StrategyResult fastest = results.values().stream()
                .filter(r -> r.getSuccessRate() >= 99.0)
                .min(Comparator.comparing(StrategyResult::getAvgDuration))
                .orElse(null);

        if (mostAccurate != null) {
            String accurateName = findStrategyName(mostAccurate);
            log.info("\n🎯 가장 정확한 전략: {} (성공률 {}%)",
                    accurateName, String.format("%.1f", mostAccurate.getSuccessRate()));
        }

        if (fastest != null) {
            String fastestName = findStrategyName(fastest);
            log.info("⚡ 가장 빠른 전략: {} (평균 {}ms)",
                    fastestName, String.format("%.2f", fastest.getAvgDuration()));
        }

        // 추천
        if (fastest != null && mostAccurate != null) {
            if (fastest == mostAccurate) {
                log.info("\n🏆 최종 추천: {} (정확성 + 성능 모두 우수)", findStrategyName(fastest));
            } else {
                log.info("\n💡 추천: 정확성 우선 시 {}, 성능 우선 시 {}",
                        findStrategyName(mostAccurate), findStrategyName(fastest));
            }
        }

        log.info("\n");
        log.info("=".repeat(100));
        log.info("\n");
    }

    private String findStrategyName(StrategyResult result) {
        return results.entrySet().stream()
                .filter(e -> e.getValue() == result)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("Unknown");
    }

    @Test
    @Order(1)
    @DisplayName("[비교] 1. Baseline - Race Condition 존재")
    void strategy1_Baseline() throws Exception {
        StrategyResult result = runStrategy("1. Baseline (문제 있음)", () -> {
            SharedCounter counter = new SharedCounter();
            return simulateConcurrentAccess(() -> {
                // Race Condition 발생 가능
                if (!counter.isFinished()) {
                    counter.finish();
                    return true;
                }
                return false;
            });
        });

        results.put("1. Baseline (문제 있음)", result);
        
        log.info("   ⚠️ Race Condition 발생: {}회", result.getRaceConditionCount());
        assertThat(result.getRaceConditionCount()).as("Baseline은 Race Condition 발생 예상")
                .isGreaterThan(0);
    }

    @Test
    @Order(2)
    @DisplayName("[비교] 2. 멱등성 보장 (Idempotent)")
    void strategy2_Idempotent() throws Exception {
        // 먼저 단순 테스트로 IdempotentCounter가 작동하는지 확인
        log.info("\n🔍 사전 검증: IdempotentCounter 단독 테스트");
        IdempotentCounter testCounter = new IdempotentCounter();
        boolean first = testCounter.finish();
        boolean second = testCounter.finish();
        log.info("   첫 번째 호출: {}, 두 번째 호출: {}", first, second);
        assertThat(first).as("첫 번째 호출은 true").isTrue();
        assertThat(second).as("두 번째 호출은 false").isFalse();
        log.info("   ✅ IdempotentCounter 정상 작동 확인\n");
        
        StrategyResult result = runStrategy("2. 멱등성 보장", () -> {
            IdempotentCounter counter = new IdempotentCounter();
            log.trace("   [Lambda] 새로운 IdempotentCounter 생성됨");
            return simulateConcurrentAccess(() -> {
                // 멱등성 보장: 중복 호출 시 false 반환
                boolean finishResult = counter.finish();
                if (finishResult) {
                    log.trace("      [Thread] finish() 성공!");
                }
                return finishResult;
            });
        });

        results.put("2. 멱등성 보장", result);
        
        log.info("   ✅ 멱등성으로 안전하게 처리");
        log.info("   📊 최종 성공률: {}% (successCount={}, iterations={})", 
                String.format("%.1f", result.getSuccessRate()), result.getSuccessCount(), result.getIterations());
        
        assertThat(result.getSuccessRate()).as("멱등성 보장으로 100% 정확")
                .isGreaterThanOrEqualTo(99.0);
    }

    @Test
    @Order(3)
    @DisplayName("[비교] 3. synchronized 블록")
    void strategy3_Synchronized() throws Exception {
        StrategyResult result = runStrategy("3. synchronized 블록", () -> {
            SynchronizedCounter counter = new SynchronizedCounter();
            return simulateConcurrentAccess(() -> {
                return counter.finish();
            });
        });

        results.put("3. synchronized 블록", result);
        
        log.info("   ✅ synchronized로 순차 처리");
        assertThat(result.getSuccessRate()).as("synchronized로 100% 정확")
                .isEqualTo(100.0);
    }

    @Test
    @Order(4)
    @DisplayName("[비교] 4. ReentrantLock")
    void strategy4_ReentrantLock() throws Exception {
        StrategyResult result = runStrategy("4. ReentrantLock", () -> {
            ReentrantLockCounter counter = new ReentrantLockCounter();
            return simulateConcurrentAccess(() -> {
                return counter.finish();
            });
        });

        results.put("4. ReentrantLock", result);
        
        log.info("   ✅ ReentrantLock으로 명시적 제어");
        assertThat(result.getSuccessRate()).as("ReentrantLock으로 100% 정확")
                .isEqualTo(100.0);
    }

    @Test
    @Order(5)
    @DisplayName("[비교] 5. AtomicBoolean (CAS)")
    void strategy5_AtomicBoolean() throws Exception {
        StrategyResult result = runStrategy("5. AtomicBoolean (CAS)", () -> {
            AtomicBooleanCounter counter = new AtomicBooleanCounter();
            return simulateConcurrentAccess(() -> {
                return counter.finish();
            });
        });

        results.put("5. AtomicBoolean (CAS)", result);
        
        log.info("   ✅ CAS 연산으로 Lock-Free");
        assertThat(result.getSuccessRate()).as("AtomicBoolean으로 100% 정확")
                .isEqualTo(100.0);
    }

    @Test
    @Order(6)
    @DisplayName("[비교] 6. Redis 분산 락 (SETNX)")
    void strategy6_RedisLock() throws Exception {
        StrategyResult result = runStrategy("6. Redis 분산 락", () -> {
            String lockKey = "test:lock:" + UUID.randomUUID();
            return simulateConcurrentAccess(() -> {
                // Redis SETNX (SET if Not eXists)
                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
                
                if (Boolean.TRUE.equals(acquired)) {
                    try {
                        // 임계 영역
                        Thread.sleep(1); // 작업 시뮬레이션
                        return true;
                    } catch (InterruptedException e) {
                        return false;
                    } finally {
                        redisTemplate.delete(lockKey);
                    }
                }
                return false;
            });
        });

        results.put("6. Redis 분산 락", result);
        
        log.info("   ✅ Redis로 분산 환경 지원");
        assertThat(result.getSuccessRate()).as("Redis 분산 락으로 정확한 제어")
                .isGreaterThanOrEqualTo(99.0);
    }

    @Test
    @Order(7)
    @DisplayName("[비교] 7. 이벤트 중복 제거 (Redis)")
    void strategy7_EventDeduplication() throws Exception {
        StrategyResult result = runStrategy("7. 이벤트 중복 제거", () -> {
            String dedupKey = "test:dedup:" + UUID.randomUUID();
            return simulateConcurrentAccess(() -> {
                // 중복 제거: 첫 번째만 성공
                Boolean isFirst = redisTemplate.opsForValue()
                        .setIfAbsent(dedupKey, "processing", Duration.ofSeconds(5));
                
                if (Boolean.TRUE.equals(isFirst)) {
                    try {
                        Thread.sleep(1);
                        return true;
                    } catch (InterruptedException e) {
                        return false;
                    }
                }
                return false;
            });
        });

        results.put("7. 이벤트 중복 제거", result);
        
        log.info("   ✅ 이벤트 중복 자동 제거");
        assertThat(result.getSuccessRate()).as("중복 제거로 정확한 제어")
                .isGreaterThanOrEqualTo(99.0);
    }

    // === Helper Methods ===

    /**
     * 전략 실행 및 측정
     */
    private StrategyResult runStrategy(String name, Supplier<ConcurrencyTestResult> strategy) {
        log.info("\n📌 테스트: {}", name);
        log.info("-".repeat(80));

        List<Double> durations = new ArrayList<>();
        int totalSuccess = 0;
        int totalRaceConditions = 0;
        int validIterations = 0; // 정상적으로 완료된 iteration 수

        for (int i = 0; i < ITERATIONS; i++) {
            Instant start = Instant.now();
            ConcurrencyTestResult testResult = strategy.get();
            double duration = Duration.between(start, Instant.now()).toNanos() / 1_000_000.0; // ms

            durations.add(duration);
            totalSuccess += testResult.successCount;
            totalRaceConditions += testResult.raceConditionCount;
            
            // 정상적으로 1개의 스레드만 성공한 경우를 카운트
            if (testResult.successCount == 1) {
                validIterations++;
            }
            
            // 디버깅: 첫 5개 iteration 로그
            if (i < 5) {
                log.info("   [디버그 Iteration {}] successCount={}, raceConditions={}, duration={}ms",
                        i + 1, testResult.successCount, testResult.raceConditionCount, String.format("%.2f", duration));
            }

            if ((i + 1) % 20 == 0) {
                log.info("   진행: {}/{} (정상: {}, 총성공: {})", 
                        i + 1, ITERATIONS, validIterations, totalSuccess);
            }
        }

        double avgDuration = durations.stream().mapToDouble(d -> d).average().orElse(0.0);
        double minDuration = durations.stream().mapToDouble(d -> d).min().orElse(0.0);
        double maxDuration = durations.stream().mapToDouble(d -> d).max().orElse(0.0);

        StrategyResult result = StrategyResult.builder()
                .name(name)
                .iterations(ITERATIONS)
                .successCount(validIterations) // ⭐ 수정: totalSuccess 대신 validIterations 사용
                .avgDuration(avgDuration)
                .minDuration(minDuration)
                .maxDuration(maxDuration)
                .raceConditionCount(totalRaceConditions)
                .build();

        log.info("-".repeat(80));
        log.info("   결과: 정상완료 {}/{} (성공률 {}%), 총성공 {}, 평균 {}ms",
                validIterations, ITERATIONS, String.format("%.1f", result.getSuccessRate()), 
                totalSuccess, String.format("%.2f", avgDuration));

        return result;
    }

    /**
     * 동시 접근 시뮬레이션
     */
    private ConcurrencyTestResult simulateConcurrentAccess(Supplier<Boolean> action) {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_THREADS);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        
        List<Future<Boolean>> futures = new ArrayList<>();

        // 모든 스레드가 동시에 시작하도록 준비
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await(); // 동시 시작 대기
                    boolean result = action.get();
                    if (result) {
                        successCount.incrementAndGet();
                    }
                    return result;
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                    log.warn("⚠️ 스레드 실행 중 예외 발생: {}", e.getMessage());
                    return false;
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        // 모든 스레드 동시 시작
        startLatch.countDown();

        try {
            boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                log.error("❌ 타임아웃: {}개 스레드가 완료되지 않음", doneLatch.getCount());
            }
        } catch (InterruptedException e) {
            log.error("❌ 대기 중 인터럽트 발생", e);
        }

        executor.shutdown();
        
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("⚠️ Executor가 정상 종료되지 않음");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        int actualSuccess = successCount.get();
        int raceConditions = actualSuccess > 1 ? actualSuccess - 1 : 0;
        
        if (exceptionCount.get() > 0) {
            log.warn("⚠️ {} 개의 스레드에서 예외 발생", exceptionCount.get());
        }

        return new ConcurrencyTestResult(actualSuccess, raceConditions);
    }

    // === Test Counter Implementations ===

    /**
     * 1. Baseline - Race Condition 존재
     */
    static class SharedCounter {
        private boolean finished = false;

        public boolean isFinished() {
            return finished;
        }

        public void finish() {
            // ⚠️ Check-Then-Act Race Condition
            if (!finished) {
                // 여기서 다른 스레드가 끼어들 수 있음
                try {
                    Thread.sleep(1); // Race Condition 유발
                } catch (InterruptedException e) {
                    // ignore
                }
                finished = true;
            }
        }
    }

    /**
     * 2. 멱등성 보장
     */
    static class IdempotentCounter {
        private boolean finished = false;

        public synchronized boolean finish() {
            if (finished) {
                return false; // 이미 종료됨
            }
            finished = true;
            return true; // 종료 성공
        }
    }

    /**
     * 3. synchronized 블록
     */
    static class SynchronizedCounter {
        private boolean finished = false;
        private final Object lock = new Object();

        public boolean finish() {
            synchronized (lock) {
                if (finished) {
                    return false;
                }
                finished = true;
                return true;
            }
        }
    }

    /**
     * 4. ReentrantLock
     */
    static class ReentrantLockCounter {
        private boolean finished = false;
        private final java.util.concurrent.locks.ReentrantLock lock = 
                new java.util.concurrent.locks.ReentrantLock();

        public boolean finish() {
            lock.lock();
            try {
                if (finished) {
                    return false;
                }
                finished = true;
                return true;
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 5. AtomicBoolean (CAS)
     */
    static class AtomicBooleanCounter {
        private final AtomicBoolean finished = new AtomicBoolean(false);

        public boolean finish() {
            // compareAndSet: atomic operation
            return finished.compareAndSet(false, true);
        }
    }

    // === Result Classes ===

    @Data
    @AllArgsConstructor
    static class ConcurrencyTestResult {
        int successCount;
        int raceConditionCount;
    }

    @Data
    @Builder
    static class StrategyResult {
        String name;
        int iterations;
        int successCount;
        double avgDuration;
        double minDuration;
        double maxDuration;
        int raceConditionCount;

        public double getSuccessRate() {
            return (successCount * 100.0) / iterations;
        }
    }
}


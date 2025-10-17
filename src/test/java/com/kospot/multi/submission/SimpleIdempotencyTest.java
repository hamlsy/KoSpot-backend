package com.kospot.multi.submission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 순수 Java 멱등성 테스트 (Spring 없이 실행)
 * 
 * 목적: Spring Context 로드 없이 빠르게 멱등성 로직만 검증
 */
class SimpleIdempotencyTest {

    @Test
    @DisplayName("멱등성 카운터 - 단일 스레드 테스트")
    void singleThreadTest() {
        IdempotentCounter counter = new IdempotentCounter();
        
        boolean first = counter.finish();
        boolean second = counter.finish();
        boolean third = counter.finish();
        
        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(third).isFalse();
        
        System.out.println("✅ 단일 스레드 테스트 성공");
    }

    @Test
    @DisplayName("멱등성 카운터 - 멀티 스레드 테스트")
    void multiThreadTest() throws InterruptedException {
        IdempotentCounter counter = new IdempotentCounter();
        int threadCount = 10;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // 10개 스레드가 동시에 finish() 호출
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 동시 시작
                    boolean result = counter.finish();
                    if (result) {
                        successCount.incrementAndGet();
                        System.out.println("  ✓ 스레드 " + Thread.currentThread().getName() + " 성공!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        System.out.println("🚀 " + threadCount + "개 스레드 동시 시작...");
        startLatch.countDown(); // 모든 스레드 동시 시작
        
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertThat(completed).as("모든 스레드가 완료되어야 함").isTrue();
        assertThat(successCount.get()).as("정확히 1개만 성공해야 함").isEqualTo(1);
        
        System.out.println("✅ 멀티 스레드 테스트 성공: " + successCount.get() + "/10 성공");
    }

    @Test
    @DisplayName("멱등성 카운터 - 100회 반복 테스트")
    void repeatedTest() throws InterruptedException {
        int iterations = 100;
        int successfulIterations = 0;
        
        for (int i = 0; i < iterations; i++) {
            IdempotentCounter counter = new IdempotentCounter();
            int threadCount = 10;
            
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            
            for (int j = 0; j < threadCount; j++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        if (counter.finish()) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            doneLatch.await(5, TimeUnit.SECONDS);
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
            
            if (successCount.get() == 1) {
                successfulIterations++;
            }
            
            if ((i + 1) % 20 == 0) {
                System.out.println("  진행: " + (i + 1) + "/100 (성공: " + successfulIterations + ")");
            }
        }
        
        double successRate = (successfulIterations * 100.0) / iterations;
        System.out.println("✅ 반복 테스트 완료: " + successfulIterations + "/100 (" + 
                String.format("%.1f%%", successRate) + ")");
        
        assertThat(successRate).as("성공률이 100%여야 함").isEqualTo(100.0);
    }

    /**
     * 멱등성 보장 카운터
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
}


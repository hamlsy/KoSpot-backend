package com.kospot.application.main;

import com.kospot.banner.domain.entity.Banner;
import com.kospot.banner.infrastructure.persistence.BannerRepository;
import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.repository.ImageRepository;
import com.kospot.domain.image.vo.ImageType;
import com.kospot.domain.item.entity.Item;
import com.kospot.domain.item.repository.ItemRepository;
import com.kospot.domain.item.vo.ItemType;
import com.kospot.member.domain.entity.Member;
import com.kospot.member.infrastructure.persistence.MemberRepository;
import com.kospot.member.domain.vo.Role;
import com.kospot.notice.domain.entity.Notice;
import com.kospot.notice.infrastructure.persistence.NoticeRepository;
import com.kospot.domain.statistic.entity.MemberStatistic;
import com.kospot.domain.statistic.repository.MemberStatisticRepository;
import com.kospot.presentation.main.dto.response.MainPageResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 메인 페이지 성능 테스트
 * 
 * Redis 캐시 적용 전/후 성능 비교를 위한 테스트 클래스입니다.
 * 
 * [실행 방법]
 * 1. Phase 0 (Baseline): Redis 캐시 적용 전 실행하여 DB 직접 조회 성능 측정
 * 2. Phase 5 (비교): Redis 캐시 적용 후 동일 테스트 실행하여 개선율 확인
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MainPagePerformanceTest {

    @Autowired
    private FindMainPageInfoUseCase findMainPageInfoUseCase;

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberStatisticRepository memberStatisticRepository;

    @Autowired
    private ItemRepository itemRepository;

    private Member testMember;

    // 성능 측정 결과 저장
    private static long singleQueryTime = 0;
    private static double avgQueryTime = 0;
    private static long concurrentQueryTime = 0;

    @BeforeEach
    void setUp() {
        Image markerImage = imageRepository.save(
                Image.builder()
                        .imageUrl("https://test-cdn.kospot.com/profiles/perftest-user.jpg")
                        .s3Key("profiles/perftest-user.jpg")
                        .imageType(ImageType.ITEM)
                        .build());
        Item markerItem = Item.builder()
                .name("기본 마커 이미지")
                .image(markerImage)
                .itemType(ItemType.MARKER)
                .isDefault(true)
                .build();
        itemRepository.save(markerItem);
        // 테스트용 멤버 생성
        testMember = memberRepository.save(
                Member.builder()
                        .username("perftest_user_" + System.currentTimeMillis())
                        .nickname("성능테스트유저")
                        .equippedMarkerImage(markerImage)
                        .role(Role.USER)
                        .build());


        // 테스트용 MemberStatistic 생성
        memberStatisticRepository.save(MemberStatistic.create(testMember));
        // 테스트용 배너 5개 생성 (활성화 상태)
        for (int i = 0; i < 5; i++) {
            Image image = imageRepository.save(
                    Image.builder()
                            .imageUrl("https://test-cdn.kospot.com/banner/test-" + i + ".jpg")
                            .s3Key("banner/test-" + i + ".jpg")
                            .imageType(ImageType.BANNER)
                            .build());

            bannerRepository.save(
                    Banner.builder()
                            .title("테스트 배너 " + i)
                            .image(image)
                            .linkUrl("https://kospot.com/event/" + i)
                            .description("테스트 배너 설명 " + i)
                            .displayOrder(i + 1)
                            .isActive(true)
                            .build());
        }

        // 테스트용 공지사항 10개 생성
        for (int i = 0; i < 10; i++) {
            noticeRepository.save(
                    Notice.builder()
                            .title("테스트 공지사항 " + i)
                            .contentMd("# 테스트 공지사항 내용 " + i)
                            .contentHtml("<h1>테스트 공지사항 내용 " + i + "</h1>")
                            .build());
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. 단일 조회 성능 측정")
    @Transactional
    void singleQueryPerformanceTest() {
        // Warm-up (JIT 컴파일 최적화를 위해)
        findMainPageInfoUseCase.execute(testMember);

        // 측정
        long startTime = System.nanoTime();
        MainPageResponse.MainPageInfo result = findMainPageInfoUseCase.execute(testMember);
        long endTime = System.nanoTime();

        singleQueryTime = (endTime - startTime) / 1_000_000; // ms 변환

        // 검증
        assertNotNull(result);
        assertNotNull(result.getBanners());
        assertNotNull(result.getRecentNotices());

        log.info("=================================================");
        log.info("📊 [단일 조회] 응답 시간: {} ms", singleQueryTime);
        log.info("   - 배너 수: {}", result.getBanners().size());
        log.info("   - 공지사항 수: {}", result.getRecentNotices().size());
        log.info("=================================================");
    }

    @Test
    @Order(2)
    @DisplayName("2. 반복 조회 성능 측정 (100회)")
    @Transactional(readOnly = true)
    void repeatedQueryPerformanceTest() {
        int iterations = 100;

        // Warm-up
        for (int i = 0; i < 5; i++) {
            findMainPageInfoUseCase.execute(testMember);
        }

        // 측정
        long totalTime = 0;
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            findMainPageInfoUseCase.execute(testMember);
            long endTime = System.nanoTime();
            long elapsed = (endTime - startTime) / 1_000_000;
            times.add(elapsed);
            totalTime += elapsed;
        }

        avgQueryTime = (double) totalTime / iterations;
        long minTime = times.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxTime = times.stream().mapToLong(Long::longValue).max().orElse(0);

        log.info("=================================================");
        log.info("📊 [반복 조회 {}회]", iterations);
        log.info("   - 평균 응답 시간: {:.2f} ms", avgQueryTime);
        log.info("   - 최소 응답 시간: {} ms", minTime);
        log.info("   - 최대 응답 시간: {} ms", maxTime);
        log.info("   - 총 소요 시간: {} ms", totalTime);
        log.info("=================================================");
    }

    @Test
    @Order(3)
    @DisplayName("3. 동시 요청 성능 측정 (10개 스레드)")
    void concurrentQueryPerformanceTest() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();

        // 모든 스레드가 동시에 시작하도록 설정
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                latch.await(); // 동시 시작 대기
                long startTime = System.nanoTime();
                findMainPageInfoUseCase.execute(testMember);
                long endTime = System.nanoTime();
                return (endTime - startTime) / 1_000_000;
            }));
        }

        // 동시 시작
        long overallStart = System.nanoTime();
        latch.countDown();

        // 결과 수집
        List<Long> times = new ArrayList<>();
        for (Future<Long> future : futures) {
            times.add(future.get());
        }
        long overallEnd = System.nanoTime();

        concurrentQueryTime = (overallEnd - overallStart) / 1_000_000;

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        double avgConcurrentTime = times.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxConcurrentTime = times.stream().mapToLong(Long::longValue).max().orElse(0);

        log.info("=================================================");
        log.info("📊 [동시 요청 {}개 스레드]", threadCount);
        log.info("   - 총 처리 시간: {} ms", concurrentQueryTime);
        log.info("   - 평균 응답 시간: {:.2f} ms", avgConcurrentTime);
        log.info("   - 최대 응답 시간: {} ms", maxConcurrentTime);
        log.info("   - 처리량 (TPS): {:.2f} req/sec",
                threadCount * 1000.0 / concurrentQueryTime);
        log.info("=================================================");
    }

    @Test
    @Order(4)
    @DisplayName("4. Redis 캐시 성능 비교 (Cache Miss vs Cache Hit)")
    void cachePerformanceComparisonTest() {
        // 캐시 무효화를 위해 새로운 데이터 환경 구성
        // (BeforeEach에서 이미 데이터가 생성됨)

        // === 첫 번째 호출: Cache MISS (DB 조회) ===
        long cacheMissStart = System.nanoTime();
        MainPageResponse.MainPageInfo firstResult = findMainPageInfoUseCase.execute(null);
        long cacheMissEnd = System.nanoTime();
        long cacheMissTime = (cacheMissEnd - cacheMissStart) / 1_000_000;

        assertNotNull(firstResult);

        // === 두 번째 호출: Cache HIT (Redis 조회) ===
        long cacheHitStart = System.nanoTime();
        MainPageResponse.MainPageInfo secondResult = findMainPageInfoUseCase.execute(null);
        long cacheHitEnd = System.nanoTime();
        long cacheHitTime = (cacheHitEnd - cacheHitStart) / 1_000_000;

        assertNotNull(secondResult);

        // === 개선율 계산 ===
        double improvementPercent = cacheMissTime > 0
                ? ((double) (cacheMissTime - cacheHitTime) / cacheMissTime) * 100
                : 0;

        log.info("=================================================");
        log.info("📊 [Redis 캐시 성능 비교]");
        log.info("   ┌──────────────────┬────────────────┐");
        log.info("   │ 구분             │ 응답 시간      │");
        log.info("   ├──────────────────┼────────────────┤");
        log.info("   │ 첫 호출 (DB)     │ {} ms    │",
                String.format("%8.2f", (double) cacheMissTime));

        log.info("   │ 두번째 (Redis)   │ {} ms    │",
                String.format("%8.2f", (double) cacheHitTime));

        log.info("   ├──────────────────┼────────────────┤");
        log.info("   │ 개선율           │ {}%     │",
                String.format("%7.1f", improvementPercent));
        log.info("   └──────────────────┴────────────────┘");
        log.info("=================================================");

        // 캐시 히트가 캐시 미스보다 빠르거나 같아야 함 (허용 오차 있음)
        // 매우 빠른 쿼리의 경우 오차가 있을 수 있어 assertion은 느슨하게 설정
        assertTrue(cacheHitTime <= cacheMissTime + 10,
                "캐시 히트가 캐시 미스보다 현저히 느립니다. 캐시 로직을 확인하세요.");
    }

    @Test
    @Order(5)
    @DisplayName("5. 반복 조회 캐시 효과 측정 (50회)")
    void repeatedCacheHitPerformanceTest() {
        int iterations = 50;

        // 첫 번째 호출로 캐시 채우기
        findMainPageInfoUseCase.execute(null);

        // 캐시 히트 연속 측정
        List<Long> cacheHitTimes = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            findMainPageInfoUseCase.execute(null);
            long end = System.nanoTime();
            cacheHitTimes.add((end - start) / 1_000_000);
        }

        double avgCacheHitTime = cacheHitTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
        long minCacheHitTime = cacheHitTimes.stream()
                .mapToLong(Long::longValue)
                .min()
                .orElse(0);
        long maxCacheHitTime = cacheHitTimes.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

        log.info("=================================================");
        log.info("📊 [반복 캐시 히트 {}회]", iterations);
        log.info("   - 평균 응답 시간: {:.2f} ms", avgCacheHitTime);
        log.info("   - 최소 응답 시간: {} ms", minCacheHitTime);
        log.info("   - 최대 응답 시간: {} ms", maxCacheHitTime);
        log.info("   - 예상 TPS: {:.0f} req/sec", 1000.0 / avgCacheHitTime);
        log.info("=================================================");
    }

    @Test
    @Order(6)
    @DisplayName("6. 성능 측정 요약")
    void performanceSummary() {
        log.info("");
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║               📈 메인 페이지 성능 측정 요약                      ║");
        log.info("╠═══════════════════════════════════════════════════════════════╣");
        log.info("║  테스트               │  결과                                  ║");
        log.info("╠═══════════════════════════════════════════════════════════════╣");
        log.info("║  단일 조회            │  {} ms                              ║", String.format("%6d", singleQueryTime));
        log.info("║  100회 반복 평균      │  {:.2f} ms                         ║", avgQueryTime);
        log.info("║  10 스레드 동시 요청  │  {} ms                              ║", String.format("%6d", concurrentQueryTime));
        log.info("╚═══════════════════════════════════════════════════════════════╝");
        log.info("");
        log.info("💡 테스트 4,5번에서 Redis 캐시 적용 전/후 성능 비교를 확인하세요.");
        log.info("");
    }
}

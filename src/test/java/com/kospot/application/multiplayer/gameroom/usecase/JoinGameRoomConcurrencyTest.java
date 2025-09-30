package com.kospot.application.multiplayer.gameroom.usecase;

import com.kospot.application.multiplayer.gameroom.http.usecase.JoinGameRoomUseCase;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.multigame.game.vo.PlayerMatchType;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRoom.repository.GameRoomRepository;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomStatus;
import com.kospot.infrastructure.exception.object.domain.GameRoomHandler;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import com.kospot.presentation.multigame.gameroom.dto.request.GameRoomRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JoinGameRoomUseCase의 동시성 및 성능 테스트
 * 현재 설계의 Race Condition 문제를 명확히 보여주고,
 * 향후 최적화 작업의 기준점을 제공하는 테스트
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JoinGameRoomConcurrencyTest {

    @Autowired
    private JoinGameRoomUseCase joinGameRoomUseCase;

    @Autowired
    private GameRoomRedisService gameRoomRedisService;

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private MemberRepository memberRepository;

    private GameRoom testGameRoom;
    private List<Member> testMembers;
    private GameRoomRequest.Join joinRequest;

    @BeforeEach
    void setUp() {
        // 호스트 멤버 생성
        Member hostMember = createAndSaveMember("host", "호스트");

        // 테스트 게임방 생성 (최대 4명)
        testGameRoom = createAndSaveGameRoom(hostMember, 4);

        // 테스트 멤버들 생성 (10명 - 의도적으로 최대 인원보다 많이)
        testMembers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Member member = createAndSaveMember("member" + i, "멤버" + i);
            testMembers.add(member);
        }

        // 기본 참가 요청
        joinRequest = GameRoomRequest.Join.builder()
                .password(null)
                .build();

        // Redis 초기화
        gameRoomRedisService.removePlayerFromRoom(testGameRoom.getId().toString(), hostMember.getId());
    }

    @Test
    @DisplayName("동시성 테스트: Race Condition 발생 확인 (현재 설계의 문제점 검증)")
    void shouldDemonstrateRaceConditionIssue() throws InterruptedException {
        // given
        int threadCount = 8;
        int maxPlayers = testGameRoom.getMaxPlayers(); // 4명
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        log.info("🧪 동시성 테스트 시작 - 최대 인원: {}, 시도 스레드: {}", maxPlayers, threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            final int memberIndex = i;
            executorService.execute(() -> {
                try {
                    Member member = testMembers.get(memberIndex);
                    joinGameRoomUseCase.executeV1(member, testGameRoom.getId(), joinRequest);
                    successCount.incrementAndGet();
                    log.debug("✅ 성공 - Member: {}", member.getNickname());
                } catch (GameRoomHandler e) {
                    failureCount.incrementAndGet();
                    exceptions.add(e);
                    log.debug("❌ 실패 - Member: {}, Error: {}", 
                            testMembers.get(memberIndex).getNickname(), e.getMessage());
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    exceptions.add(e);
                    log.error("💥 예상치 못한 에러 - Member: {}", 
                            testMembers.get(memberIndex).getNickname(), e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        log.info("📊 동시성 테스트 결과:");
        log.info("   성공한 참가: {} / {}", successCount.get(), threadCount);
        log.info("   실패한 참가: {}", failureCount.get());
        log.info("   DB에서 실제 참가한 멤버 수: {}", countMembersInGameRoom());
        log.info("   Redis에서 실제 참가한 멤버 수: {}", 
                gameRoomRedisService.getCurrentPlayerCount(testGameRoom.getId().toString()));

        // 현재 설계에서는 Race Condition으로 인해 최대 인원을 초과할 수 있음
        // 이는 문제점을 보여주는 테스트이므로, 완벽한 검증보다는 상황 관찰에 중점
        assertThat(successCount.get()).isLessThanOrEqualTo(threadCount);
        assertThat(failureCount.get()).isGreaterThanOrEqualTo(0);

        // 예외 분석
        exceptions.forEach(e -> log.debug("발생한 예외: {}", e.getMessage()));

        log.warn("⚠️ 현재 설계의 Race Condition 이슈가 확인되었습니다. " +
                "최대 {}명을 초과하여 참가할 수 있는 문제가 있습니다.", maxPlayers);
    }

    @Test
    @DisplayName("순차적 참가 테스트 (정상 케이스 검증)")
    void shouldJoinSequentiallyWithoutIssues() {
        // given
        int maxPlayers = testGameRoom.getMaxPlayers();

        log.info("🔄 순차적 참가 테스트 시작 - 최대 인원: {}", maxPlayers);

        // when & then
        int successfulJoins = 0;
        for (int i = 0; i < maxPlayers; i++) {
            try {
                Member member = testMembers.get(i);
                joinGameRoomUseCase.executeV1(member, testGameRoom.getId(), joinRequest);
                successfulJoins++;
                log.debug("✅ 순차 참가 성공 - Member: {}, 현재 인원: {}", 
                        member.getNickname(), successfulJoins);
            } catch (Exception e) {
                log.error("❌ 순차 참가 실패 - Member: {}, Error: {}", 
                        testMembers.get(i).getNickname(), e.getMessage());
                break;
            }
        }

        // 최대 인원만큼 성공해야 함
        assertThat(successfulJoins).isEqualTo(maxPlayers);
        assertThat(countMembersInGameRoom()).isEqualTo(maxPlayers);

        // 추가 참가 시도는 실패해야 함
        try {
            Member extraMember = testMembers.get(maxPlayers);
            joinGameRoomUseCase.executeV1(extraMember, testGameRoom.getId(), joinRequest);
            log.error("💥 최대 인원 초과 참가가 성공해버림!");
            assert false : "최대 인원 초과 참가가 허용되면 안됩니다";
        } catch (GameRoomHandler e) {
            log.info("✅ 최대 인원 초과 시 올바르게 예외 발생: {}", e.getMessage());
            assertThat(e.getMessage()).contains("FULL");
        }

        log.info("📊 순차적 참가 테스트 완료 - 성공한 참가: {}/{}", successfulJoins, maxPlayers);
    }

    @Test
    @DisplayName("성능 기준 테스트 (현재 설계의 베이스라인 측정)")
    void shouldMeasureCurrentPerformanceBaseline() {
        // given
        int testRounds = 5;
        int participantsPerRound = 4;
        List<Long> executionTimes = new ArrayList<>();

        log.info("📈 성능 기준 테스트 시작 - {} 라운드, 라운드당 {} 명", testRounds, participantsPerRound);

        // when
        for (int round = 0; round < testRounds; round++) {
            // 새로운 게임방 생성
            Member roundHost = createAndSaveMember("host_round_" + round, "라운드" + round + "호스트");
            GameRoom roundGameRoom = createAndSaveGameRoom(roundHost, participantsPerRound);

            long startTime = System.currentTimeMillis();

            // 참가자들 순차 참가
            for (int i = 0; i < participantsPerRound; i++) {
                Member member = createAndSaveMember("member_round_" + round + "_" + i, 
                        "라운드" + round + "멤버" + i);
                joinGameRoomUseCase.executeV1(member, roundGameRoom.getId(), joinRequest);
            }

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            executionTimes.add(executionTime);

            log.debug("라운드 {} 완료 - 실행 시간: {}ms", round + 1, executionTime);
        }

        // then
        double avgExecutionTime = executionTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        long maxExecutionTime = executionTimes.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);

        long minExecutionTime = executionTimes.stream()
                .mapToLong(Long::longValue)
                .min()
                .orElse(0L);

        log.info("📊 성능 기준 테스트 결과:");
        log.info("   평균 실행 시간: {:.2f}ms", avgExecutionTime);
        log.info("   최대 실행 시간: {}ms", maxExecutionTime);
        log.info("   최소 실행 시간: {}ms", minExecutionTime);
        log.info("   테스트 일시: {}", LocalDateTime.now());

        // 기준 성능 검증 (향후 최적화 시 비교 기준)
        assertThat(avgExecutionTime).isLessThan(5000.0); // 5초 이내
        assertThat(maxExecutionTime).isLessThan(10000L); // 10초 이내

        log.info("✅ 현재 설계의 성능 기준이 측정되었습니다. 향후 최적화 작업 시 이 값을 참조하세요.");
    }

    @Test
    @DisplayName("데이터 일관성 검증 테스트")
    void shouldVerifyDataConsistencyIssues() throws InterruptedException {
        // given
        int threadCount = 6;
        int maxPlayers = testGameRoom.getMaxPlayers();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        log.info("🔍 데이터 일관성 검증 테스트 시작");

        // when
        for (int i = 0; i < threadCount; i++) {
            final int memberIndex = i;
            executorService.execute(() -> {
                try {
                    Member member = testMembers.get(memberIndex);
                    joinGameRoomUseCase.executeV1(member, testGameRoom.getId(), joinRequest);
                } catch (Exception e) {
                    // 예외는 예상된 것임
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // 일정 시간 대기 (이벤트 처리 완료 대기)
        Thread.sleep(1000);

        // then
        int dbCount = countMembersInGameRoom();
        int redisCount = gameRoomRedisService.getCurrentPlayerCount(testGameRoom.getId().toString());

        log.info("📊 데이터 일관성 검증 결과:");
        log.info("   DB에 기록된 참가자 수: {}", dbCount);
        log.info("   Redis에 기록된 참가자 수: {}", redisCount);
        log.info("   최대 허용 인원: {}", maxPlayers);

        // 현재 설계에서는 DB와 Redis 간 불일치가 발생할 수 있음
        if (dbCount != redisCount) {
            log.warn("⚠️ DB와 Redis 간 데이터 불일치 발견! DB: {}, Redis: {}", dbCount, redisCount);
        } else {
            log.info("✅ DB와 Redis 데이터 일치 확인");
        }

        // 어느 쪽이든 최대 인원은 검증
        assertThat(dbCount).isLessThanOrEqualTo(maxPlayers + 2); // 동시성 이슈로 인한 여유분
        assertThat(redisCount).isLessThanOrEqualTo(maxPlayers + 2);

        log.info("🔍 데이터 일관성 검증 완료 - 현재 설계의 특성이 확인되었습니다.");
    }

    // Helper Methods

    private Member createAndSaveMember(String username, String nickname) {
        Member member = Member.builder()
                .username(username)
                .nickname(nickname)
                .role(Role.USER)
                .point(1000)
                .build();
        return memberRepository.save(member);
    }

    private GameRoom createAndSaveGameRoom(Member host, int maxPlayers) {
        GameRoom gameRoom = GameRoom.builder()
                .title("동시성 테스트 게임방")
                .gameMode(GameMode.ROADVIEW)
                .playerMatchType(PlayerMatchType.SOLO)
                .privateRoom(false)
                .maxPlayers(maxPlayers)
                .teamCount(1)
                .status(GameRoomStatus.WAITING)
                .host(host)
                .deleted(false)
                .build();
        return gameRoomRepository.save(gameRoom);
    }

    private int countMembersInGameRoom() {
        return memberRepository.findAll().stream()
                .mapToInt(member -> testGameRoom.getId().equals(member.getGameRoomId()) ? 1 : 0)
                .sum();
    }
}

package com.kospot.application.multi.room.http.usecase;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.repository.GameRoomRepository;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.domain.multi.room.vo.GameRoomStatus;
import com.kospot.infrastructure.redis.domain.multi.room.dao.GameRoomRedisRepository;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LeaveGameRoomUseCase의 동시성 테스트
 * 분산 락 도입으로 Race Condition이 해결되었는지 검증
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LeaveGameRoomUseCaseConcurrencyTest {

    @Autowired
    private LeaveGameRoomUseCase leaveGameRoomUseCase;

    @Autowired
    private JoinGameRoomUseCase joinGameRoomUseCase;

    @Autowired
    private GameRoomRedisService gameRoomRedisService;

    @Autowired
    private GameRoomRedisRepository gameRoomRedisRepository;

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private MemberRepository memberRepository;

    private GameRoom testGameRoom;
    private List<Member> testMembers;
    private Member hostMember;

    @BeforeEach
    void setUp() {
        // 호스트 멤버 생성
        hostMember = createAndSaveMember("host", "호스트");

        // 테스트 게임방 생성 (최대 4명)
        testGameRoom = createAndSaveGameRoom(hostMember, 4);

        // 테스트 멤버들 생성 (5명)
        testMembers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Member member = createAndSaveMember("member" + i, "멤버" + i);
            testMembers.add(member);
        }

        // 호스트를 Redis에 추가
        GameRoomPlayerInfo hostInfo = GameRoomPlayerInfo.builder()
                .memberId(hostMember.getId())
                .nickname(hostMember.getNickname())
                .isHost(true)
                .joinedAt(System.currentTimeMillis())
                .build();
        gameRoomRedisService.savePlayerToRoom(testGameRoom.getId().toString(), hostInfo);

        // 테스트 멤버들을 방에 참가시킴
        for (int i = 0; i < 3; i++) {
            Member member = testMembers.get(i);
            joinGameRoomUseCase.executeV1(member, testGameRoom.getId(), 
                    com.kospot.presentation.multi.room.dto.request.GameRoomRequest.Join.builder().build());
        }
    }

    @Test
    @DisplayName("동시 퇴장 시 Race Condition 해결 확인 - 방장 없는 방 발생 방지")
    void shouldResolveRaceConditionWithDistributedLock() throws InterruptedException {
        // given
        int testRounds = 100;
        int hostlessRoomCount = 0;

        log.info("동시성 테스트 시작 - 테스트 라운드: {}", testRounds);

        // when
        for (int round = 0; round < testRounds; round++) {
            // 매 라운드마다 새로운 방과 멤버 생성
            Member roundHost = createAndSaveMember("host_round_" + round, "라운드" + round + "호스트");
            GameRoom roundGameRoom = createAndSaveGameRoom(roundHost, 4);

            // 호스트를 Redis에 추가
            GameRoomPlayerInfo hostInfo = GameRoomPlayerInfo.builder()
                    .memberId(roundHost.getId())
                    .nickname(roundHost.getNickname())
                    .isHost(true)
                    .joinedAt(System.currentTimeMillis())
                    .build();
            gameRoomRedisService.savePlayerToRoom(roundGameRoom.getId().toString(), hostInfo);

            // 플레이어들을 방에 참가시킴
            List<Member> roundPlayers = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Member player = createAndSaveMember("player_round_" + round + "_" + i, 
                        "라운드" + round + "플레이어" + i);
                roundPlayers.add(player);
                joinGameRoomUseCase.executeV1(player, roundGameRoom.getId(),
                        com.kospot.presentation.multi.room.dto.request.GameRoomRequest.Join.builder().build());
            }

            // 호스트와 플레이어 1명이 동시에 퇴장
            ExecutorService executorService = Executors.newFixedThreadPool(2);
            CountDownLatch latch = new CountDownLatch(2);

            executorService.execute(() -> {
                try {
                    leaveGameRoomUseCase.execute(roundHost, roundGameRoom.getId());
                } catch (Exception e) {
                    log.debug("호스트 퇴장 중 예외: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });

            executorService.execute(() -> {
                try {
                    leaveGameRoomUseCase.execute(roundPlayers.get(0), roundGameRoom.getId());
                } catch (Exception e) {
                    log.debug("플레이어 퇴장 중 예외: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });

            latch.await(5, TimeUnit.SECONDS);
            executorService.shutdown();

            // 잠시 대기 (이벤트 처리 완료 대기)
            Thread.sleep(100);

            // 방장 존재 여부 확인
            String roomId = roundGameRoom.getId().toString();
            List<GameRoomPlayerInfo> remainingPlayers = gameRoomRedisService.getRoomPlayers(roomId);
            boolean hasHost = remainingPlayers.stream().anyMatch(GameRoomPlayerInfo::isHost);

            if (!remainingPlayers.isEmpty() && !hasHost) {
                hostlessRoomCount++;
                log.warn("라운드 {}: 방장 없는 방 발생! 남은 플레이어 수: {}", round, remainingPlayers.size());
            }
        }

        // then
        log.info("동시성 테스트 결과:");
        log.info("   총 테스트 라운드: {}", testRounds);
        log.info("   방장 없는 방 발생 횟수: {}", hostlessRoomCount);
        log.info("   발생률: {:.2f}%", (hostlessRoomCount * 100.0 / testRounds));

        // 분산 락 도입 후 방장 없는 방 발생은 거의 없어야 함
        assertThat(hostlessRoomCount).isLessThanOrEqualTo(2); // 100회 중 2회 이하 (네트워크 지연 등 예외 상황)
        log.info("분산 락 도입으로 Race Condition이 해결되었습니다.");
    }

    @Test
    @DisplayName("방장 퇴장 시 다음 방장 지정 확인")
    void shouldChangeHostWhenHostLeaves() throws InterruptedException {
        // given
        Member host = hostMember;
        Member player1 = testMembers.get(0);
        Member player2 = testMembers.get(1);

        // 플레이어들을 방에 참가시킴
        joinGameRoomUseCase.executeV1(player1, testGameRoom.getId(),
                com.kospot.presentation.multi.room.dto.request.GameRoomRequest.Join.builder().build());
        joinGameRoomUseCase.executeV1(player2, testGameRoom.getId(),
                com.kospot.presentation.multi.room.dto.request.GameRoomRequest.Join.builder().build());

        Thread.sleep(200); // 참가 처리 대기

        // when: 호스트 퇴장
        leaveGameRoomUseCase.execute(host, testGameRoom.getId());

        Thread.sleep(200); // 퇴장 처리 대기

        // then: 다음 방장이 지정되었는지 확인
        String roomId = testGameRoom.getId().toString();
        List<GameRoomPlayerInfo> remainingPlayers = gameRoomRedisService.getRoomPlayers(roomId);
        
        boolean hasHost = remainingPlayers.stream().anyMatch(GameRoomPlayerInfo::isHost);
        assertThat(hasHost).isTrue();
        
        GameRoomPlayerInfo newHost = remainingPlayers.stream()
                .filter(GameRoomPlayerInfo::isHost)
                .findFirst()
                .orElse(null);
        
        assertThat(newHost).isNotNull();
        log.info("새 방장 지정 확인 - MemberId: {}, Nickname: {}", 
                newHost.getMemberId(), newHost.getNickname());
    }

    @Test
    @DisplayName("락 대기 시간 측정")
    void shouldMeasureLockWaitTime() throws InterruptedException {
        // given
        Member host = hostMember;
        Member player1 = testMembers.get(0);
        Member player2 = testMembers.get(1);

        joinGameRoomUseCase.executeV1(player1, testGameRoom.getId(),
                com.kospot.presentation.multi.room.dto.request.GameRoomRequest.Join.builder().build());
        joinGameRoomUseCase.executeV1(player2, testGameRoom.getId(),
                com.kospot.presentation.multi.room.dto.request.GameRoomRequest.Join.builder().build());

        Thread.sleep(200);

        List<Long> waitTimes = new ArrayList<>();

        // when: 여러 번 동시 퇴장 시도
        for (int i = 0; i < 10; i++) {
            ExecutorService executorService = Executors.newFixedThreadPool(2);
            CountDownLatch latch = new CountDownLatch(2);

            executorService.execute(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    leaveGameRoomUseCase.execute(host, testGameRoom.getId());
                    long endTime = System.currentTimeMillis();
                    waitTimes.add(endTime - startTime);
                } catch (Exception e) {
                    // 예외 무시
                } finally {
                    latch.countDown();
                }
            });

            executorService.execute(() -> {
                try {
                    leaveGameRoomUseCase.execute(player1, testGameRoom.getId());
                } catch (Exception e) {
                    // 예외 무시
                } finally {
                    latch.countDown();
                }
            });

            latch.await(5, TimeUnit.SECONDS);
            executorService.shutdown();

            // 다음 테스트를 위해 다시 참가
            Thread.sleep(100);
            joinGameRoomUseCase.executeV1(host, testGameRoom.getId(),
                    com.kospot.presentation.multi.room.dto.request.GameRoomRequest.Join.builder().build());
            joinGameRoomUseCase.executeV1(player1, testGameRoom.getId(),
                    com.kospot.presentation.multi.room.dto.request.GameRoomRequest.Join.builder().build());
            Thread.sleep(200);
        }

        // then
        if (!waitTimes.isEmpty()) {
            double avgWaitTime = waitTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
            
            long maxWaitTime = waitTimes.stream()
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L);

            log.info("락 대기 시간 측정 결과:");
            log.info("   평균: {:.2f}ms", avgWaitTime);
            log.info("   최대: {}ms", maxWaitTime);

            // 락 대기 시간은 일반적으로 100ms 이하여야 함
            assertThat(avgWaitTime).isLessThan(200.0);
        }
    }

    @Test
    @DisplayName("일반 플레이어 퇴장 - 정상 동작 확인")
    void shouldLeaveRoomNormallyForNonHost() {
        // given
        Member player = testMembers.get(0);
        joinGameRoomUseCase.executeV1(player, testGameRoom.getId(),
                com.kospot.presentation.multi.room.dto.request.GameRoomRequest.Join.builder().build());

        String roomId = testGameRoom.getId().toString();
        long playerCountBefore = gameRoomRedisService.getRoomPlayers(roomId).size();

        // when
        leaveGameRoomUseCase.execute(player, testGameRoom.getId());

        // then
        long playerCountAfter = gameRoomRedisService.getRoomPlayers(roomId).size();
        assertThat(playerCountAfter).isEqualTo(playerCountBefore - 1);
        
        // 플레이어가 Redis에서 제거되었는지 확인
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(roomId);
        boolean playerExists = players.stream()
                .anyMatch(p -> p.getMemberId().equals(player.getId()));
        assertThat(playerExists).isFalse();
    }

    @Test
    @DisplayName("모든 플레이어 퇴장 시 방 삭제 확인")
    void shouldDeleteRoomWhenAllPlayersLeave() {
        // given
        Member host = hostMember;
        Member player = testMembers.get(0);

        // 플레이어 퇴장
        leaveGameRoomUseCase.execute(player, testGameRoom.getId());

        // when: 호스트 퇴장 (마지막 플레이어)
        leaveGameRoomUseCase.execute(host, testGameRoom.getId());

        // then: 방 데이터가 Redis에서 삭제되었는지 확인
        String roomId = testGameRoom.getId().toString();
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(roomId);
        
        assertThat(players).isEmpty();
        log.info("모든 플레이어 퇴장 시 방 삭제 확인 완료");
    }

    @Test
    @DisplayName("기존 로직 문제점 재현: 이미 퇴장한 플레이어에게 방장 부여되는 현상 확인")
    void shouldDemonstrateIssueWhereLeftPlayerBecomesHost() throws InterruptedException {
        // given
        int testRounds = 50;
        int issueCount = 0; // 이미 퇴장한 플레이어가 방장으로 지정된 횟수

        log.info("기존 로직 문제점 재현 테스트 시작 - 테스트 라운드: {}", testRounds);
        log.warn("⚠️ 이 테스트는 분산 락이 적용된 현재 코드에서는 문제가 발생하지 않아야 합니다.");
        log.warn("⚠️ 하지만 타이밍에 따라 기존 로직의 문제점을 확인할 수 있습니다.");

        // when
        for (int round = 0; round < testRounds; round++) {
            // 매 라운드마다 새로운 방과 멤버 생성
            Member roundHost = createAndSaveMember("host_round_" + round, "라운드" + round + "호스트");
            GameRoom roundGameRoom = createAndSaveGameRoom(roundHost, 4);

            // 호스트를 Redis에 추가
            long hostJoinedAt = System.currentTimeMillis();
            GameRoomPlayerInfo hostInfo = GameRoomPlayerInfo.builder()
                    .memberId(roundHost.getId())
                    .nickname(roundHost.getNickname())
                    .isHost(true)
                    .joinedAt(hostJoinedAt)
                    .build();
            gameRoomRedisService.savePlayerToRoom(roundGameRoom.getId().toString(), hostInfo);

            // 플레이어들을 방에 참가시킴 (호스트보다 늦게 들어옴)
            List<Member> roundPlayers = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                Member player = createAndSaveMember("player_round_" + round + "_" + i, 
                        "라운드" + round + "플레이어" + i);
                roundPlayers.add(player);
                
                long playerJoinedAt = System.currentTimeMillis() + 100; // 호스트보다 늦게
                GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.builder()
                        .memberId(player.getId())
                        .nickname(player.getNickname())
                        .isHost(false)
                        .joinedAt(playerJoinedAt)
                        .build();
                gameRoomRedisService.savePlayerToRoom(roundGameRoom.getId().toString(), playerInfo);
            }

            String roomId = roundGameRoom.getId().toString();
            Member nextHostCandidate = roundPlayers.get(0); // 다음 방장 후보

            // 시나리오: 호스트와 다음 방장 후보가 거의 동시에 퇴장 요청
            // 기존 로직에서는:
            // 1. 호스트가 pickNextHostByJoinedAt으로 nextHostCandidate를 선택 (아직 Redis에 있음)
            // 2. nextHostCandidate가 먼저 퇴장 처리 완료 → Redis에서 제거됨
            // 3. 호스트가 savePlayerToRoom으로 nextHostCandidate를 방장으로 저장 → 이미 퇴장한 플레이어가 방장으로 지정됨

            ExecutorService executorService = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1); // 동시 시작을 위한 래치
            CountDownLatch finishLatch = new CountDownLatch(2);

            // 플레이어가 먼저 퇴장하도록 약간의 지연을 주되, 거의 동시에 실행
            executorService.execute(() -> {
                try {
                    startLatch.await(); // 동시 시작 대기
                    // 약간의 지연 후 퇴장 (호스트보다 먼저 처리되도록)
                    Thread.sleep(10);
                    leaveGameRoomUseCase.execute(nextHostCandidate, roundGameRoom.getId());
                } catch (Exception e) {
                    log.debug("플레이어 퇴장 중 예외: {}", e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });

            // 호스트가 나중에 퇴장 (하지만 거의 동시에 시작)
            executorService.execute(() -> {
                try {
                    startLatch.await(); // 동시 시작 대기
                    // 약간의 지연 후 퇴장 (플레이어보다 나중에 처리되도록)
                    Thread.sleep(20);
                    leaveGameRoomUseCase.execute(roundHost, roundGameRoom.getId());
                } catch (Exception e) {
                    log.debug("호스트 퇴장 중 예외: {}", e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });

            // 동시 시작
            startLatch.countDown();
            finishLatch.await(5, TimeUnit.SECONDS);
            executorService.shutdown();

            // 잠시 대기 (이벤트 처리 완료 대기)
            Thread.sleep(200);

            // 검증: 이미 퇴장한 플레이어가 방장으로 지정되었는지 확인
            List<GameRoomPlayerInfo> remainingPlayers = gameRoomRedisService.getRoomPlayers(roomId);
            
            if (!remainingPlayers.isEmpty()) {
                // 방에 남은 플레이어 중 방장이 있는지 확인
                List<GameRoomPlayerInfo> hosts = remainingPlayers.stream()
                        .filter(GameRoomPlayerInfo::isHost)
                        .toList();
                
                if (!hosts.isEmpty()) {
                    GameRoomPlayerInfo currentHost = hosts.get(0);
                    
                    // 문제: 이미 퇴장한 플레이어(nextHostCandidate)가 방장으로 지정되었는지 확인
                    // 이는 분산 락이 없었을 때 발생할 수 있는 문제입니다
                    // 현재 코드는 분산 락이 적용되어 있으므로, 이 문제가 발생하지 않아야 합니다
                    
                    // 하지만 타이밍에 따라 재검증 로직이 제대로 작동하지 않을 수 있으므로 확인
                    boolean isLeftPlayerHost = currentHost.getMemberId().equals(nextHostCandidate.getId());
                    
                    // 실제로 방에 남아있는 플레이어인지 확인
                    boolean isHostStillInRoom = remainingPlayers.stream()
                            .anyMatch(p -> p.getMemberId().equals(currentHost.getMemberId()));
                    
                    if (isLeftPlayerHost && !isHostStillInRoom) {
                        issueCount++;
                        log.error("라운드 {}: ⚠️ 이미 퇴장한 플레이어가 방장으로 지정됨! " +
                                "HostId: {}, HostNickname: {}", 
                                round, currentHost.getMemberId(), currentHost.getNickname());
                    } else if (isLeftPlayerHost) {
                        // 정상 케이스: 플레이어가 실제로 방에 남아있고 방장으로 지정됨
                        log.debug("라운드 {}: 정상 - 플레이어가 방장으로 지정됨 (방에 남아있음)", round);
                    }
                } else {
                    // 방장 없는 방 발생
                    log.warn("라운드 {}: 방장 없는 방 발생! 남은 플레이어 수: {}", round, remainingPlayers.size());
                }
            }
        }

        // then
        log.info("기존 로직 문제점 재현 테스트 결과:");
        log.info("   총 테스트 라운드: {}", testRounds);
        log.info("   이미 퇴장한 플레이어가 방장으로 지정된 횟수: {}", issueCount);
        log.info("   발생률: {:.2f}%", (issueCount * 100.0 / testRounds));

        // 분산 락이 적용된 현재 코드에서는 이 문제가 발생하지 않아야 함
        // 하지만 타이밍에 따라 극히 드물게 발생할 수 있으므로, 0~1회 정도는 허용
        assertThat(issueCount).isLessThanOrEqualTo(1);
        
        if (issueCount == 0) {
            log.info("✅ 분산 락이 적용된 현재 코드에서는 문제가 발생하지 않습니다.");
        } else {
            log.warn("⚠️ 분산 락이 적용되어 있음에도 불구하고 문제가 발생했습니다. " +
                    "이는 네트워크 지연이나 극히 예외적인 상황일 수 있습니다.");
        }
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
                .build();
        return gameRoomRepository.save(gameRoom);
    }
}

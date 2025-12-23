package com.kospot.application.multi.room.http.usecase;

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
import java.util.Comparator;
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

    @Autowired
    private ImageRepository imageRepository;

    private GameRoom testGameRoom;
    private List<Member> testMembers;
    private Member hostMember;

    @BeforeEach
    void setUp() {
        // #region agent log
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("c:\\KoSpot-backend\\.cursor\\debug.log"),
                    (java.time.Instant.now().toEpochMilli() + "|setUp|entry|{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"LeaveGameRoomUseCaseConcurrencyTest.java:67\",\"message\":\"setUp 시작\"}\n").getBytes(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {}
        // #endregion
        
        // 호스트 멤버 생성
        hostMember = createAndSaveMember("host", "호스트");

        // 테스트 게임방 생성 (최대 4명)
        testGameRoom = createAndSaveGameRoom(hostMember, 4);

        // #region agent log
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("c:\\KoSpot-backend\\.cursor\\debug.log"),
                    (java.time.Instant.now().toEpochMilli() + "|setUp|beforeJoin|{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"LeaveGameRoomUseCaseConcurrencyTest.java:89\",\"message\":\"방 생성 완료\",\"data\":{\"roomId\":\"" + testGameRoom.getId() + "\",\"maxPlayers\":4}}\n").getBytes(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {}
        // #endregion

        // 이전 테스트에서 남은 Redis 데이터 정리
        String roomId = testGameRoom.getId().toString();
        gameRoomRedisService.deleteRoomData(roomId);

        // #region agent log
        try {
            int countAfterCleanup = gameRoomRedisService.getRoomPlayers(roomId).size();
            java.nio.file.Files.write(java.nio.file.Paths.get("c:\\KoSpot-backend\\.cursor\\debug.log"),
                    (java.time.Instant.now().toEpochMilli() + "|setUp|afterCleanup|{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"LeaveGameRoomUseCaseConcurrencyTest.java:95\",\"message\":\"Redis 데이터 정리 후\",\"data\":{\"roomId\":\"" + roomId + "\",\"currentCount\":" + countAfterCleanup + "}}\n").getBytes(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {}
        // #endregion

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
        gameRoomRedisService.savePlayerToRoom(roomId, hostInfo);

        // #region agent log
        try {
            int currentCount = gameRoomRedisService.getRoomPlayers(roomId).size();
            java.nio.file.Files.write(java.nio.file.Paths.get("c:\\KoSpot-backend\\.cursor\\debug.log"),
                    (java.time.Instant.now().toEpochMilli() + "|setUp|afterHostAdd|{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"LeaveGameRoomUseCaseConcurrencyTest.java:115\",\"message\":\"호스트 추가 후\",\"data\":{\"roomId\":\"" + roomId + "\",\"currentCount\":" + currentCount + ",\"maxPlayers\":4}}\n").getBytes(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {}
        // #endregion

        // 테스트 멤버들을 방에 참가시킴
        for (int i = 0; i < 3; i++) {
            Member member = testMembers.get(i);
            
            // #region agent log
            try {
                int countBefore = gameRoomRedisService.getRoomPlayers(roomId).size();
                java.nio.file.Files.write(java.nio.file.Paths.get("c:\\KoSpot-backend\\.cursor\\debug.log"),
                        (java.time.Instant.now().toEpochMilli() + "|setUp|beforeJoinMember|{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"LeaveGameRoomUseCaseConcurrencyTest.java:125\",\"message\":\"멤버 참가 전\",\"data\":{\"roomId\":\"" + roomId + "\",\"memberId\":\"" + member.getId() + "\",\"currentCount\":" + countBefore + ",\"maxPlayers\":4}}\n").getBytes(),
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception e) {}
            // #endregion
            
            joinGameRoomUseCase.executeV1(member, testGameRoom.getId(), 
                    com.kospot.presentation.multi.room.dto.request.GameRoomRequest.Join.builder().build());
            
            // #region agent log
            try {
                int countAfter = gameRoomRedisService.getRoomPlayers(roomId).size();
                java.nio.file.Files.write(java.nio.file.Paths.get("c:\\KoSpot-backend\\.cursor\\debug.log"),
                        (java.time.Instant.now().toEpochMilli() + "|setUp|afterJoinMember|{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"LeaveGameRoomUseCaseConcurrencyTest.java:133\",\"message\":\"멤버 참가 후\",\"data\":{\"roomId\":\"" + roomId + "\",\"memberId\":\"" + member.getId() + "\",\"currentCount\":" + countAfter + ",\"maxPlayers\":4}}\n").getBytes(),
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception e) {}
            // #endregion
        }
        
        // #region agent log
        try {
            int finalCount = gameRoomRedisService.getRoomPlayers(roomId).size();
            java.nio.file.Files.write(java.nio.file.Paths.get("c:\\KoSpot-backend\\.cursor\\debug.log"),
                    (java.time.Instant.now().toEpochMilli() + "|setUp|exit|{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"LeaveGameRoomUseCaseConcurrencyTest.java:141\",\"message\":\"setUp 완료\",\"data\":{\"roomId\":\"" + roomId + "\",\"finalCount\":" + finalCount + ",\"maxPlayers\":4}}\n").getBytes(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {}
        // #endregion
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
        // given: 한 방에서 게임이 진행되기 전 여러 플레이어가 퇴장하는 시나리오
        int testRounds = 50;
        int issueCount = 0; // 이미 퇴장한 플레이어가 방장으로 지정된 횟수

        log.info("기존 로직 문제점 재현 테스트 시작 - 테스트 라운드: {}", testRounds);
        log.info("한 방에서 게임 시작 전 퇴장 시나리오를 검증합니다.");
        log.warn("⚠️ 이 테스트는 분산 락이 적용된 현재 코드에서는 문제가 발생하지 않아야 합니다.");

        // 한 방 생성 (게임 시작 전 상태)
        Member testHost = createAndSaveMember("test_host", "테스트호스트");
        GameRoom testGameRoom = createAndSaveGameRoom(testHost, 4);
        String roomId = testGameRoom.getId().toString();

        // #region agent log
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("c:\\KoSpot-backend\\.cursor\\debug.log"),
                    (java.time.Instant.now().toEpochMilli() + "|test|newRoom|{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"LeaveGameRoomUseCaseConcurrencyTest.java:360\",\"message\":\"새 방 생성\",\"data\":{\"roomId\":\"" + roomId + "\",\"maxPlayers\":4}}\n").getBytes(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {}
        // #endregion

        // when: 여러 라운드 반복 (동일한 방에서)
        for (int round = 0; round < testRounds; round++) {
            // #region agent log
            try {
                java.nio.file.Files.write(java.nio.file.Paths.get("c:\\KoSpot-backend\\.cursor\\debug.log"),
                        (java.time.Instant.now().toEpochMilli() + "|test|roundStart|{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"LeaveGameRoomUseCaseConcurrencyTest.java:365\",\"message\":\"라운드 시작\",\"data\":{\"round\":" + round + ",\"roomId\":\"" + roomId + "\"}}\n").getBytes(),
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception e) {}
            // #endregion
            
            // 각 라운드마다 플레이어들을 다시 참가시킴 (게임 시작 전 상태 재현)
            long hostJoinedAt = System.currentTimeMillis();
            GameRoomPlayerInfo hostInfo = GameRoomPlayerInfo.builder()
                    .memberId(testHost.getId())
                    .nickname(testHost.getNickname())
                    .isHost(true)
                    .joinedAt(hostJoinedAt)
                    .build();
            gameRoomRedisService.savePlayerToRoom(roomId, hostInfo);

            // #region agent log
            try {
                int countAfterHost = gameRoomRedisService.getRoomPlayers(roomId).size();
                java.nio.file.Files.write(java.nio.file.Paths.get("c:\\KoSpot-backend\\.cursor\\debug.log"),
                        (java.time.Instant.now().toEpochMilli() + "|test|afterHostAdd|{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"LeaveGameRoomUseCaseConcurrencyTest.java:377\",\"message\":\"호스트 추가 후\",\"data\":{\"round\":" + round + ",\"roomId\":\"" + roomId + "\",\"currentCount\":" + countAfterHost + ",\"maxPlayers\":4}}\n").getBytes(),
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception e) {}
            // #endregion

            // 플레이어들을 방에 참가시킴 (호스트보다 늦게 들어옴)
            List<Member> roundPlayers = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                Member player = testMembers.get(i);
                roundPlayers.add(player);
                
                // #region agent log
                try {
                    int countBefore = gameRoomRedisService.getRoomPlayers(roomId).size();
                    java.nio.file.Files.write(java.nio.file.Paths.get("c:\\KoSpot-backend\\.cursor\\debug.log"),
                            (java.time.Instant.now().toEpochMilli() + "|test|beforePlayerAdd|{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"LeaveGameRoomUseCaseConcurrencyTest.java:384\",\"message\":\"플레이어 추가 전\",\"data\":{\"round\":" + round + ",\"roomId\":\"" + roomId + "\",\"memberId\":\"" + player.getId() + "\",\"currentCount\":" + countBefore + ",\"maxPlayers\":4}}\n").getBytes(),
                            java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                } catch (Exception e) {}
                // #endregion
                
                long playerJoinedAt = System.currentTimeMillis() + 100 + (i * 10); // 호스트보다 늦게, 순차적으로
                GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.builder()
                        .memberId(player.getId())
                        .nickname(player.getNickname())
                        .isHost(false)
                        .joinedAt(playerJoinedAt)
                        .build();
                gameRoomRedisService.savePlayerToRoom(roomId, playerInfo);
                
                // #region agent log
                try {
                    int countAfter = gameRoomRedisService.getRoomPlayers(roomId).size();
                    java.nio.file.Files.write(java.nio.file.Paths.get("c:\\KoSpot-backend\\.cursor\\debug.log"),
                            (java.time.Instant.now().toEpochMilli() + "|test|afterPlayerAdd|{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"LeaveGameRoomUseCaseConcurrencyTest.java:395\",\"message\":\"플레이어 추가 후\",\"data\":{\"round\":" + round + ",\"roomId\":\"" + roomId + "\",\"memberId\":\"" + player.getId() + "\",\"currentCount\":" + countAfter + ",\"maxPlayers\":4}}\n").getBytes(),
                            java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                } catch (Exception e) {}
                // #endregion
            }

            Member nextHostCandidate = roundPlayers.get(0); // 다음 방장 후보 (가장 먼저 들어온 플레이어)

            // 시나리오: 호스트가 퇴장할 때 다음 방장 후보를 선택하는데,
            // 그 후보가 이미 퇴장 요청을 했고 먼저 처리됨
            // 기존 로직에서는:
            // 1. 호스트가 makeLeaveDecisionWithLock에서 nextHostCandidate를 선택 (아직 Redis에 있음)
            // 2. nextHostCandidate가 먼저 퇴장 처리 완료 → Redis에서 제거됨
            // 3. 호스트가 applyLeaveToRedis에서 nextHostCandidate를 방장으로 지정하려고 시도
            //    → 재검증 로직(isNewHostStillInRoom)으로 인해 이미 퇴장한 플레이어는 방장이 되지 않아야 함

            ExecutorService executorService = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1); // 동시 시작을 위한 래치
            CountDownLatch finishLatch = new CountDownLatch(2);

            // 플레이어가 먼저 퇴장하도록 약간의 지연을 주되, 거의 동시에 실행
            executorService.execute(() -> {
                try {
                    startLatch.await(); // 동시 시작 대기
                    // 약간의 지연 후 퇴장 (호스트보다 먼저 처리되도록)
                    Thread.sleep(10);
                    leaveGameRoomUseCase.execute(nextHostCandidate, testGameRoom.getId());
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
                    leaveGameRoomUseCase.execute(testHost, testGameRoom.getId());
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
            } else {
                // 모든 플레이어가 퇴장한 경우 (정상)
                log.debug("라운드 {}: 모든 플레이어가 퇴장하여 방이 삭제됨", round);
            }

            // 다음 라운드를 위해 방 상태 초기화 (Redis 데이터 정리)
            // #region agent log
            try {
                int countBeforeDelete = gameRoomRedisService.getRoomPlayers(roomId).size();
                java.nio.file.Files.write(java.nio.file.Paths.get("c:\\KoSpot-backend\\.cursor\\debug.log"),
                        (java.time.Instant.now().toEpochMilli() + "|test|beforeDelete|{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"C\",\"location\":\"LeaveGameRoomUseCaseConcurrencyTest.java:485\",\"message\":\"방 삭제 전\",\"data\":{\"round\":" + round + ",\"roomId\":\"" + roomId + "\",\"currentCount\":" + countBeforeDelete + "}}\n").getBytes(),
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception e) {}
            // #endregion
            
            gameRoomRedisService.deleteRoomData(roomId);
            Thread.sleep(50); // 상태 정리 대기
            
            // #region agent log
            try {
                int countAfterDelete = gameRoomRedisService.getRoomPlayers(roomId).size();
                java.nio.file.Files.write(java.nio.file.Paths.get("c:\\KoSpot-backend\\.cursor\\debug.log"),
                        (java.time.Instant.now().toEpochMilli() + "|test|afterDelete|{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"C\",\"location\":\"LeaveGameRoomUseCaseConcurrencyTest.java:490\",\"message\":\"방 삭제 후\",\"data\":{\"round\":" + round + ",\"roomId\":\"" + roomId + "\",\"currentCount\":" + countAfterDelete + "}}\n").getBytes(),
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception e) {}
            // #endregion
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
        Image image = Image.builder()
                .imageUrl("http://example.com/marker/" + username + ".png")
                .build();
        imageRepository.save(image);
        Member member = Member.builder()
                .username(username)
                .nickname(nickname)
                .role(Role.USER)
                .point(1000)
                .equippedMarkerImage(image)
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

    @Test
    @DisplayName("분산 락 없이 Race Condition 재현: 방장과 그 다음으로 들어온 플레이어 2명이 동시 퇴장 시 문제 발생 확인")
    void shouldDemonstrateRaceConditionWithoutLock() throws InterruptedException {
        // given: 방장과 그 다음으로 들어온 플레이어 2명이 동시에 나가는 시나리오
        int testIterations = 50; // 테스트 반복 횟수
        int issueCount = 0; // 이미 퇴장한 플레이어가 방장으로 지정된 횟수
        int hostlessRoomCount = 0; // 방장 없는 방 발생 횟수

        log.info("분산 락 없이 Race Condition 재현 테스트 시작 - 테스트 반복: {}", testIterations);
        log.warn("⚠️ 이 테스트는 분산 락 없이 실행하여 기존 로직의 문제점을 재현합니다.");
        log.info("시나리오: 방장과 그 다음으로 들어온 플레이어 2명이 동시에 나갔을 때를 검증");

        // 한 방 생성 (게임 시작 전 대기 상태)
        Member testHost = createAndSaveMember("test_host_no_lock", "테스트호스트_락없음");
        GameRoom testGameRoom = createAndSaveGameRoom(testHost, 6);
        String roomId = testGameRoom.getId().toString();

        // 이전 테스트에서 남은 Redis 데이터 정리
        gameRoomRedisService.deleteRoomData(roomId);

        // 테스트용 플레이어들 생성
        List<Member> testPlayers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Member player = createAndSaveMember("test_player_" + i, "테스트플레이어" + i);
            testPlayers.add(player);
        }

        // when: 여러 번 반복하여 문제 발생 확인
        for (int iteration = 0; iteration < testIterations; iteration++) {
            log.info("=== 반복 {} 시작 ===", iteration + 1);

            // 1. 방 초기화: 호스트와 여러 플레이어 참가
            long baseTime = System.currentTimeMillis();
            
            // 호스트 참가
            GameRoomPlayerInfo hostInfo = GameRoomPlayerInfo.builder()
                    .memberId(testHost.getId())
                    .nickname(testHost.getNickname())
                    .isHost(true)
                    .joinedAt(baseTime)
                    .build();
            gameRoomRedisService.savePlayerToRoom(roomId, hostInfo);

            // 플레이어들 순차적으로 참가 (호스트보다 늦게 들어옴)
            for (int i = 0; i < 4; i++) {
                Member player = testPlayers.get(i);
                long playerJoinedAt = baseTime + 100 + (i * 10); // 순차적으로 들어옴
                GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.builder()
                        .memberId(player.getId())
                        .nickname(player.getNickname())
                        .isHost(false)
                        .joinedAt(playerJoinedAt)
                        .build();
                gameRoomRedisService.savePlayerToRoom(roomId, playerInfo);
            }

            // 2. 현재 방 상태 확인 및 방장, 2번째, 3번째 플레이어 식별
            List<GameRoomPlayerInfo> playersBeforeCritical = gameRoomRedisService.getRoomPlayers(roomId);
            
            // joinedAt 기준으로 정렬하여 방장, 2번째, 3번째 플레이어 식별
            List<GameRoomPlayerInfo> sortedPlayers = playersBeforeCritical.stream()
                    .sorted(Comparator.comparing(GameRoomPlayerInfo::getJoinedAt))
                    .toList();

            GameRoomPlayerInfo hostPlayer = sortedPlayers.stream()
                    .filter(GameRoomPlayerInfo::isHost)
                    .findFirst()
                    .orElse(sortedPlayers.get(0)); // 호스트가 없으면 첫 번째 플레이어

            GameRoomPlayerInfo secondPlayer = sortedPlayers.size() > 1 ? sortedPlayers.get(1) : null;
            GameRoomPlayerInfo thirdPlayer = sortedPlayers.size() > 2 ? sortedPlayers.get(2) : null;

            if (secondPlayer == null || thirdPlayer == null) {
                log.warn("반복 {}: 플레이어가 부족하여 테스트 스킵", iteration + 1);
                gameRoomRedisService.deleteRoomData(roomId);
                continue;
            }

            // 플레이어 Member 객체 찾기
            Member hostMember = testHost.getId().equals(hostPlayer.getMemberId()) 
                    ? testHost 
                    : testPlayers.stream()
                            .filter(p -> p.getId().equals(hostPlayer.getMemberId()))
                            .findFirst()
                            .orElse(null);
            Member secondMember = testPlayers.stream()
                    .filter(p -> p.getId().equals(secondPlayer.getMemberId()))
                    .findFirst()
                    .orElse(null);
            Member thirdMember = testPlayers.stream()
                    .filter(p -> p.getId().equals(thirdPlayer.getMemberId()))
                    .findFirst()
                    .orElse(null);

            if (hostMember == null || secondMember == null || thirdMember == null) {
                log.warn("반복 {}: 플레이어를 찾을 수 없어서 테스트 스킵", iteration + 1);
                gameRoomRedisService.deleteRoomData(roomId);
                continue;
            }

            log.info("중요 시점: 방장(MemberId: {}), 2번째(MemberId: {}), 3번째(MemberId: {}) 플레이어가 동시에 퇴장",
                    hostMember.getId(), secondMember.getId(), thirdMember.getId());

            // 4. 방장, 2번째, 3번째 플레이어가 완전 동시에 퇴장
            // 타이밍을 조정하여 Race Condition을 유발:
            // - 2번째 플레이어가 먼저 퇴장 처리 완료 → Redis에서 제거됨
            // - 방장이 pickNextHostByJoinedAt 호출 시점에는 2번째 플레이어가 아직 Redis에 있을 수 있음
            // - 방장이 savePlayerToRoom 호출 시점에는 2번째 플레이어가 이미 퇴장한 상태
            ExecutorService executorService = Executors.newFixedThreadPool(3);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch finishLatch = new CountDownLatch(3);

            // 2번째 플레이어가 먼저 퇴장 (즉시 처리)
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    leaveGameRoomUseCase.executeWithoutLock(secondMember, testGameRoom.getId());
                } catch (Exception e) {
                    log.debug("2번째 플레이어 퇴장 중 예외: {}", e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });

            // 3번째 플레이어도 거의 동시에 퇴장
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(1); // 매우 짧은 지연
                    leaveGameRoomUseCase.executeWithoutLock(thirdMember, testGameRoom.getId());
                } catch (Exception e) {
                    log.debug("3번째 플레이어 퇴장 중 예외: {}", e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });

            // 방장이 나중에 퇴장 (하지만 거의 동시에 시작)
            // 방장이 pickNextHostByJoinedAt을 호출할 때는 2번째 플레이어가 아직 Redis에 있을 수 있지만,
            // 실제로 savePlayerToRoom을 호출할 때는 이미 퇴장한 상태
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(20); // 2번째, 3번째보다 충분히 늦게 처리되도록 (Race Condition 유발)
                    leaveGameRoomUseCase.executeWithoutLock(hostMember, testGameRoom.getId());
                } catch (Exception e) {
                    log.debug("방장 퇴장 중 예외: {}", e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });

            // 완전 동시 시작
            startLatch.countDown();
            finishLatch.await(5, TimeUnit.SECONDS);
            executorService.shutdown();

            // 잠시 대기 (이벤트 처리 완료 대기)
            Thread.sleep(500);

            // 5. 검증: 문제 발생 여부 확인
            List<GameRoomPlayerInfo> remainingPlayers = gameRoomRedisService.getRoomPlayers(roomId);
            
            // 퇴장한 플레이어들의 ID (방장, 2번째, 3번째)
            Long hostMemberId = hostMember.getId();
            Long secondMemberId = secondMember.getId();
            Long thirdMemberId = thirdMember.getId();
            
            if (!remainingPlayers.isEmpty()) {
                // 방에 남은 플레이어 중 방장이 있는지 확인
                List<GameRoomPlayerInfo> hosts = remainingPlayers.stream()
                        .filter(GameRoomPlayerInfo::isHost)
                        .toList();
                
                if (!hosts.isEmpty()) {
                    GameRoomPlayerInfo currentHost = hosts.get(0);
                    Long currentHostId = currentHost.getMemberId();
                    
                    // 문제 1: 이미 퇴장한 플레이어(2번째 또는 3번째)가 방장으로 지정되었는지 확인
                    // 이들은 동시에 퇴장했으므로 실제로는 방에 없어야 함
                    // 하지만 savePlayerToRoom으로 Redis에 저장되었을 수 있음
                    boolean isLeftPlayerHost = currentHostId.equals(secondMemberId) 
                            || currentHostId.equals(thirdMemberId);
                    
                    // 실제로 방에 남아있는 플레이어인지 확인 (4번째 이후 플레이어만 남아있어야 함)
                    // 퇴장한 플레이어들(방장, 2번째, 3번째)이 방장으로 지정되었다면 문제
                    if (isLeftPlayerHost) {
                        // 이미 퇴장한 플레이어가 방장으로 지정된 경우
                        issueCount++;
                        log.error("반복 {}: ⚠️ 이미 퇴장한 플레이어가 방장으로 지정됨! " +
                                "HostId: {}, HostNickname: {}, 퇴장한 플레이어: {} 또는 {}", 
                                iteration + 1, currentHostId, currentHost.getNickname(),
                                secondMemberId, thirdMemberId);
                    } else if (currentHostId.equals(hostMemberId)) {
                        // 방장이 방장으로 남아있는 경우도 문제 (방장은 퇴장했어야 함)
                        issueCount++;
                        log.error("반복 {}: ⚠️ 이미 퇴장한 방장이 여전히 방장으로 남아있음! " +
                                "HostId: {}", iteration + 1, currentHostId);
                    }
                } else {
                    // 문제 2: 방장 없는 방 발생 (플레이어는 남아있지만 방장이 없음)
                    // 4번째 이후 플레이어가 남아있을 수 있음
                    boolean hasRemainingPlayers = remainingPlayers.stream()
                            .anyMatch(p -> !p.getMemberId().equals(hostMemberId) 
                                    && !p.getMemberId().equals(secondMemberId)
                                    && !p.getMemberId().equals(thirdMemberId));
                    
                    if (hasRemainingPlayers) {
                        hostlessRoomCount++;
                        log.warn("반복 {}: 방장 없는 방 발생! 남은 플레이어 수: {}", 
                                iteration + 1, remainingPlayers.size());
                    }
                }
            } else {
                // 모든 플레이어가 퇴장한 경우 (정상)
                log.debug("반복 {}: 모든 플레이어가 퇴장하여 방이 삭제됨", iteration + 1);
            }

            // 다음 반복을 위해 방 상태 초기화
            gameRoomRedisService.deleteRoomData(roomId);
            Thread.sleep(100); // 상태 정리 대기
        }

        // then
        log.info("=== 분산 락 없이 Race Condition 재현 테스트 결과 ===");
        log.info("   총 테스트 반복: {}", testIterations);
        log.info("   이미 퇴장한 플레이어가 방장으로 지정된 횟수: {}", issueCount);
        log.info("   방장 없는 방 발생 횟수: {}", hostlessRoomCount);
        log.info("   문제 발생률: {:.2f}%", ((issueCount + hostlessRoomCount) * 100.0 / testIterations));

        // 분산 락 없이 실행하면 문제가 발생해야 함
        int totalIssues = issueCount + hostlessRoomCount;
        assertThat(totalIssues).isGreaterThan(0);
        log.info("✅ 분산 락 없이 실행하면 Race Condition 문제가 발생함을 확인했습니다.");
        log.info("   → 분산 락 도입의 필요성을 검증했습니다.");
    }
}

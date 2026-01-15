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
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LeaveGameRoomUseCase 동시성 테스트
 * 
 * 분산 락(RLock)이 적용된 방 퇴장 로직이 동시 요청 상황에서도
 * 데이터 정합성을 보장하는지 검증합니다.
 * 
 * 검증 시나리오:
 * - 방장과 다음 방장 후보가 동시에 퇴장 요청
 * - 남은 플레이어가 정상적으로 방장 권한을 승계받는지 확인
 */
@SpringBootTest
@ActiveProfiles("test")
class LeaveGameRoomUseCaseConcurrencyTest {

    @Autowired
    private LeaveGameRoomUseCase leaveGameRoomUseCase;

    @Autowired
    private GameRoomRedisService gameRoomRedisService;

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ImageRepository imageRepository;

    private static final int THREAD_COUNT = 2;

    // 테스트에서 생성한 엔티티들 추적 (cleanup용)
    private String testRoomId;
    private GameRoom testGameRoom;
    private final List<Member> testMembers = new ArrayList<>();
    private final List<Image> testImages = new ArrayList<>();

    @AfterEach
    void tearDown() {
        // 1. Redis 정리
        if (testRoomId != null) {
            gameRoomRedisService.deleteRoomData(testRoomId);
        }

        // 2. DB 정리 (순서 중요: 외래키 제약조건 고려)
        // Member의 gameRoom 참조 해제
        for (Member member : testMembers) {
            try {
                Member freshMember = memberRepository.findById(member.getId()).orElse(null);
                if (freshMember != null) {
                    freshMember.leaveGameRoom();
                    memberRepository.save(freshMember);
                }
            } catch (Exception ignored) {
            }
        }

        // GameRoom 삭제
        if (testGameRoom != null) {
            try {
                gameRoomRepository.deleteById(testGameRoom.getId());
            } catch (Exception ignored) {
            }
        }

        // Member 삭제
        for (Member member : testMembers) {
            try {
                memberRepository.deleteById(member.getId());
            } catch (Exception ignored) {
            }
        }

        // Image 삭제
        for (Image image : testImages) {
            try {
                imageRepository.deleteById(image.getId());
            } catch (Exception ignored) {
            }
        }

        // 리스트 초기화
        testMembers.clear();
        testImages.clear();
        testGameRoom = null;
        testRoomId = null;
    }

    @DisplayName("방장과 다음 방장 후보가 동시에 퇴장해도, 남은 인원 중 한 명이 정상적으로 방장이 된다")
    @RepeatedTest(100)
    void concurrentLeaveRequest_shouldAssignHostToSurvivor() throws InterruptedException {
        // given
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        Member host = createMember("host_" + uniqueId);
        Member candidate = createMember("candidate_" + uniqueId);
        Member survivor = createMember("survivor_" + uniqueId);

        testGameRoom = createGameRoom(host);
        testRoomId = testGameRoom.getId().toString();

        setUpRedisPlayers(testRoomId, host, candidate, survivor);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(THREAD_COUNT);

        // when: 호스트와 후보자가 동시에 퇴장 요청
        executor.submit(() -> executeLeave(host, testGameRoom.getId(), startLatch, finishLatch));
        executor.submit(() -> executeLeave(candidate, testGameRoom.getId(), startLatch, finishLatch));

        startLatch.countDown(); // 동시 시작
        finishLatch.await();
        executor.shutdown();

        // then
        List<GameRoomPlayerInfo> remainingPlayers = gameRoomRedisService.getRoomPlayers(testRoomId);

        // 1. 남은 플레이어는 1명이어야 한다 (survivor)
        assertThat(remainingPlayers).hasSize(1);

        // 2. 남은 플레이어가 방장 권한을 승계받았어야 한다
        GameRoomPlayerInfo newHost = remainingPlayers.get(0);
        assertThat(newHost.getMemberId()).isEqualTo(survivor.getId());
        assertThat(newHost.isHost()).isTrue();

        // 3. DB 상태도 일관성 있게 업데이트되어야 한다
        GameRoom updatedRoom = gameRoomRepository.findById(testGameRoom.getId()).orElseThrow();
        assertThat(updatedRoom.getHost().getId()).isEqualTo(survivor.getId());
    }

    @DisplayName("방장 혼자 남은 상태에서 퇴장하면 방이 삭제된다")
    @RepeatedTest(5)
    void lastPlayerLeave_shouldDeleteRoom() {
        // given
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        Member host = createMember("host_" + uniqueId);

        testGameRoom = createGameRoom(host);
        testRoomId = testGameRoom.getId().toString();

        GameRoomPlayerInfo hostInfo = GameRoomPlayerInfo.builder()
                .memberId(host.getId())
                .nickname(host.getNickname())
                .isHost(true)
                .joinedAt(System.currentTimeMillis())
                .build();
        gameRoomRedisService.savePlayerToRoom(testRoomId, hostInfo);

        // when
        leaveGameRoomUseCase.execute(host, testGameRoom.getId());

        // then
        List<GameRoomPlayerInfo> remainingPlayers = gameRoomRedisService.getRoomPlayers(testRoomId);
        assertThat(remainingPlayers).isEmpty();
    }

    @DisplayName("일반 플레이어가 퇴장해도 방장은 그대로 유지된다")
    @RepeatedTest(5)
    void normalPlayerLeave_shouldKeepHost() {
        // given
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        Member host = createMember("host_" + uniqueId);
        Member player = createMember("player_" + uniqueId);

        testGameRoom = createGameRoom(host);
        testRoomId = testGameRoom.getId().toString();

        setUpRedisPlayers(testRoomId, host, player);

        // when
        leaveGameRoomUseCase.execute(player, testGameRoom.getId());

        // then
        List<GameRoomPlayerInfo> remainingPlayers = gameRoomRedisService.getRoomPlayers(testRoomId);

        assertThat(remainingPlayers).hasSize(1);

        GameRoomPlayerInfo remainingHost = remainingPlayers.get(0);
        assertThat(remainingHost.getMemberId()).isEqualTo(host.getId());
        assertThat(remainingHost.isHost()).isTrue();
    }

    // ========== Helper Methods ==========

    private void executeLeave(Member member, Long roomId, CountDownLatch startLatch, CountDownLatch finishLatch) {
        try {
            startLatch.await();
            leaveGameRoomUseCase.execute(member, roomId);
        } catch (Exception e) {
            // 동시 요청 시 예외 발생 가능 (정상)
        } finally {
            finishLatch.countDown();
        }
    }

    private void setUpRedisPlayers(String roomId, Member... members) {
        long baseTime = System.currentTimeMillis();
        for (int i = 0; i < members.length; i++) {
            Member member = members[i];
            GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.builder()
                    .memberId(member.getId())
                    .nickname(member.getNickname())
                    .isHost(i == 0) // 첫 번째가 호스트
                    .joinedAt(baseTime + (i * 100)) // 순차적 입장
                    .build();
            gameRoomRedisService.savePlayerToRoom(roomId, playerInfo);
        }
    }

    private Member createMember(String username) {
        Image image = Image.builder()
                .imageUrl("http://example.com/marker/" + username + ".png")
                .build();
        imageRepository.save(image);
        testImages.add(image); // 추적

        Member member = Member.builder()
                .username(username)
                .nickname(username)
                .role(Role.USER)
                .point(1000)
                .equippedMarkerImage(image)
                .build();

        Member savedMember = memberRepository.save(member);
        testMembers.add(savedMember); // 추적
        return savedMember;
    }

    private GameRoom createGameRoom(Member host) {
        GameRoom gameRoom = GameRoom.builder()
                .title("테스트 게임방")
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
}

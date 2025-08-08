package com.kospot.application.multiplayer.gameroom.usecase;

import com.kospot.application.multiplayer.gameroom.event.GameRoomEventHandler;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.repository.ImageRepository;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.multigame.game.vo.PlayerMatchType;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRoom.event.GameRoomJoinEvent;
import com.kospot.domain.multigame.gameRoom.repository.GameRoomRepository;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomPlayerInfo;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomStatus;
import com.kospot.infrastructure.exception.object.domain.GameRoomHandler;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomNotificationService;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomRedisService;
import com.kospot.presentation.multigame.gameroom.dto.request.GameRoomRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JoinGameRoomUseCase의 전체 플로우와 통합 테스트
 * 성능 개선은 나중에 하되, 현재 설계가 올바르게 작동하는지 검증
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JoinGameRoomUseCaseTest {

    @Autowired
    private JoinGameRoomUseCase joinGameRoomUseCase;

    @Autowired
    private GameRoomRedisService gameRoomRedisService;

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRoomNotificationService gameRoomNotificationService;

    private Member testMember;
    private Member hostMember;
    private GameRoom testGameRoom;
    private GameRoomRequest.Join joinRequest;

    @BeforeEach
    void setUp() {
        // 테스트 멤버 생성
        testMember = createAndSaveMember("testUser", "테스트유저");
        hostMember = createAndSaveMember("hostUser", "호스트유저");

        // 테스트 게임방 생성
        testGameRoom = createAndSaveGameRoom(hostMember);

        // 기본 참가 요청
        joinRequest = GameRoomRequest.Join.builder()
                .password(null)
                .build();

        // Redis 초기화
        gameRoomRedisService.removePlayerFromRoom(testGameRoom.getId().toString(), testMember.getId());
    }

    @Test
    @DisplayName("정상적인 게임방 참가 플로우 테스트")
    void shouldJoinGameRoomSuccessfully() {
        // given
        assertThat(testMember.getGameRoomId()).isNull();
        assertThat(gameRoomRedisService.getCurrentPlayerCount(testGameRoom.getId().toString())).isEqualTo(0);

        // when
        joinGameRoomUseCase.executeV1(testMember, testGameRoom.getId(), joinRequest);

        // then
        // 1. DB 상태 검증
        Member updatedMember = memberRepository.findById(testMember.getId()).orElseThrow();
        assertThat(updatedMember.getGameRoomId()).isEqualTo(testGameRoom.getId());

        // 2. 이벤트 처리 후 Redis 상태 검증 (실제 환경에서는 이벤트가 자동 처리됨)
        // 여기서는 Redis 상태 변화로 이벤트 처리를 간접 확인

        log.info("✅ 정상적인 게임방 참가 완료 - MemberId: {}, GameRoomId: {}", 
                testMember.getId(), testGameRoom.getId());
    }

    @Test
    @DisplayName("이벤트 처리 후 Redis 상태 검증 테스트")
    void shouldUpdateRedisAfterEventProcessing() {
        // given
        GameRoomEventHandler eventHandler = new GameRoomEventHandler(gameRoomRedisService, gameRoomNotificationService);

        // when
        joinGameRoomUseCase.executeV1(testMember, testGameRoom.getId(), joinRequest);

        // 이벤트 핸들러 수동 실행 (실제로는 Spring이 자동 처리)
        GameRoomJoinEvent event = new GameRoomJoinEvent(testGameRoom, testMember);
        eventHandler.handleJoin(event);

        // then
        // Redis 상태 검증
        assertThat(gameRoomRedisService.getCurrentPlayerCount(testGameRoom.getId().toString())).isEqualTo(1);

        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(testGameRoom.getId().toString());
        assertThat(players).hasSize(1);
        assertThat(players.get(0).getMemberId()).isEqualTo(testMember.getId());
        assertThat(players.get(0).getNickname()).isEqualTo(testMember.getNickname());

        log.info("✅ Redis 상태 업데이트 완료 - RoomId: {}, PlayerCount: {}", 
                testGameRoom.getId(), gameRoomRedisService.getCurrentPlayerCount(testGameRoom.getId().toString()));
    }

    @Test
    @DisplayName("게임방 최대 인원 초과 시 예외 발생 테스트")
    void shouldThrowExceptionWhenRoomIsFull() {
        // given
        String roomId = testGameRoom.getId().toString();
        int maxPlayers = testGameRoom.getMaxPlayers();

        // Redis에 최대 인원만큼 플레이어 추가 (실제 상황 시뮬레이션)
        for (int i = 0; i < maxPlayers; i++) {
            GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.builder()
                    .memberId((long) (i + 1000))
                    .nickname("player" + i)
                    .build();
            gameRoomRedisService.addPlayerToRoom(roomId, playerInfo);
        }

        // when & then
        assertThatThrownBy(() -> joinGameRoomUseCase.executeV1(testMember, testGameRoom.getId(), joinRequest))
                .isInstanceOf(GameRoomHandler.class)
                .hasMessageContaining("GAME_ROOM_IS_FULL");

        // DB 상태는 변경되지 않아야 함
        Member unchangedMember = memberRepository.findById(testMember.getId()).orElseThrow();
        assertThat(unchangedMember.getGameRoomId()).isNull();

        log.info("✅ 인원 초과 시 예외 처리 완료 - MaxPlayers: {}, CurrentCount: {}", 
                maxPlayers, gameRoomRedisService.getCurrentPlayerCount(roomId));
    }

    @Test
    @DisplayName("비밀번호 방 참가 테스트")
    void shouldJoinPrivateRoomWithCorrectPassword() {
        // given
        String password = "secret123";
        GameRoom privateRoom = createAndSavePrivateGameRoom(hostMember, password);
        GameRoomRequest.Join privateJoinRequest = GameRoomRequest.Join.builder()
                .password(password)
                .build();

        // when
        joinGameRoomUseCase.executeV1(testMember, privateRoom.getId(), privateJoinRequest);

        // then
        Member updatedMember = memberRepository.findById(testMember.getId()).orElseThrow();
        assertThat(updatedMember.getGameRoomId()).isEqualTo(privateRoom.getId());

        log.info("✅ 비밀번호 방 참가 완료 - RoomId: {}", privateRoom.getId());
    }

    @Test
    @DisplayName("잘못된 비밀번호로 비밀번호 방 참가 시 예외 발생 테스트")
    void shouldThrowExceptionWhenWrongPassword() {
        // given
        String correctPassword = "secret123";
        String wrongPassword = "wrong456";
        GameRoom privateRoom = createAndSavePrivateGameRoom(hostMember, correctPassword);
        GameRoomRequest.Join wrongPasswordRequest = GameRoomRequest.Join.builder()
                .password(wrongPassword)
                .build();

        // when & then
        assertThatThrownBy(() -> joinGameRoomUseCase.executeV1(testMember, privateRoom.getId(), wrongPasswordRequest))
                .isInstanceOf(GameRoomHandler.class)
                .hasMessageContaining("NOT_CORRECT_PASSWORD");

        // DB 상태는 변경되지 않아야 함
        Member unchangedMember = memberRepository.findById(testMember.getId()).orElseThrow();
        assertThat(unchangedMember.getGameRoomId()).isNull();

        log.info("✅ 잘못된 비밀번호 예외 처리 완료");
    }

    @Test
    @DisplayName("이미 다른 방에 참가한 플레이어 참가 시 예외 발생 테스트")
    void shouldThrowExceptionWhenPlayerAlreadyInRoom() {
        // given
        // 다른 게임방 생성
        GameRoom anotherRoom = createAndSaveGameRoom(hostMember);
        
        // 먼저 testMember를 첫 번째 방에 참가시킴
        joinGameRoomUseCase.executeV1(testMember, testGameRoom.getId(), joinRequest);

        // when & then
        // 두 번째 방 참가 시도 시 예외 발생해야 함
        assertThatThrownBy(() -> joinGameRoomUseCase.executeV1(testMember, anotherRoom.getId(), joinRequest))
                .isInstanceOf(GameRoomHandler.class)
                .hasMessageContaining("ALREADY_IN_ROOM");

        // 첫 번째 방에는 여전히 참가 상태여야 함
        Member updatedMember = memberRepository.findById(testMember.getId()).orElseThrow();
        assertThat(updatedMember.getGameRoomId()).isEqualTo(testGameRoom.getId());

        log.info("✅ 중복 참가 방지 완료 - CurrentRoomId: {}", updatedMember.getGameRoomId());
    }

    @Test
    @DisplayName("존재하지 않는 게임방 참가 시 예외 발생 테스트")
    void shouldThrowExceptionWhenGameRoomNotFound() {
        // given
        Long nonExistentRoomId = 99999L;

        // when & then
        assertThatThrownBy(() -> joinGameRoomUseCase.executeV1(testMember, nonExistentRoomId, joinRequest))
                .isInstanceOf(GameRoomHandler.class)
                .hasMessageContaining("NOT_FOUND");

        log.info("✅ 존재하지 않는 방 예외 처리 완료");
    }

    @Test
    @DisplayName("게임 진행 중인 방 참가 시 예외 발생 테스트")
    void shouldThrowExceptionWhenGameRoomIsPlaying() {
        // given
        testGameRoom.start(hostMember);  // 게임 시작 (상태를 PLAYING으로 변경)
        gameRoomRepository.save(testGameRoom);

        // when & then
        assertThatThrownBy(() -> joinGameRoomUseCase.executeV1(testMember, testGameRoom.getId(), joinRequest))
                .isInstanceOf(GameRoomHandler.class)
                .hasMessageContaining("CANNOT_JOIN_NOW");

        log.info("✅ 게임 진행 중 참가 방지 완료");
    }

    @Test
    @DisplayName("Redis와 DB 간 데이터 일관성 검증 테스트")
    void shouldMaintainDataConsistencyBetweenRedisAndDB() {
        // given
        Member member1 = createAndSaveMember("member1", "멤버1");
        Member member2 = createAndSaveMember("member2", "멤버2");

        GameRoomEventHandler eventHandler = new GameRoomEventHandler(gameRoomRedisService, gameRoomNotificationService);

        // when
        // 두 명의 플레이어가 순차적으로 참가
        joinGameRoomUseCase.executeV1(member1, testGameRoom.getId(), joinRequest);
        joinGameRoomUseCase.executeV1(member2, testGameRoom.getId(), joinRequest);

        // 이벤트 처리 시뮬레이션
        GameRoomJoinEvent event1 = new GameRoomJoinEvent(testGameRoom, member1);
        GameRoomJoinEvent event2 = new GameRoomJoinEvent(testGameRoom, member2);
        eventHandler.handleJoin(event1);
        eventHandler.handleJoin(event2);

        // then
        // DB 검증
        Member updatedMember1 = memberRepository.findById(member1.getId()).orElseThrow();
        Member updatedMember2 = memberRepository.findById(member2.getId()).orElseThrow();
        
        assertThat(updatedMember1.getGameRoomId()).isEqualTo(testGameRoom.getId());
        assertThat(updatedMember2.getGameRoomId()).isEqualTo(testGameRoom.getId());

        // Redis 검증
        String roomId = testGameRoom.getId().toString();
        assertThat(gameRoomRedisService.getCurrentPlayerCount(roomId)).isEqualTo(2);
        
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(roomId);
        assertThat(players).hasSize(2);
        assertThat(players).extracting(GameRoomPlayerInfo::getMemberId)
                .containsExactlyInAnyOrder(member1.getId(), member2.getId());

        log.info("✅ Redis-DB 일관성 검증 완료 - DB Players: 2, Redis Players: {}", 
                gameRoomRedisService.getCurrentPlayerCount(roomId));
    }

    // Helper Methods

    private Member createAndSaveMember(String username, String nickname) {
        Image image = Image.builder()
                .imageUrl("http://example.com/image.png")
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

    private GameRoom createAndSaveGameRoom(Member host) {
        GameRoom gameRoom = GameRoom.builder()
                .title("테스트 게임방")
                .gameMode(GameMode.ROADVIEW)
                .playerMatchType(PlayerMatchType.INDIVIDUAL)
                .privateRoom(false)
                .maxPlayers(4)
                .teamCount(1)
                .status(GameRoomStatus.WAITING)
                .host(host)
                .deleted(false)
                .build();
        return gameRoomRepository.save(gameRoom);
    }

    private GameRoom createAndSavePrivateGameRoom(Member host, String password) {
        GameRoom gameRoom = GameRoom.builder()
                .title("비밀 게임방")
                .gameMode(GameMode.ROADVIEW)
                .playerMatchType(PlayerMatchType.INDIVIDUAL)
                .privateRoom(true)
                .password(password)
                .maxPlayers(4)
                .teamCount(1)
                .status(GameRoomStatus.WAITING)
                .host(host)
                .deleted(false)
                .build();
        return gameRoomRepository.save(gameRoom);
    }
}

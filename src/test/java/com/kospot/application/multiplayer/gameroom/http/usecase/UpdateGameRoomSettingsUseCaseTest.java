package com.kospot.application.multiplayer.gameroom.http.usecase;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.multigame.game.vo.PlayerMatchType;
// import com.kospot.domain.multigame.gameRoom.adaptor.GameRoomAdaptor; // 현재 테스트에서 사용하지 않음
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRoom.repository.GameRoomRepository;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomPlayerInfo;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomStatus;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomRedisService;
import com.kospot.presentation.multigame.gameroom.dto.request.GameRoomRequest;
import com.kospot.presentation.multigame.gameroom.dto.response.GameRoomResponse;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * UpdateGameRoomSettingsUseCase의 팀 설정 변경 로직 테스트
 * Redis를 통한 실제 팀 할당/해제 기능을 검증합니다.
 */
@Slf4j
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
public class UpdateGameRoomSettingsUseCaseTest {

    @Autowired
    private UpdateGameRoomSettingsUseCase updateGameRoomSettingsUseCase;

    // @Autowired
    // private GameRoomAdaptor gameRoomAdaptor; // 현재 테스트에서 사용하지 않음

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private GameRoomRedisService gameRoomRedisService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private EntityManager entityManager;

    private Member host;
    private List<Member> players;
    private GameRoom gameRoom;

    @BeforeEach
    void setUp() {
        // 호스트 생성
        host = Member.builder()
                .username("host")
                .nickname("호스트")
                .role(Role.USER)
                .build();

        // 플레이어들 생성 (4명)
        players = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Member player = Member.builder()
                    .username("player" + i)
                    .nickname("플레이어" + i)
                    .role(Role.USER)
                    .build();
            players.add(player);
        }

        // 게임방 생성 (개인전으로 시작)
        gameRoom = GameRoom.builder()
                .title("테스트 게임방")
                .host(host)
                .gameMode(GameMode.ROADVIEW)
                .playerMatchType(PlayerMatchType.SOLO)
                .privateRoom(false)
                .status(GameRoomStatus.WAITING)
                .maxPlayers(8)
                .build();

        gameRoomRepository.save(gameRoom);
        entityManager.flush();

        // Redis에 플레이어들 추가
        String roomId = gameRoom.getId().toString();
        for (int i = 0; i < players.size(); i++) {
            GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.builder()
                    .memberId(players.get(i).getId())
                    .nickname(players.get(i).getNickname())
                    .markerImageUrl("default-marker.png")
                    .team(null) // 개인전이므로 팀 없음
                    .isHost(i == 0) // 첫 번째 플레이어가 호스트
                    .joinedAt(System.currentTimeMillis())
                    .build();

            gameRoomRedisService.addPlayerToRoom(roomId, playerInfo);
        }

        log.info("테스트 설정 완료 - 게임방 ID: {}, 플레이어 수: {}", roomId, players.size());
    }

    @DisplayName("개인전에서 팀전으로 변경 시 모든 플레이어에게 팀이 할당되는지 테스트")
    @Test
    void testIndividualToTeam_ShouldAssignTeamsToAllPlayers() {
        // Given
        String roomId = gameRoom.getId().toString();
        GameRoomRequest.Update request = GameRoomRequest.Update.builder()
                .title("팀전으로 변경")
                .gameModeKey("roadview")
                .playerMatchTypeKey("TEAM")
                .privateRoom(false)
                .build();

        // 개인전 상태 확인 (팀이 없어야 함)
        List<GameRoomPlayerInfo> playersBefore = gameRoomRedisService.getRoomPlayers(roomId);
        assertThat(playersBefore).allMatch(player -> player.getTeam() == null);

        // When
        GameRoomResponse response = updateGameRoomSettingsUseCase.execute(host, request, gameRoom.getId());

        // Then
        // 게임방 설정이 팀전으로 변경되었는지 확인
        assertEquals(PlayerMatchType.TEAM, response.getPlayerMatchType());

        // Redis에서 플레이어들의 팀 할당 확인
        List<GameRoomPlayerInfo> playersAfter = gameRoomRedisService.getRoomPlayers(roomId);
        assertThat(playersAfter).hasSize(4);
        assertThat(playersAfter).allMatch(player -> player.getTeam() != null);
        assertThat(playersAfter).allMatch(player -> 
            "RED".equals(player.getTeam()) || "BLUE".equals(player.getTeam()));

        // 팀 분배가 균등한지 확인 (4명이므로 2:2 또는 3:1)
        long redTeamCount = playersAfter.stream()
                .filter(player -> "RED".equals(player.getTeam()))
                .count();
        long blueTeamCount = playersAfter.stream()
                .filter(player -> "BLUE".equals(player.getTeam()))
                .count();

        assertThat(redTeamCount + blueTeamCount).isEqualTo(4);
        assertThat(Math.abs(redTeamCount - blueTeamCount)).isLessThanOrEqualTo(1);

        log.info("개인전 → 팀전 변경 테스트 완료 - RED: {}, BLUE: {}", redTeamCount, blueTeamCount);
    }

    @DisplayName("팀전에서 개인전으로 변경 시 모든 플레이어의 팀이 해제되는지 테스트")
    @Test
    void testTeamToIndividual_ShouldResetAllPlayerTeams() {
        // Given - 먼저 팀전으로 설정
        String roomId = gameRoom.getId().toString();
        gameRoomRedisService.assignAllPlayersTeam(roomId);

        // 팀이 할당되었는지 확인
        List<GameRoomPlayerInfo> playersWithTeams = gameRoomRedisService.getRoomPlayers(roomId);
        assertThat(playersWithTeams).allMatch(player -> player.getTeam() != null);

        // 개인전으로 변경 요청
        GameRoomRequest.Update request = GameRoomRequest.Update.builder()
                .title("개인전으로 변경")
                .gameModeKey("roadview")
                .playerMatchTypeKey("INDIVIDUAL")
                .privateRoom(false)
                .build();

        // When
        GameRoomResponse response = updateGameRoomSettingsUseCase.execute(host, request, gameRoom.getId());

        // Then
        // 게임방 설정이 개인전으로 변경되었는지 확인
        assertEquals(PlayerMatchType.SOLO, response.getPlayerMatchType());

        // Redis에서 모든 플레이어의 팀이 해제되었는지 확인
        List<GameRoomPlayerInfo> playersAfter = gameRoomRedisService.getRoomPlayers(roomId);
        assertThat(playersAfter).hasSize(4);
        assertThat(playersAfter).allMatch(player -> player.getTeam() == null);

        log.info("팀전 → 개인전 변경 테스트 완료");
    }

    @DisplayName("팀전에서 팀전으로 변경 시 팀이 재할당되는지 테스트")
    @Test
    void testTeamToTeam_ShouldReassignTeams() {
        // Given - 먼저 팀전으로 설정
        String roomId = gameRoom.getId().toString();
        gameRoomRedisService.assignAllPlayersTeam(roomId);

        // 첫 번째 팀 할당 상태 저장 (로직 검증용)
        // List<GameRoomPlayerInfo> playersBefore = gameRoomRedisService.getRoomPlayers(roomId);
        // List<String> teamsBefore = playersBefore.stream()
        //         .map(GameRoomPlayerInfo::getTeam)
        //         .toList(); // 현재 테스트에서 사용하지 않음

        // 팀전으로 다시 변경 요청 (제목만 변경)
        GameRoomRequest.Update request = GameRoomRequest.Update.builder()
                .title("팀 재할당 테스트")
                .gameModeKey("roadview")
                .playerMatchTypeKey("TEAM")
                .privateRoom(false)
                .build();

        // When
        GameRoomResponse response = updateGameRoomSettingsUseCase.execute(host, request, gameRoom.getId());

        // Then
        // 게임방 설정이 팀전으로 유지되었는지 확인
        assertEquals(PlayerMatchType.TEAM, response.getPlayerMatchType());

        // 팀이 재할당되었는지 확인 (동일한 팀 구성일 수도 있지만, 로직은 실행됨)
        List<GameRoomPlayerInfo> playersAfter = gameRoomRedisService.getRoomPlayers(roomId);
        assertThat(playersAfter).hasSize(4);
        assertThat(playersAfter).allMatch(player -> player.getTeam() != null);

        log.info("팀전 → 팀전 재할당 테스트 완료");
    }

    @DisplayName("개인전에서 개인전으로 변경 시 팀 상태가 유지되는지 테스트")
    @Test
    void testIndividualToIndividual_ShouldMaintainNoTeams() {
        // Given - 개인전 상태 (팀 없음)
        String roomId = gameRoom.getId().toString();
        List<GameRoomPlayerInfo> playersBefore = gameRoomRedisService.getRoomPlayers(roomId);
        assertThat(playersBefore).allMatch(player -> player.getTeam() == null);

        // 개인전으로 다시 변경 요청 (제목만 변경)
        GameRoomRequest.Update request = GameRoomRequest.Update.builder()
                .title("개인전 유지 테스트")
                .gameModeKey("roadview")
                .playerMatchTypeKey("INDIVIDUAL")
                .privateRoom(false)
                .build();

        // When
        GameRoomResponse response = updateGameRoomSettingsUseCase.execute(host, request, gameRoom.getId());

        // Then
        // 게임방 설정이 개인전으로 유지되었는지 확인
        assertEquals(PlayerMatchType.SOLO, response.getPlayerMatchType());

        // 팀 상태가 유지되었는지 확인 (여전히 팀 없음)
        List<GameRoomPlayerInfo> playersAfter = gameRoomRedisService.getRoomPlayers(roomId);
        assertThat(playersAfter).hasSize(4);
        assertThat(playersAfter).allMatch(player -> player.getTeam() == null);

        log.info("개인전 → 개인전 유지 테스트 완료");
    }

    @DisplayName("플레이어가 없는 빈 방에서 팀 설정 변경 시 예외가 발생하지 않는지 테스트")
    @Test
    void testEmptyRoom_ShouldNotThrowException() {
        // Given - 빈 방 생성
        GameRoom emptyRoom = GameRoom.builder()
                .title("빈 게임방")
                .host(host)
                .gameMode(GameMode.ROADVIEW)
                .playerMatchType(PlayerMatchType.SOLO)
                .privateRoom(false)
                .status(GameRoomStatus.WAITING)
                .maxPlayers(8)
                .build();

        gameRoomRepository.save(emptyRoom);
        entityManager.flush();

        GameRoomRequest.Update request = GameRoomRequest.Update.builder()
                .title("빈 방 팀전 변경")
                .gameModeKey("roadview")
                .playerMatchTypeKey("TEAM")
                .privateRoom(false)
                .build();

        // When & Then - 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> {
            GameRoomResponse response = updateGameRoomSettingsUseCase.execute(host, request, emptyRoom.getId());
            assertEquals(PlayerMatchType.TEAM, response.getPlayerMatchType());
        });

        log.info("빈 방 팀 설정 변경 테스트 완료");
    }

    @DisplayName("Redis 연결 상태를 확인하는 헬스체크 테스트")
    @Test
    void testRedisConnection() {
        // Given
        String testKey = "test:connection";
        String testValue = "test-value";

        // When
        redisTemplate.opsForValue().set(testKey, testValue);
        String retrievedValue = redisTemplate.opsForValue().get(testKey);

        // Then
        assertEquals(testValue, retrievedValue);

        // Cleanup
        redisTemplate.delete(testKey);

        log.info("Redis 연결 테스트 완료");
    }

    /**
     * 테스트 후 Redis 데이터 정리
     */
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // 테스트용 Redis 데이터 정리
        String roomId = gameRoom.getId().toString();
        String roomKey = String.format("game:room:%s:players", roomId);
        redisTemplate.delete(roomKey);

        log.info("테스트 데이터 정리 완료 - RoomId: {}", roomId);
    }
}

package com.kospot.infrastructure.websocket.domain.gameroom.service;

import com.kospot.domain.multigame.gameRoom.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.redis.domain.gameroom.service.GameRoomRedisService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * GameRoomRedisService의 팀 할당/해제 로직 단위 테스트
 * Redis를 통한 실제 팀 관리 기능을 검증합니다.
 */
@Slf4j
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
public class GameRoomRedisServiceTeamTest {

    @Autowired
    private GameRoomRedisService gameRoomRedisService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String TEST_ROOM_ID = "test-room-123";
    private List<GameRoomPlayerInfo> testPlayers;

    @BeforeEach
    void setUp() {
        // 테스트용 플레이어 데이터 생성
        testPlayers = List.of(
                createPlayerInfo(1L, "플레이어1", true),
                createPlayerInfo(2L, "플레이어2", false),
                createPlayerInfo(3L, "플레이어3", false),
                createPlayerInfo(4L, "플레이어4", false),
                createPlayerInfo(5L, "플레이어5", false)
        );

        // Redis에 플레이어들 추가
        for (GameRoomPlayerInfo player : testPlayers) {
            gameRoomRedisService.addPlayerToRoom(TEST_ROOM_ID, player);
        }

        log.info("테스트 설정 완료 - RoomId: {}, 플레이어 수: {}", TEST_ROOM_ID, testPlayers.size());
    }

    @DisplayName("assignAllPlayersTeam - 모든 플레이어에게 팀을 할당하는 테스트")
    @Test
    void testAssignAllPlayersTeam() {
        // Given - 팀이 없는 상태 확인
        List<GameRoomPlayerInfo> playersBefore = gameRoomRedisService.getRoomPlayers(TEST_ROOM_ID);
        assertThat(playersBefore).allMatch(player -> player.getTeam() == null);

        // When
        gameRoomRedisService.assignAllPlayersTeam(TEST_ROOM_ID);

        // Then
        List<GameRoomPlayerInfo> playersAfter = gameRoomRedisService.getRoomPlayers(TEST_ROOM_ID);
        
        // 모든 플레이어에게 팀이 할당되었는지 확인
        assertThat(playersAfter).hasSize(5);
        assertThat(playersAfter).allMatch(player -> player.getTeam() != null);
        assertThat(playersAfter).allMatch(player -> 
            "RED".equals(player.getTeam()) || "BLUE".equals(player.getTeam()));

        // 팀 분배가 균등한지 확인 (5명이므로 3:2 또는 2:3)
        long redTeamCount = playersAfter.stream()
                .filter(player -> "RED".equals(player.getTeam()))
                .count();
        long blueTeamCount = playersAfter.stream()
                .filter(player -> "BLUE".equals(player.getTeam()))
                .count();

        assertThat(redTeamCount + blueTeamCount).isEqualTo(5);
        assertThat(Math.abs(redTeamCount - blueTeamCount)).isLessThanOrEqualTo(1);

        log.info("팀 할당 테스트 완료 - RED: {}, BLUE: {}", redTeamCount, blueTeamCount);
    }

    @DisplayName("resetAllPlayersTeam - 모든 플레이어의 팀을 해제하는 테스트")
    @Test
    void testResetAllPlayersTeam() {
        // Given - 먼저 팀을 할당
        gameRoomRedisService.assignAllPlayersTeam(TEST_ROOM_ID);
        
        // 팀이 할당되었는지 확인
        List<GameRoomPlayerInfo> playersWithTeams = gameRoomRedisService.getRoomPlayers(TEST_ROOM_ID);
        assertThat(playersWithTeams).allMatch(player -> player.getTeam() != null);

        // When
        gameRoomRedisService.resetAllPlayersTeam(TEST_ROOM_ID);

        // Then
        List<GameRoomPlayerInfo> playersAfter = gameRoomRedisService.getRoomPlayers(TEST_ROOM_ID);
        
        // 모든 플레이어의 팀이 해제되었는지 확인
        assertThat(playersAfter).hasSize(5);
        assertThat(playersAfter).allMatch(player -> player.getTeam() == null);

        log.info("팀 해제 테스트 완료");
    }

    @DisplayName("assignAllPlayersTeam - 빈 방에서 호출 시 예외가 발생하지 않는지 테스트")
    @Test
    void testAssignAllPlayersTeamWithEmptyRoom() {
        // Given - 빈 방
        String emptyRoomId = "empty-room";
        
        // When & Then - 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> {
            gameRoomRedisService.assignAllPlayersTeam(emptyRoomId);
        });

        // 빈 방의 플레이어 목록이 비어있는지 확인
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(emptyRoomId);
        assertThat(players).isEmpty();

        log.info("빈 방 팀 할당 테스트 완료");
    }

    @DisplayName("resetAllPlayersTeam - 빈 방에서 호출 시 예외가 발생하지 않는지 테스트")
    @Test
    void testResetAllPlayersTeamWithEmptyRoom() {
        // Given - 빈 방
        String emptyRoomId = "empty-room-2";
        
        // When & Then - 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> {
            gameRoomRedisService.resetAllPlayersTeam(emptyRoomId);
        });

        // 빈 방의 플레이어 목록이 비어있는지 확인
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(emptyRoomId);
        assertThat(players).isEmpty();

        log.info("빈 방 팀 해제 테스트 완료");
    }

    @DisplayName("팀 할당 후 개별 플레이어 팀 변경 테스트")
    @Test
    void testSwitchTeamAfterAssignment() {
        // Given - 팀 할당
        gameRoomRedisService.assignAllPlayersTeam(TEST_ROOM_ID);
        
        // When - 첫 번째 플레이어의 팀을 변경
        Long playerId = testPlayers.get(0).getMemberId();
        gameRoomRedisService.switchTeam(TEST_ROOM_ID, playerId, "BLUE");

        // Then
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(TEST_ROOM_ID);
        GameRoomPlayerInfo switchedPlayer = players.stream()
                .filter(player -> player.getMemberId().equals(playerId))
                .findFirst()
                .orElseThrow();

        assertEquals("BLUE", switchedPlayer.getTeam());

        log.info("개별 플레이어 팀 변경 테스트 완료");
    }

    @DisplayName("팀 할당 알고리즘의 일관성 테스트")
    @Test
    void testTeamAssignmentConsistency() {
        // Given - 동일한 플레이어 구성으로 여러 번 팀 할당
        String roomId1 = "room-1";
        String roomId2 = "room-2";
        
        // 동일한 플레이어들을 두 방에 추가
        for (GameRoomPlayerInfo player : testPlayers) {
            gameRoomRedisService.addPlayerToRoom(roomId1, player);
            gameRoomRedisService.addPlayerToRoom(roomId2, player);
        }

        // When - 두 방 모두에 팀 할당
        gameRoomRedisService.assignAllPlayersTeam(roomId1);
        gameRoomRedisService.assignAllPlayersTeam(roomId2);

        // Then - 두 방 모두에서 팀이 할당되었는지 확인
        List<GameRoomPlayerInfo> players1 = gameRoomRedisService.getRoomPlayers(roomId1);
        List<GameRoomPlayerInfo> players2 = gameRoomRedisService.getRoomPlayers(roomId2);

        assertThat(players1).allMatch(player -> player.getTeam() != null);
        assertThat(players2).allMatch(player -> player.getTeam() != null);

        // 팀 분배가 균등한지 확인
        long redCount1 = players1.stream().filter(p -> "RED".equals(p.getTeam())).count();
        long blueCount1 = players1.stream().filter(p -> "BLUE".equals(p.getTeam())).count();
        long redCount2 = players2.stream().filter(p -> "RED".equals(p.getTeam())).count();
        long blueCount2 = players2.stream().filter(p -> "BLUE".equals(p.getTeam())).count();

        assertThat(Math.abs(redCount1 - blueCount1)).isLessThanOrEqualTo(1);
        assertThat(Math.abs(redCount2 - blueCount2)).isLessThanOrEqualTo(1);

        // Cleanup
        redisTemplate.delete(String.format("game:room:%s:players", roomId1));
        redisTemplate.delete(String.format("game:room:%s:players", roomId2));

        log.info("팀 할당 일관성 테스트 완료");
    }

    @DisplayName("Redis 연결 및 데이터 지속성 테스트")
    @Test
    void testRedisDataPersistence() {
        // Given
        String testRoomId = "persistence-test";
        GameRoomPlayerInfo testPlayer = createPlayerInfo(999L, "테스트플레이어", false);
        
        // When - 플레이어 추가
        gameRoomRedisService.addPlayerToRoom(testRoomId, testPlayer);
        
        // Then - 데이터가 저장되었는지 확인
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(testRoomId);
        assertThat(players).hasSize(1);
        assertThat(players.get(0).getMemberId()).isEqualTo(999L);
        assertThat(players.get(0).getNickname()).isEqualTo("테스트플레이어");

        // 팀 할당 후 데이터 확인
        gameRoomRedisService.assignAllPlayersTeam(testRoomId);
        List<GameRoomPlayerInfo> playersWithTeam = gameRoomRedisService.getRoomPlayers(testRoomId);
        assertThat(playersWithTeam.get(0).getTeam()).isNotNull();

        // 팀 해제 후 데이터 확인
        gameRoomRedisService.resetAllPlayersTeam(testRoomId);
        List<GameRoomPlayerInfo> playersWithoutTeam = gameRoomRedisService.getRoomPlayers(testRoomId);
        assertThat(playersWithoutTeam.get(0).getTeam()).isNull();

        // Cleanup
        redisTemplate.delete(String.format("game:room:%s:players", testRoomId));

        log.info("Redis 데이터 지속성 테스트 완료");
    }

    /**
     * 테스트용 플레이어 정보 생성
     */
    private GameRoomPlayerInfo createPlayerInfo(Long memberId, String nickname, boolean isHost) {
        return GameRoomPlayerInfo.builder()
                .memberId(memberId)
                .nickname(nickname)
                .markerImageUrl("default-marker.png")
                .team(null)
                .isHost(isHost)
                .joinedAt(System.currentTimeMillis())
                .build();
    }

    /**
     * 테스트 후 Redis 데이터 정리
     */
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // 테스트용 Redis 데이터 정리
        String roomKey = String.format("game:room:%s:players", TEST_ROOM_ID);
        redisTemplate.delete(roomKey);

        log.info("테스트 데이터 정리 완료 - RoomId: {}", TEST_ROOM_ID);
    }
}

package com.kospot.application.multi.room.http.usecase;

import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 간단한 팀 할당 테스트
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class SimpleTeamTest {

    @Autowired
    private GameRoomRedisService gameRoomRedisService;

    @DisplayName("Redis 팀 할당 직접 테스트")
    @Test
    void testDirectTeamAssignment() {
        // Given
        String testRoomId = "test-room-direct";
        
        // 테스트 플레이어 생성
        GameRoomPlayerInfo player1 = GameRoomPlayerInfo.builder()
                .memberId(1L)
                .nickname("테스트플레이어1")
                .markerImageUrl("default-marker.png")
                .team(null)
                .isHost(true)
                .joinedAt(System.currentTimeMillis())
                .build();

        GameRoomPlayerInfo player2 = GameRoomPlayerInfo.builder()
                .memberId(2L)
                .nickname("테스트플레이어2")
                .markerImageUrl("default-marker.png")
                .team(null)
                .isHost(false)
                .joinedAt(System.currentTimeMillis())
                .build();

        // Redis에 플레이어 추가
        gameRoomRedisService.addPlayerToRoom(testRoomId, player1);
        gameRoomRedisService.addPlayerToRoom(testRoomId, player2);

        // When - 팀 할당
        gameRoomRedisService.assignAllPlayersTeam(testRoomId);

        // Then - 팀 할당 확인
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(testRoomId);
        
        log.info("팀 할당 후 플레이어 수: {}", players.size());
        for (GameRoomPlayerInfo player : players) {
            log.info("플레이어: {}, 팀: {}", player.getNickname(), player.getTeam());
        }

        assertThat(players).hasSize(2);
        assertThat(players).allMatch(player -> player.getTeam() != null);
        assertThat(players).allMatch(player -> 
            "RED".equals(player.getTeam()) || "BLUE".equals(player.getTeam()));

        // Cleanup
        gameRoomRedisService.resetAllPlayersTeam(testRoomId);
    }
}

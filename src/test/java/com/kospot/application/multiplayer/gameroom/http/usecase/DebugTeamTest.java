package com.kospot.application.multiplayer.gameroom.http.usecase;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.multigame.game.vo.PlayerMatchType;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRoom.repository.GameRoomRepository;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomPlayerInfo;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomStatus;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomRedisService;
import com.kospot.presentation.multigame.gameroom.dto.request.GameRoomRequest;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 팀 설정 변경 디버깅 테스트
 */
@Slf4j
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
public class DebugTeamTest {

    @Autowired
    private UpdateGameRoomSettingsUseCase updateGameRoomSettingsUseCase;

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRoomRedisService gameRoomRedisService;

    @Autowired
    private EntityManager entityManager;

    private Member host;
    private List<Member> players;
    private GameRoom gameRoom;

    @BeforeEach
    void setUp() {
        // 호스트 생성 및 저장
        host = Member.builder()
                .username("debug-host")
                .nickname("디버그호스트")
                .role(Role.USER)
                .build();
        host = memberRepository.save(host);

        // 플레이어들 생성 및 저장 (2명)
        players = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            Member player = Member.builder()
                    .username("debug-player" + i)
                    .nickname("디버그플레이어" + i)
                    .role(Role.USER)
                    .build();
            players.add(memberRepository.save(player));
        }

        // 게임방 생성 (개인전으로 시작)
        gameRoom = GameRoom.builder()
                .title("디버그 게임방")
                .host(host)
                .gameMode(GameMode.ROADVIEW)
                .playerMatchType(PlayerMatchType.SOLO)
                .privateRoom(false)
                .status(GameRoomStatus.WAITING)
                .maxPlayers(8)
                .build();

        gameRoom = gameRoomRepository.save(gameRoom);
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

        log.info("디버그 테스트 설정 완료 - 게임방 ID: {}, 플레이어 수: {}", roomId, players.size());
    }

    @DisplayName("팀 설정 변경 디버깅 테스트")
    @Test
    void testTeamSettingChangeDebug() {
        // Given
        String roomId = gameRoom.getId().toString();
        
        // 현재 상태 확인
        log.info("=== 현재 상태 확인 ===");
        log.info("GameRoom PlayerMatchType: {}", gameRoom.getPlayerMatchType());
        List<GameRoomPlayerInfo> playersBefore = gameRoomRedisService.getRoomPlayers(roomId);
        log.info("Redis 플레이어 수: {}", playersBefore.size());
        for (GameRoomPlayerInfo player : playersBefore) {
            log.info("플레이어: {}, 팀: {}", player.getNickname(), player.getTeam());
        }

        // 팀전으로 변경 요청
        GameRoomRequest.Update request = GameRoomRequest.Update.builder()
                .title("팀전으로 변경")
                .gameModeKey("roadview")
                .playerMatchTypeKey("TEAM")
                .privateRoom(false)
                .build();

        log.info("=== 팀전 변경 요청 ===");
        log.info("Request PlayerMatchTypeKey: {}", request.getPlayerMatchTypeKey());

        // When
        log.info("=== UpdateGameRoomSettingsUseCase 실행 ===");
        updateGameRoomSettingsUseCase.execute(host, request, gameRoom.getId());

        // Then
        log.info("=== 결과 확인 ===");
        List<GameRoomPlayerInfo> playersAfter = gameRoomRedisService.getRoomPlayers(roomId);
        log.info("변경 후 Redis 플레이어 수: {}", playersAfter.size());
        for (GameRoomPlayerInfo player : playersAfter) {
            log.info("플레이어: {}, 팀: {}", player.getNickname(), player.getTeam());
        }

        // 검증
        assertThat(playersAfter).hasSize(2);
        assertThat(playersAfter).allMatch(player -> player.getTeam() != null);
    }
}

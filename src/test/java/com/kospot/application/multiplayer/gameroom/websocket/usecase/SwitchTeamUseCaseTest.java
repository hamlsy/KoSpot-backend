package com.kospot.application.multiplayer.gameroom.websocket.usecase;

import com.kospot.domain.multigame.room.vo.GameRoomPlayerInfo;
import com.kospot.domain.multigame.gamePlayer.exception.GameTeamHandler;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.infrastructure.websocket.domain.multi.room.service.GameRoomNotificationService;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import com.kospot.presentation.multigame.gameroom.dto.request.GameRoomRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class SwitchTeamUseCaseTest {

    @Autowired
    private SwitchTeamUseCase switchTeamUseCase;

    @Autowired
    private GameRoomRedisService gameRoomRedisService;

    @MockitoBean
    private GameRoomNotificationService gameRoomNotificationService;

    private String roomId;
    private Long memberId;

    @BeforeEach
    void setUp() {
        // 매 테스트마다 고유 roomId 사용으로 격리
        roomId = UUID.randomUUID().toString();
        memberId = 1001L;

        // 사전 상태: RED 팀으로 등록된 플레이어 1명
        GameRoomPlayerInfo player = GameRoomPlayerInfo.builder()
                .memberId(memberId)
                .nickname("member-" + memberId)
                .team("RED")
                .build();
        gameRoomRedisService.addPlayerToRoom(roomId, player);
    }

    private SimpMessageHeaderAccessor buildHeaderAccessor(Long memberId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        HashMap<String, Object> session = new HashMap<>();
        session.put("user", new WebSocketMemberPrincipal(memberId, "nick" + memberId, "m@e.com", "USER"));
        accessor.setSessionAttributes(session);
        return accessor;
    }

    @Test
    @DisplayName("팀 변경 성공 시 Redis에 반영되고 알림이 전송된다")
    void switchTeam_success_updatesRedis_and_notifies() {
        // given
        var headerAccessor = buildHeaderAccessor(memberId);
        var request = GameRoomRequest.SwitchTeam.builder().team("BLUE").build();

        // when
        switchTeamUseCase.execute(roomId, request, headerAccessor);

        // then: Redis의 플레이어 팀이 BLUE로 변경
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(roomId);
        GameRoomPlayerInfo me = players.stream().filter(p -> p.getMemberId().equals(memberId)).findFirst().orElseThrow();
        assertThat(me.getTeam()).isEqualTo("BLUE");

        // then: 알림 호출 검증
        Mockito.verify(gameRoomNotificationService, Mockito.times(1)).notifyPlayerListUpdated(roomId);
    }

    @Test
    @DisplayName("대상 팀이 정원(4명)일 때 팀 변경이 거부된다")
    void switchTeam_denied_when_targetTeam_is_full() {
        // given: BLUE 팀을 4명으로 채움
        for (int i = 0; i < 4; i++) {
            GameRoomPlayerInfo p = GameRoomPlayerInfo.builder()
                    .memberId(2000L + i)
                    .nickname("p" + i)
                    .team("BLUE")
                    .build();
            gameRoomRedisService.addPlayerToRoom(roomId, p);
        }

        var headerAccessor = buildHeaderAccessor(memberId);
        var request = GameRoomRequest.SwitchTeam.builder().team("BLUE").build();

        // when
        switchTeamUseCase.execute(roomId, request, headerAccessor);

        // then: 여전히 RED 팀이어야 함
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(roomId);
        GameRoomPlayerInfo me = players.stream().filter(p -> p.getMemberId().equals(memberId)).findFirst().orElseThrow();
        assertThat(me.getTeam()).isEqualTo("RED");

        Mockito.verify(gameRoomNotificationService, Mockito.times(1)).notifyPlayerListUpdated(roomId);
    }

    @Test
    @DisplayName("잘못된 팀 문자열이면 GameTeamHandler 예외가 발생한다")
    void switchTeam_invalidTeam_throws() {
        var headerAccessor = buildHeaderAccessor(memberId);
        var badRequest = GameRoomRequest.SwitchTeam.builder().team("INVALID_TEAM").build();

        assertThatThrownBy(() -> switchTeamUseCase.execute(roomId, badRequest, headerAccessor))
                .isInstanceOf(GameTeamHandler.class);
    }
}



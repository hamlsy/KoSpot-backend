package com.kospot.application.multi.game.usecase;

import com.kospot.application.multi.round.roadview.NextRoadViewRoundUseCase;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.multi.lobby.service.LobbyRoomNotificationService;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class StartRoadViewSoloGameUseCase {

    private final NextRoadViewRoundUseCase nextRoadViewRoundUseCase;

    /**
     * 모든 플레이어 로딩 ACK 이후 호출되어 1라운드 준비를 NextRoadViewRoundUseCase에 위임한다.
     */
    public MultiRoadViewGameResponse.StartPlayerGame execute(Long roomId, Long gameId) {
        MultiRoadViewGameResponse.StartPlayerGame response = nextRoadViewRoundUseCase.executeInitial(roomId, gameId);
        if (response == null) {
            log.info("Initial round already prepared - RoomId: {}, GameId: {}", roomId, gameId);
        }

        return response;
    }
}


package com.kospot.application.multi.flow;

import com.kospot.presentation.multi.game.dto.message.LoadingAckMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;

/**
 * 플레이어 전환을 관리하는 서비스
 * 기존 책임을 LoadingPhaseService와 GameTransitionOrchestrator로 위임한다.
 * 
 * @deprecated GameTransitionOrchestrator를 직접 사용하세요.
 *             하위 호환성을 위해 유지됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerTransitionService {

    private final GameTransitionOrchestrator gameTransitionOrchestrator;

    /**
     * 게임 시작 브로드캐스트 직후 로딩 상태를 초기화하고 타임아웃 타이머를 건다.
     *
     * @param roomId 방 ID
     * @param gameId 게임 ID
     */
    public void initializeLoadingPhase(String roomId, Long gameId) {
        gameTransitionOrchestrator.initializeLoadingPhase(roomId, gameId);
    }

    /**
     * 플레이어가 준비 완료를 알리면 처리한다.
     *
     * @param roomId         방 ID
     * @param message        로딩 ACK 메시지
     * @param headerAccessor WebSocket 헤더
     */
    public void handleLoadingAck(String roomId, LoadingAckMessage message,
                                  SimpMessageHeaderAccessor headerAccessor) {
        gameTransitionOrchestrator.handleLoadingAck(roomId, message, headerAccessor);
    }
}

package com.kospot.infrastructure.websocket.domain.multi.round.service;

import com.kospot.application.multi.round.message.GameFinishedMessage;
import com.kospot.infrastructure.doc.annotation.WebSocketDoc;
import com.kospot.infrastructure.websocket.domain.multi.game.constants.MultiGameChannelConstants;
import com.kospot.presentation.multi.game.dto.response.MultiGameResponse;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
import com.kospot.presentation.multi.round.dto.response.RoadViewRoundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoundNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @WebSocketDoc(
            trigger = "라운드 끝날 시",
            description = "로드뷰 개인전 라운드 결과 알림 메시지를 방송합니다.",
            destination = MultiGameChannelConstants.PREFIX_GAME + "{roomId}/round/results",
            payloadType = RoadViewRoundResponse.PlayerResult.class
    )
    public void broadcastRoadViewSoloRoundResults(String roomId, RoadViewRoundResponse.PlayerResult playerResult) {
        broadcastRoundResults(roomId, playerResult);
    }

    /**
     * 라운드 결과를 모든 플레이어에게 브로드캐스트
     */
    public void broadcastRoundResults(String roomId, Object notification) {
        sendNotification(roomId, notification, MultiGameChannelConstants.getRoundResultChannel(roomId));
    }

    @WebSocketDoc(
            trigger = "로드뷰 개인전 라운드 시작 시",
            description = "로드뷰 개인전 게임의 라운드 시작 알림 메시지를 방송합니다.",
            destination = MultiGameChannelConstants.PREFIX_GAME + "{roomId}/round/start",
            payloadType = MultiRoadViewGameResponse.NextRound.class
    )
    public void broadcastRoadViewRoundStart(String roomId, MultiRoadViewGameResponse.NextRound nextRound) {
        broadcastRoundStart(roomId, nextRound);
    }

    /**
     * 라운드 시작을 모든 플레이어에게 브로드캐스트
     */
    public void broadcastRoundStart(String roomId, Object notification) {
        sendNotification(roomId, notification, MultiGameChannelConstants.getRoundStartChannel(roomId));
    }

//    public void broadcastRoundEnd(String roomId, Long gameId) {
//        String destination = MultiGameChannelConstants.getRoundResultChannel()
//        sendNotification(roomId, notification, MultiGameChannelConstants.getRoundEndChannel(roomId));
//    }

    private void sendNotification(String roomId, Object notification, String destination) {
        try {
            messagingTemplate.convertAndSend(destination, notification);
            log.info("Round notification sent - RoomId: {}, Destination: {}", roomId, destination);
        } catch (Exception e) {
            log.error("Failed to send round notification - RoomId: {}, Destination: {}", roomId, destination, e);
        }
    }


    /**
     * 게임 종료 알림 (최종 결과 포함)
     */
    @WebSocketDoc(
            trigger = "게임 종료 시",
            description = "게임 종료 알림 메시지를 방송합니다.",
            destination = MultiGameChannelConstants.PREFIX_GAME + "{roomId}/game/finished",
            payloadType = MultiGameResponse.GameFinalResult.class
    )
    public void notifyGameFinishedWithResults(String roomId, MultiGameResponse.GameFinalResult finalResult) {
        String destination = MultiGameChannelConstants.getGameFinishChannel(roomId);
        messagingTemplate.convertAndSend(destination, finalResult);
        log.info("Game finished notification sent with results - RoomId: {}", roomId);
    }
}

package com.kospot.multi.lobby.infrastructure.websocket.service;

import com.kospot.multi.room.domain.entity.GameRoom;
import com.kospot.multi.room.domain.vo.GameRoomStatus;
import com.kospot.doc.infrastructure.annotation.WebSocketDoc;
import com.kospot.multi.lobby.infrastructure.websocket.constants.LobbyChannelConstants;
import com.kospot.multi.lobby.presentation.message.LobbyNotification;
import com.kospot.multi.room.presentation.dto.response.FindGameRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LobbyRoomNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @WebSocketDoc(
            description = "게임 방 생성시 로비 방에 알림을 보냅니다.",
            destination = LobbyChannelConstants.ROOM_LIST_CHANNEL,
            payloadType = LobbyNotification.class,
            trigger = "게임 방이 생성될 때"
    )
    public void notifyRoomCreated(GameRoom gameRoom) {
        FindGameRoomResponse response = FindGameRoomResponse.from(gameRoom, 1L);
        LobbyNotification notification = LobbyNotification.roomCreated(response);
        String destination = LobbyChannelConstants.ROOM_LIST_CHANNEL;
        sendNotification(destination, notification);
    }

    @WebSocketDoc(
            description = "게임 방 삭제시 로비 방에 알림을 보냅니다.",
            destination = LobbyChannelConstants.ROOM_LIST_CHANNEL,
            payloadType = LobbyNotification.class,
            trigger = "게임 방이 삭제될 때"
    )
    public void notifyRoomDeleted(Long roomId) {
        LobbyNotification notification = LobbyNotification.roomDeleted(roomId);
        String destination = LobbyChannelConstants.ROOM_LIST_CHANNEL;
        sendNotification(destination, notification);
    }

    @WebSocketDoc(
            description = "게임 방 설정 변경시 로비 방에 알림을 보냅니다.",
            destination = LobbyChannelConstants.ROOM_LIST_CHANNEL,
            payloadType = LobbyNotification.class,
            trigger = "게임 방 설정 변경될 때"
    )
    public void notifyRoomUpdated(GameRoom gameRoom, long currentPlayerCount) {
        FindGameRoomResponse response = FindGameRoomResponse.from(gameRoom, currentPlayerCount);
        LobbyNotification notification = LobbyNotification.roomUpdated(response);
        String destination = LobbyChannelConstants.ROOM_LIST_CHANNEL;
        sendNotification(destination, notification);
    }

    @WebSocketDoc(
            description = "게임 방 상태 변경시 로비 방에 알림을 보냅니다.",
            destination = LobbyChannelConstants.ROOM_LIST_CHANNEL,
            payloadType = LobbyNotification.class,
            trigger = "게임 방 상태 변경될 때"
    )
    public void notifyRoomStatusUpdated(Long roomId, long currentPlayerCount, GameRoomStatus status) {
        LobbyNotification.StatusUpdatedRoom room = LobbyNotification.StatusUpdatedRoom.builder()
                .currentPlayerCount(currentPlayerCount)
                .status(status)
                .build();
        LobbyNotification notification = LobbyNotification.roomStatusUpdated(roomId, room);
        String destination = LobbyChannelConstants.ROOM_LIST_CHANNEL;
        sendNotification(destination, notification);
    }

    private void sendNotification(String destination, Object notification) {
        try {
            messagingTemplate.convertAndSend(destination, notification);
        } catch (Exception e) {
            log.error("Failed to send lobby notification - Destination: {}", destination, e);
        }
    }

}

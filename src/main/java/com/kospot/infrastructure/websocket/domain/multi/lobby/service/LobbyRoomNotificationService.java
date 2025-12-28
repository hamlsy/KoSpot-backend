package com.kospot.infrastructure.websocket.domain.multi.lobby.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.infrastructure.websocket.domain.multi.lobby.constants.LobbyChannelConstants;
import com.kospot.presentation.multi.lobby.message.LobbyNotification;
import com.kospot.presentation.multi.room.dto.response.FindGameRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LobbyRoomNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public void notifyRoomCreated(GameRoom gameRoom) {
        FindGameRoomResponse response = FindGameRoomResponse.from(gameRoom, 1L);
        LobbyNotification notification = LobbyNotification.roomCreated(response);
        String destination = LobbyChannelConstants.ROOM_LIST_CHANNEL;
        sendNotification(destination, notification);
    }

    public void notifyRoomDeleted(Long roomId) {
        LobbyNotification notification = LobbyNotification.roomDeleted(roomId);
        String destination = LobbyChannelConstants.ROOM_LIST_CHANNEL;
        sendNotification(destination, notification);
    }

    public void notifyRoomUpdated(GameRoom gameRoom) {
        FindGameRoomResponse response = FindGameRoomResponse.from(gameRoom, 1L);
        LobbyNotification notification = LobbyNotification.roomCreated(response);
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

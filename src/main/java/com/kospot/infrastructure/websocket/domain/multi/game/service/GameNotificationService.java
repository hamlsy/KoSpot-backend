package com.kospot.infrastructure.websocket.domain.multi.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.infrastructure.websocket.domain.multi.game.constants.MultiGameChannelConstants;
import com.kospot.infrastructure.websocket.domain.multi.room.constants.GameRoomChannelConstants;
import com.kospot.presentation.multi.flow.dto.message.RoomGameStartMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;


}

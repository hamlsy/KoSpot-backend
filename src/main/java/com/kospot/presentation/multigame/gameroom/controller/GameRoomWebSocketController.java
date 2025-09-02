package com.kospot.presentation.multigame.gameroom.controller;

import com.kospot.application.multiplayer.gameroom.websocket.usecase.SendGameRoomMessageUseCase;
import com.kospot.application.multiplayer.gameroom.websocket.usecase.SetGameRoomIdAttrUseCase;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
@Tag(name = "GameRoom WebSocket", description = "게임방 실시간 통신")
public class GameRoomWebSocketController {

    private final SetGameRoomIdAttrUseCase setGameRoomIdAttrUseCase;
    private final SendGameRoomMessageUseCase sendGameRoomMessageUseCase;

    /**
     * 게임방 채팅 메시지 전송
     * 클라이언트: /app/room.123.chat
     * 구독: /topic/room/123/chat
     */
    @MessageMapping("/room.{roomId}.chat")
    public void sendRoomMessage(@DestinationVariable String roomId, @Valid @Payload ChatMessageDto.GameRoom dto, SimpMessageHeaderAccessor headerAccessor){
        sendGameRoomMessageUseCase.execute(roomId, dto, headerAccessor);
    }

    @SubscribeMapping("/room/{roomId}/playerList")
    public void subscribeGameRoomPlayerList(@DestinationVariable String roomId, SimpMessageHeaderAccessor headerAccessor) {
        setGameRoomIdAttrUseCase.execute(roomId, headerAccessor);
    }

}

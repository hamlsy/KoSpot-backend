package com.kospot.presentation.multigame.gameroom.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
@Tag(name = "GameRoom WebSocket", description = "게임방 실시간 통신")
public class GameRoomWebSocketController {

    /**
     * 게임방 채팅 메시지 전송
     * 클라이언트: /app/room.123.chat
     * 구독: /topic/room/123/chat
     */
    @MessageMapping("/room.{roomId}.chat")
    public void sendRoomMessage(){

    }

    /**
     * 게임방 WebSocket 구독 (플레이어 목록 실시간 업데이트용)
     * 클라이언트: /app/room.123.subscribe
     * 구독: /topic/room/123/playerList, /topic/room/123/status
     */
    @MessageMapping("/room.{roomId}.subscribe")
    public void subscribeToRoom(){

    }

}

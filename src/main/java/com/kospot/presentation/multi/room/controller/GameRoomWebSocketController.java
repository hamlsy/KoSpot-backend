package com.kospot.presentation.multi.room.controller;

import com.kospot.application.multi.room.websocket.usecase.SendGameRoomMessageUseCase;
import com.kospot.application.multi.room.websocket.usecase.SetGameRoomIdAttrUseCase;
import com.kospot.application.multi.room.websocket.usecase.SwitchTeamUseCase;
import com.kospot.infrastructure.doc.annotation.WebSocketDoc;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import com.kospot.presentation.multi.room.dto.request.GameRoomRequest;
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
    private final SwitchTeamUseCase switchTeamUseCase;

    /**
     * 게임방 채팅 메시지 전송
     * 클라이언트: /app/room.123.chat
     * 구독: /topic/room/123/chat
     */
    @WebSocketDoc(
            description = "게임방 채팅 메시지 전송",
            destination = "/app/room.{roomId}.chat",
            payloadType = ChatMessageDto.GameRoom.class,
            trigger = "게임 방 메시지"
    )
    @MessageMapping("/room.{roomId}.chat")
    public void sendRoomMessage(@DestinationVariable("roomId")  String roomId, @Valid @Payload ChatMessageDto.GameRoom dto, SimpMessageHeaderAccessor headerAccessor){
        sendGameRoomMessageUseCase.execute(roomId, dto, headerAccessor);
    }

    @SubscribeMapping("/room/{roomId}/playerList")
    public void subscribeGameRoomPlayerList(@DestinationVariable("roomId")  String roomId, SimpMessageHeaderAccessor headerAccessor) {
        setGameRoomIdAttrUseCase.execute(roomId, headerAccessor);
    }

    //todo 1. race condition 해결
    // 2. 팀 ENUM 최적화
    // 3. redis 내부 팀 직접 변경 서비스
    @MessageMapping("/room.{roomId}.switchTeam")
    public void switchTeam(@DestinationVariable("roomId")  String roomId,
                           @Valid @Payload GameRoomRequest.SwitchTeam request,
                           SimpMessageHeaderAccessor headerAccessor) {
        switchTeamUseCase.execute(roomId, request, headerAccessor);
    }

}

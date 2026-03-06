package com.kospot.presentation.friend.controller;

import com.kospot.application.friend.SendFriendChatMessageUseCase;
import com.kospot.common.doc.annotation.WebSocketDoc;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
@Tag(name = "Friend Chat Websocket", description = "친구 채팅 소켓")
public class FriendWebSocketController {

    private final SendFriendChatMessageUseCase sendFriendChatMessageUseCase;

    @WebSocketDoc(
            description = "친구 채팅방 메시지 전송",
            destination = "/app/chat-rooms.{roomId}.chat",
            payloadType = ChatMessageDto.Friend.class,
            trigger = "친구 채팅 메시지"
    )
    @MessageMapping("/chat-rooms.{roomId}.chat")
    public void sendChatMessage(
            @DestinationVariable("roomId") Long roomId,
            @Valid @Payload ChatMessageDto.Friend dto,
            SimpMessageHeaderAccessor headerAccessor) {
        sendFriendChatMessageUseCase.execute(roomId, dto, headerAccessor);
    }

}

package com.kospot.chat.application.service;

import com.kospot.chat.domain.entity.ChatMessage;
import com.kospot.common.redis.domain.chat.transientstore.TransientChatRedisStore;
import com.kospot.doc.infrastructure.annotation.WebSocketDoc;
import com.kospot.common.websocket.domain.multi.game.constants.MultiGameChannelConstants;
import com.kospot.common.websocket.domain.multi.lobby.constants.LobbyChannelConstants;
import com.kospot.common.websocket.domain.multi.room.constants.GameRoomChannelConstants;
import com.kospot.chat.presentation.dto.event.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TransientChatRedisStore transientChatRedisStore;

    @WebSocketDoc(
            payloadType = ChatMessageEvent.GlobalLobby.class,
            trigger = "글로벌 로비 채팅 메시지 전송",
            description = "글로벌 로비에 채팅 메시지를 전송합니다.",
            destination = LobbyChannelConstants.GLOBAL_LOBBY_CHANNEL
    )
    public void sendGlobalLobbyMessage(ChatMessage chatMessage) {
        processAndSend(
                chatMessage,
                ChatMessageEvent.GlobalLobby::from,  // DTO 변환 전략
                LobbyChannelConstants.GLOBAL_LOBBY_CHANNEL,
                TransientChatRedisStore.GLOBAL_LOBBY_CHAT_KEY
        );
    }

    public <T> void processAndSend(
            ChatMessage chatMessage,
            Function<ChatMessage, T> dtoMapper,
            String destination,
            String transientStorageKey
    ) {
        try {
            if (chatMessage.getMessageId() == null) {
                chatMessage.generateMessageId();
            }
            if (!validateMessageDeduplication(chatMessage)) {
                return;
            }

            T response = dtoMapper.apply(chatMessage);

            transientChatRedisStore.store(transientStorageKey, response, resolveTimestampMillis());

            simpMessagingTemplate.convertAndSend(destination, response);

            log.debug("Chat message processed: {} by user {}", chatMessage.getMessageId(), chatMessage.getMemberId());

        } catch (Exception e) {
            log.error("Error processing chat message for user: " + chatMessage.getMemberId(), e);
        }
    }

    private boolean validateMessageDeduplication(ChatMessage chatMessage) {
        String deduplicationKey = "dedup:" + chatMessage.getMessageId();
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(deduplicationKey, "1", Duration.ofMinutes(5));
        if (!Boolean.TRUE.equals(isNew)) {
            log.warn("Duplicate message detected: {}", chatMessage.getMessageId());
            return false;
        }
        return true;
    }

    private long resolveTimestampMillis() {
        return System.currentTimeMillis();
    }

    @WebSocketDoc(
            payloadType = ChatMessageEvent.GameRoom.class,
            trigger = "게임방 채팅 메시지 전송",
            description = "특정 게임방에 채팅 메시지를 전송합니다.",
            destination = GameRoomChannelConstants.PREFIX_GAME_ROOM + "/{roomId}/chat"
    )
    public void sendGameRoomMessage(ChatMessage chatMessage) {
        processAndSend(chatMessage,
                ChatMessageEvent.GameRoom::from,
                GameRoomChannelConstants.getGameRoomChatChannel(chatMessage.getGameRoomId().toString()),
                TransientChatRedisStore.gameRoomChatKey(chatMessage.getGameRoomId())
        );
    }

    @WebSocketDoc(
            payloadType = ChatMessageEvent.MultiGameGlobal.class,
            trigger = "솔로게임 채팅 메시지 전송",
            description = "특정 솔로게임에 채팅 메시지를 전송합니다.",
            destination = MultiGameChannelConstants.PREFIX_GAME + "{roomId}/chat/global"
    )
    public void sendSoloGameMessage(ChatMessage chatMessage) {
        processAndSend(chatMessage,
                ChatMessageEvent.MultiGameGlobal::from,
                MultiGameChannelConstants.getGlobalChatChannel(chatMessage.getGameRoomId().toString()),
                TransientChatRedisStore.globalGameChatKey(chatMessage.getGameRoomId())
        );
    }

}

package com.kospot.domain.chat.service;

import com.kospot.domain.chat.entity.ChatMessage;
import com.kospot.domain.chat.repository.ChatMessageRepository;
import com.kospot.infrastructure.websocket.domain.multi.room.constants.GameRoomChannelConstants;
import com.kospot.presentation.chat.dto.event.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.function.Function;

import static com.kospot.infrastructure.websocket.constants.WebSocketChannelConstants.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public void sendGlobalLobbyMessage(ChatMessage chatMessage) {
        processAndSend(
                chatMessage,
                ChatMessageEvent.GlobalLobby::from,  // DTO 변환 전략
                GLOBAL_LOBBY_CHANNEL      // 채널
        );
    }

    public <T> void processAndSend(
            ChatMessage chatMessage,
            Function<ChatMessage, T> dtoMapper,
            String destination
    ) {
        try {
            chatMessage.generateMessageId();
            validateMessageDeduplication(chatMessage);

            chatMessageRepository.save(chatMessage);

            T response = dtoMapper.apply(chatMessage);

            simpMessagingTemplate.convertAndSend(destination, response);

            log.debug("Chat message processed: {} by user {}", chatMessage.getMessageId(), chatMessage.getMemberId());

        } catch (Exception e) {
            log.error("Error processing chat message for user: " + chatMessage.getMemberId(), e);
        }
    }

    private void validateMessageDeduplication(ChatMessage chatMessage) {
        String deduplicationKey = "dedup:" + chatMessage.getMessageId();
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(deduplicationKey, "1", Duration.ofMinutes(5));
        if (!Boolean.TRUE.equals(isNew)) {
            log.warn("Duplicate message detected: {}", chatMessage.getMessageId());
        }
    }

    public void sendGameRoomMessage(ChatMessage chatMessage) {
        processAndSend(chatMessage,
                ChatMessageEvent.GameRoom::from,
                GameRoomChannelConstants.getGameRoomChatChannel(chatMessage.getGameRoomId().toString())
        );
    }

}

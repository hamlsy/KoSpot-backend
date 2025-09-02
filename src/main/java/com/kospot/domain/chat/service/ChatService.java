package com.kospot.domain.chat.service;

import com.kospot.domain.chat.entity.ChatMessage;
import com.kospot.domain.chat.repository.ChatMessageRepository;
import com.kospot.infrastructure.websocket.domain.gameroom.constants.GameRoomChannelConstants;
import com.kospot.presentation.chat.dto.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.function.Function;

import static com.kospot.infrastructure.redis.constants.RedisKeyConstants.REDIS_LOBBY_USERS;
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
                ChatMessageResponse.GlobalLobby::from,  // DTO 변환 전략
                PREFIX_CHAT + GLOBAL_LOBBY_CHANNEL      // 채널
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
            return;
        }
    }

    public void sendGameRoomMessage(ChatMessage chatMessage) {
        processAndSend(chatMessage,
                ChatMessageResponse.GameRoom::from,
                GameRoomChannelConstants.getGameRoomChatChannel(chatMessage.getGameRoomId().toString())
        );
    }

    public void joinGlobalLobby(Long memberId, String sessionId) {
        try {
            // 활성 사용자 세션 관리, 중복 세션 방지 -> field: memberId
            redisTemplate.opsForHash().put(REDIS_LOBBY_USERS, sessionId, memberId.toString());
            log.info("User {} joined global lobby with session {}", sessionId, memberId);

        } catch (Exception e) {
            log.error("Error joining global lobby for user: " + memberId, e);
        }
    }

    public void leaveGlobalLobby(String sessionId) {
        try {
            // 세션 정보 정리
            redisTemplate.opsForHash().delete(REDIS_LOBBY_USERS, sessionId);
            log.info("User {} left global lobby", sessionId);

        } catch (Exception e) {
            log.error("Error leaving global lobby for user: " + sessionId, e);
        }
    }
}

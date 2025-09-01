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
        // 디버깅용 try-catch
        try {
            chatMessage.generateMessageId();
            //todo to constants
            String deduplicationKey = "dedup:" + chatMessage.getMessageId();
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(deduplicationKey, "1", Duration.ofMinutes(5));
            if (!Boolean.TRUE.equals(isNew)) {
                log.warn("Duplicate message detected: {}", chatMessage.getMessageId());
                return;
            }
            //save message todo -> batch save
            chatMessageRepository.save(chatMessage);

            //convert response dto
            ChatMessageResponse.GlobalLobby response = ChatMessageResponse.GlobalLobby.from(chatMessage);

            //send to global lobby channel
            simpMessagingTemplate.convertAndSend(PREFIX_CHAT + GLOBAL_LOBBY_CHANNEL, response);

//            todo 비동기 DB 저장을 위해 배치 큐에 추가 (성능 최적화)
//            batchService.addMessageToQueue(chatMessageDto);

            log.debug("Global chat message processed: {} by user {}", chatMessage.getMessageId(), chatMessage.getMemberId());

        } catch (Exception e) {
            log.error("Error processing global chat message for user: " + chatMessage.getMemberId(), e);
        }

    }

    public void sendGameRoomMessage(ChatMessage chatMessage) {
        try {
            chatMessage.generateMessageId();
            //todo to constants
            String deduplicationKey = "dedup:" + chatMessage.getMessageId();
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(deduplicationKey, "1", Duration.ofMinutes(5));
            if (!Boolean.TRUE.equals(isNew)) {
                log.warn("Duplicate message detected: {}", chatMessage.getMessageId());
                return;
            }
            //save message todo -> batch save
            chatMessageRepository.save(chatMessage);

            //convert response dto
            ChatMessageResponse.GlobalLobby response = ChatMessageResponse.GlobalLobby.from(chatMessage);

            //send to game room channel
            String destination = GameRoomChannelConstants.getGameRoomChatChannel(chatMessage.getGameRoomId().toString());
            simpMessagingTemplate.convertAndSend(destination, response);
        } catch (Exception e) {
            log.error("Error processing global chat message for user: " + chatMessage.getMemberId(), e);
        }
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

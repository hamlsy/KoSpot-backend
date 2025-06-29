package com.kospot.domain.chat.service;

import com.kospot.domain.chat.entity.ChatMessage;
import com.kospot.domain.chat.repository.ChatMessageRepository;
import com.kospot.domain.chat.vo.ChannelType;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static com.kospot.infrastructure.websocket.constants.WebSocketChannelConstants.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public static final int CACHE_HOURS = 1;

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
            simpMessagingTemplate.convertAndSend(PREFIX_CHAT + GLOBAL_LOBBY_CHANNEL, chatMessage);

            chatMessageRepository.save(chatMessage);
//            todo 비동기 DB 저장을 위해 배치 큐에 추가 (성능 최적화)
//            batchService.addMessageToQueue(chatMessageDto);

//            todo Redis에 최근 메시지 캐시 저장 (빠른 히스토리 조회용)
//            cacheRecentMessage(chatMessageDto);

            log.debug("Global chat message processed: {} by user {}", chatMessage.getMessageId(), chatMessage.getMemberId());

        }catch (Exception e) {
            log.error("Error processing global chat message for user: " + chatMessage.getMemberId(), e);
        }

    }

    public void joinGlobalLobby(Long memberId, String sessionId) {
        try {
            // 활성 사용자 세션 관리
            redisTemplate.opsForHash().put(REDIS_LOBBY_USERS, sessionId, memberId.toString());

            // todo 최근 채팅 히스토리 전송
//            List<ChatMessageDto> recentMessages = getRecentMessages(GLOBAL_LOBBY_CHANNEL, 50);
//            if (!recentMessages.isEmpty()) {
//                // 개인 큐로 히스토리 전송
//                messagingTemplate.convertAndSendToUser(
//                        String.valueOf(memberId),
//                        "/queue/chat.history",
//                        recentMessages
//                );
//            }

            log.info("User {} joined global lobby with session {}", memberId, sessionId);

        } catch (Exception e) {
            log.error("Error joining global lobby for user: " + memberId, e);
        }
    }

    public void leaveGlobalLobby(Long userId, String sessionId) {
        try {
            // 세션 정보 정리
            redisTemplate.opsForHash().delete(REDIS_LOBBY_USERS, sessionId);
            log.info("User {} left global lobby", userId);

        } catch (Exception e) {
            log.error("Error leaving global lobby for user: " + userId, e);
        }
    }

//    @SuppressWarnings("unchecked")
//    private List<ChatMessageDto> getRecentMessages(String channelId, int limit) {
//        // 1차: Redis 캐시에서 조회 (빠른 응답)
//        String cacheKey = "chat:recent:" + channelId;
//        List<Object> cached = redisTemplate.opsForList().range(cacheKey, -limit, -1);
//
//        if (cached != null && !cached.isEmpty()) {
//            return cached.stream()
//                    .map(obj -> (ChatMessageDto) obj)
//                    .collect(Collectors.toList());
//        }
//
//        // 2차: Redis에 없으면 DB에서 조회 후 캐시에 저장
//        List<ChatMessage> dbMessages = chatMessageRepository
//                .findRecentMessagesByChannel(ChannelType.GLOBAL_LOBBY, channelId, limit);
//
//        List<ChatMessageDto> messageDtos = dbMessages.stream()
//                .map(this::convertToDto)
//                .collect(Collectors.toList());
//
//        // 조회 결과를 Redis에 캐시 (다음 조회 성능 향상)
//        if (!messageDtos.isEmpty()) {
//            messageDtos.forEach(msg ->
//                    redisTemplate.opsForList().rightPush(cacheKey, msg)
//            );
//            redisTemplate.expire(cacheKey, Duration.ofHours(HOURS)); // 1시간 TTL
//        }
//
//        return messageDtos;
//    }
}

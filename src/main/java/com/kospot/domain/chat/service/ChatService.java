package com.kospot.domain.chat.service;

import com.kospot.domain.chat.entity.ChatMessage;
import com.kospot.domain.chat.repository.ChatMessageRepository;
import com.kospot.domain.chat.vo.ChannelType;
import com.kospot.domain.member.entity.Member;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String GLOBAL_LOBBY_CHANNEL = "global_lobby";
    private static final String REDIS_LOBBY_USERS = "lobby:users";

    public void processGlobalChatMessage(Member member, String content, ChatMessageDto chatMessageDto) {
        try {
            // 메시지 고유 ID 생성 (중복 방지용)
            String messageId = UUID.randomUUID().toString();

            // Redis를 이용한 중복 메시지 방지 (네트워크 지연으로 인한 중복 전송 대응)
            String deduplicationKey = "dedup:" + messageId;
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(deduplicationKey, "1", Duration.ofMinutes(5));

            if (!Boolean.TRUE.equals(isNew)) {
                log.warn("Duplicate message detected: {}", messageId);
                return;
            }

            // 1단계: 즉시 실시간 브로드캐스트 (사용자 경험 우선)
            messagingTemplate.convertAndSend("/topic/chat.global", chatMessageDto);

            // 2단계: 비동기 DB 저장을 위해 배치 큐에 추가 (성능 최적화)
            batchService.addMessageToQueue(chatMessageDto);

            // 3단계: Redis에 최근 메시지 캐시 저장 (빠른 히스토리 조회용)
            cacheRecentMessage(chatMessageDto);

            log.debug("Global chat message processed: {} by user {}", messageId, member.getId());

        } catch (Exception e) {
            log.error("Error processing global chat message for user: " + member.getId(), e);
        }
    }

    public void joinGlobalLobby(Long userId, String sessionId) {
        try {
            // Redis Hash를 이용한 활성 사용자 세션 관리
            redisTemplate.opsForHash().put(REDIS_LOBBY_USERS, sessionId, userId.toString());

            // 최근 채팅 히스토리 전송 (사용자 편의성 향상)
            List<ChatMessageDto> recentMessages = getRecentMessages(GLOBAL_LOBBY_CHANNEL, 50);

            if (!recentMessages.isEmpty()) {
                // 개인 큐로 히스토리 전송
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(userId),
                        "/queue/chat.history",
                        recentMessages
                );
            }

            log.info("User {} joined global lobby with session {}", userId, sessionId);

        } catch (Exception e) {
            log.error("Error joining global lobby for user: " + userId, e);
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

    @SuppressWarnings("unchecked")
    private List<MessageDto> getRecentMessages(String channelId, int limit) {
        // 1차: Redis 캐시에서 조회 (빠른 응답)
        String cacheKey = "chat:recent:" + channelId;
        List<Object> cached = redisTemplate.opsForList().range(cacheKey, -limit, -1);

        if (cached != null && !cached.isEmpty()) {
            return cached.stream()
                    .map(obj -> (MessageDto) obj)
                    .collect(Collectors.toList());
        }

        // 2차: Redis에 없으면 DB에서 조회 후 캐시에 저장
        List<ChatMessage> dbMessages = chatMessageRepository
                .findRecentMessagesByChannel(ChannelType.GLOBAL_LOBBY, channelId, limit);

        List<MessageDto> messageDtos = dbMessages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        // 조회 결과를 Redis에 캐시 (다음 조회 성능 향상)
        if (!messageDtos.isEmpty()) {
            messageDtos.forEach(msg ->
                    redisTemplate.opsForList().rightPush(cacheKey, msg)
            );
            redisTemplate.expire(cacheKey, Duration.ofHours(1)); // 1시간 TTL
        }

        return messageDtos;
    }
}

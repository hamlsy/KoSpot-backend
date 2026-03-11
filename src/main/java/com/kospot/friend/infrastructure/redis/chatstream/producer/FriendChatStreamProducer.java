package com.kospot.friend.infrastructure.redis.chatstream.producer;

import com.kospot.friend.domain.exception.FriendErrorStatus;
import com.kospot.friend.domain.exception.FriendHandler;
import com.kospot.friend.domain.model.FriendChatStreamMessage;
import com.kospot.friend.infrastructure.redis.chatstream.config.FriendChatPersistProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FriendChatStreamProducer {

    private final StringRedisTemplate stringRedisTemplate;
    private final FriendChatPersistProperties properties;

    public RecordId enqueue(FriendChatStreamMessage message) {
        try {
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("message_id", message.messageId());
            payload.put("room_id", String.valueOf(message.roomId()));
            payload.put("sender_member_id", String.valueOf(message.senderMemberId()));
            payload.put("content", message.content());
            payload.put("created_at", message.createdAt().toString());
            payload.put("retry_count", String.valueOf(message.retryCount()));

            MapRecord<String, String, String> record = StreamRecords.newRecord()
                    .in(properties.getStreamKey())
                    .ofMap(payload);

            RecordId recordId = stringRedisTemplate.opsForStream().add(record);
            if (recordId == null) {
                throw new FriendHandler(FriendErrorStatus.FRIEND_CHAT_STREAM_UNAVAILABLE);
            }
            return recordId;
        } catch (FriendHandler e) {
            throw e;
        } catch (Exception e) {
            throw new FriendHandler(FriendErrorStatus.FRIEND_CHAT_STREAM_UNAVAILABLE);
        }
    }
}

package com.kospot.friend.infrastructure.redis.chatstream.dlq;

import com.kospot.friend.infrastructure.redis.chatstream.config.FriendChatPersistProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FriendChatDlqPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final FriendChatPersistProperties properties;

    public void publish(MapRecord<String, Object, Object> record, String reason) {
        Map<String, String> values = new LinkedHashMap<>();
        record.getValue().forEach((k, v) -> values.put(String.valueOf(k), String.valueOf(v)));
        values.put("failure_reason", reason);
        values.put("failed_at", LocalDateTime.now().toString());
        values.put("source_stream_id", record.getId().getValue());

        MapRecord<String, String, String> dlqRecord = StreamRecords.newRecord()
                .in(properties.getDlqStreamKey())
                .ofMap(values);

        stringRedisTemplate.opsForStream().add(dlqRecord);
    }
}

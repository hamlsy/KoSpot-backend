package com.kospot.friend.infrastructure.redis.chatstream.persistence;

import com.kospot.friend.domain.model.FriendChatStreamMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class FriendChatBatchInsertRepository {

    private static final String BATCH_INSERT_SQL = """
            INSERT INTO friend_chat_message (
                message_id,
                room_id,
                sender_member_id,
                content,
                created_date,
                last_modified_date
            ) VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE message_id = message_id
            """;

    private static final String UPDATE_ROOM_LAST_MESSAGE_AT_SQL = """
            UPDATE friend_chat_room
            SET last_message_at = ?,
                last_modified_date = ?
            WHERE id = ?
              AND (last_message_at IS NULL OR last_message_at < ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public void batchInsert(List<FriendChatStreamMessage> messages) {
        jdbcTemplate.batchUpdate(BATCH_INSERT_SQL, messages, messages.size(), (ps, message) -> {
            Timestamp now = Timestamp.valueOf(message.createdAt());
            ps.setString(1, message.messageId());
            ps.setLong(2, message.roomId());
            ps.setLong(3, message.senderMemberId());
            ps.setString(4, message.content());
            ps.setTimestamp(5, now);
            ps.setTimestamp(6, now);
        });
    }

    public void batchUpdateRoomLastMessageAt(Map<Long, LocalDateTime> roomLastMessageAtMap) {
        if (roomLastMessageAtMap == null || roomLastMessageAtMap.isEmpty()) {
            return;
        }

        List<Map.Entry<Long, LocalDateTime>> entries = roomLastMessageAtMap.entrySet().stream().toList();
        jdbcTemplate.batchUpdate(UPDATE_ROOM_LAST_MESSAGE_AT_SQL, entries, entries.size(), (ps, entry) -> {
            Timestamp timestamp = Timestamp.valueOf(entry.getValue());
            ps.setTimestamp(1, timestamp);
            ps.setTimestamp(2, timestamp);
            ps.setLong(3, entry.getKey());
            ps.setTimestamp(4, timestamp);
        });
    }
}

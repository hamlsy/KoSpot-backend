package com.kospot.infrastructure.redis.domain.friend.chatstream.persistence;

import com.kospot.domain.friend.model.FriendChatStreamMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

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
}

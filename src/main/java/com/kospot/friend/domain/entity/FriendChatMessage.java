package com.kospot.friend.domain.entity;

import com.kospot.common.auditing.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "friend_chat_message",
        indexes = {
                @Index(name = "idx_friend_chat_message_room_created", columnList = "roomId,createdDate"),
                @Index(name = "idx_friend_chat_message_sender", columnList = "senderMemberId")
        }
)
public class FriendChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String messageId;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private Long senderMemberId;

    @Column(nullable = false, length = 500)
    private String content;

    public static FriendChatMessage create(Long roomId, Long senderMemberId, String content) {
        return FriendChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .roomId(roomId)
                .senderMemberId(senderMemberId)
                .content(content)
                .build();
    }
}

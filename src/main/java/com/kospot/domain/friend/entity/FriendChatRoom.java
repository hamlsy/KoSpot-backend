package com.kospot.domain.friend.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
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

import java.time.LocalDateTime;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "friend_chat_room",
        indexes = {
                @Index(name = "idx_friend_chat_room_member_low", columnList = "memberLowId"),
                @Index(name = "idx_friend_chat_room_member_high", columnList = "memberHighId")
        }
)
public class FriendChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberLowId;

    @Column(nullable = false)
    private Long memberHighId;

    @Column(nullable = false, unique = true, length = 64)
    private String canonicalPairKey;

    private LocalDateTime lastMessageAt;

    public static FriendChatRoom create(Long memberLowId, Long memberHighId, String canonicalPairKey) {
        return FriendChatRoom.builder()
                .memberLowId(memberLowId)
                .memberHighId(memberHighId)
                .canonicalPairKey(canonicalPairKey)
                .build();
    }

    public void touchLastMessageAt() {
        this.lastMessageAt = LocalDateTime.now();
    }

    public boolean isParticipant(Long memberId) {
        return memberLowId.equals(memberId) || memberHighId.equals(memberId);
    }
}

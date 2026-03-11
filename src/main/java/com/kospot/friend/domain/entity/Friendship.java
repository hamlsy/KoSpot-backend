package com.kospot.friend.domain.entity;

import com.kospot.common.auditing.entity.BaseTimeEntity;
import com.kospot.friend.domain.vo.FriendshipStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "friendship",
        indexes = {
                @Index(name = "idx_friendship_member_low", columnList = "memberLowId,status"),
                @Index(name = "idx_friendship_member_high", columnList = "memberHighId,status")
        }
)
public class Friendship extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberLowId;

    @Column(nullable = false)
    private Long memberHighId;

    @Column(nullable = false, unique = true, length = 64)
    private String canonicalPairKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendshipStatus status;

    private LocalDateTime deletedAt;

    public static Friendship create(Long memberLowId, Long memberHighId, String canonicalPairKey) {
        return Friendship.builder()
                .memberLowId(memberLowId)
                .memberHighId(memberHighId)
                .canonicalPairKey(canonicalPairKey)
                .status(FriendshipStatus.ACTIVE)
                .build();
    }

    public void restore() {
        this.status = FriendshipStatus.ACTIVE;
        this.deletedAt = null;
    }

    public void delete() {
        if (this.status == FriendshipStatus.DELETED) {
            return;
        }
        this.status = FriendshipStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isParticipant(Long memberId) {
        return memberLowId.equals(memberId) || memberHighId.equals(memberId);
    }
}

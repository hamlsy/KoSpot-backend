package com.kospot.domain.friend.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.friend.exception.FriendErrorStatus;
import com.kospot.domain.friend.exception.FriendHandler;
import com.kospot.domain.friend.vo.FriendRequestStatus;
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
        name = "friend_request",
        indexes = {
                @Index(name = "idx_friend_request_receiver_status", columnList = "receiverMemberId,status"),
                @Index(name = "idx_friend_request_pair_key", columnList = "canonicalPairKey")
        }
)
public class FriendRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long requesterMemberId;

    @Column(nullable = false)
    private Long receiverMemberId;

    @Column(nullable = false, unique = true, length = 64)
    private String canonicalPairKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendRequestStatus status;

    private LocalDateTime expiresAt;

    private LocalDateTime actedAt;

    public static FriendRequest create(Long requesterMemberId, Long receiverMemberId, String canonicalPairKey, LocalDateTime expiresAt) {
        return FriendRequest.builder()
                .requesterMemberId(requesterMemberId)
                .receiverMemberId(receiverMemberId)
                .canonicalPairKey(canonicalPairKey)
                .status(FriendRequestStatus.PENDING)
                .expiresAt(expiresAt)
                .build();
    }

    public void reopen(Long requesterMemberId, Long receiverMemberId, LocalDateTime expiresAt) {
        this.requesterMemberId = requesterMemberId;
        this.receiverMemberId = receiverMemberId;
        this.status = FriendRequestStatus.PENDING;
        this.expiresAt = expiresAt;
        this.actedAt = null;
    }

    public void approve(Long receiverMemberId) {
        validateReceiver(receiverMemberId);
        validatePending();
        this.status = FriendRequestStatus.APPROVED;
        this.actedAt = LocalDateTime.now();
    }

    public void reject(Long receiverMemberId) {
        validateReceiver(receiverMemberId);
        validatePending();
        this.status = FriendRequestStatus.REJECTED;
        this.actedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return this.status == FriendRequestStatus.PENDING;
    }

    private void validateReceiver(Long receiverMemberId) {
        if (!this.receiverMemberId.equals(receiverMemberId)) {
            throw new FriendHandler(FriendErrorStatus.FRIEND_REQUEST_RECEIVER_ONLY);
        }
    }

    private void validatePending() {
        if (!isPending()) {
            throw new FriendHandler(FriendErrorStatus.INVALID_FRIEND_REQUEST_STATE);
        }
    }
}

package com.kospot.domain.chat.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.chat.vo.ChannelType;
import com.kospot.domain.chat.vo.MessageType;
import jakarta.persistence.*;
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
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String messageId; // 메시지 ID, UUID 등으로 생성하여 중복 방지

    @Enumerated(EnumType.STRING)
    private ChannelType channelType;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    private String nickname;

    @Column(nullable = false, length = 500)
    private String content;

    private Long memberId;

    private Long gamePlayerId;

    private Long gameRoomId;

    private String teamId; // 팀 채팅일 경우 ID

    //uuid 생성
    public void generateMessageId() {
        this.messageId = UUID.randomUUID().toString();
    }

    @PrePersist
    void prePersist() {
        if(messageId == null) {
            messageId = UUID.randomUUID().toString();
        }
    }

}

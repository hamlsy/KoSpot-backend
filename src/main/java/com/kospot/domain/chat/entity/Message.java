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
public class Message extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String messageId; // 메시지 ID, UUID 등으로 생성하여 중복 방지

    @Enumerated(EnumType.STRING)
    private ChannelType channelType;

    private Long memberId;

    private Long gamePlayerId;

    private Long gameRoomId;

    private String nickname;

    @Column(nullable = false, length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    private String roomCode;

    private String teamId; // 팀 채팅일 경우 ID

    // Factory Methods
    public static Message createLobbyChat(Long gameRoomId, Long memberSenderId, String senderNickname, String content) {
        return Message.builder()
                .gameRoomId(gameRoomId)
                .memberId(memberSenderId)
                .nickname(senderNickname)
                .messageType(MessageType.LOBBY_CHAT)
                .content(content)
                .build();
    }

    public static Message createGameChat(Long gameRoomId, Long gamePlayerId, String senderNickname,
                                         String content) {
        return Message.builder()
                .gameRoomId(gameRoomId)
                .gamePlayerId(gamePlayerId)
                .nickname(senderNickname)
                .messageType(MessageType.GAME_CHAT)
                .content(content)
                .build();
    }

    public static Message createTeamChat(Long gameRoomId, Long gamePlayerId, String senderNickname,
                                         String teamId, String content) {
        return Message.builder()
                .gameRoomId(gameRoomId)
                .gamePlayerId(gamePlayerId)
                .nickname(senderNickname)
                .messageType(MessageType.TEAM_CHAT)
                .content(content)
                .teamId(teamId)
                .build();
    }

    public static Message createSystemMessage(Long gameRoomId, String content) {
        return Message.builder()
                .gameRoomId(gameRoomId)
                .memberId(0L)  // 시스템 메시지는 특별한 ID 사용
                .messageType(MessageType.SYSTEM_MESSAGE)
                .content(content)
                .build();
    }

    //uuid 생성
    @PrePersist
    void prePersist() {
        if(messageId == null) {
            messageId = UUID.randomUUID().toString();
        }
    }

}

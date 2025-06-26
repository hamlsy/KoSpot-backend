package com.kospot.domain.message.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberSenderId;

    private Long gamePlayerSenderId;

    private Long gameRoomId;

    private String senderNickname;

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
                .memberSenderId(memberSenderId)
                .senderNickname(senderNickname)
                .messageType(MessageType.LOBBY_CHAT)
                .content(content)
                .build();
    }

    public static Message createGameChat(Long gameRoomId, Long gamePlayerId, String senderNickname,
                                         String content) {
        return Message.builder()
                .gameRoomId(gameRoomId)
                .gamePlayerSenderId(gamePlayerId)
                .senderNickname(senderNickname)
                .messageType(MessageType.GAME_CHAT)
                .content(content)
                .build();
    }

    public static Message createTeamChat(Long gameRoomId, Long gamePlayerId, String senderNickname,
                                         String teamId, String content) {
        return Message.builder()
                .gameRoomId(gameRoomId)
                .gamePlayerSenderId(gamePlayerId)
                .senderNickname(senderNickname)
                .messageType(MessageType.TEAM_CHAT)
                .content(content)
                .teamId(teamId)
                .build();
    }

    public static Message createSystemMessage(Long gameRoomId, String content) {
        return Message.builder()
                .gameRoomId(gameRoomId)
                .memberSenderId(0L)  // 시스템 메시지는 특별한 ID 사용
                .messageType(MessageType.SYSTEM_MESSAGE)
                .content(content)
                .build();
    }


}

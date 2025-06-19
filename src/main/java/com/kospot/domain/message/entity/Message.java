package com.kospot.domain.message.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_sender_id")
    private Member memberSender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_player_sender_id")
    private GamePlayer gamePlayerSender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_room_id")
    private GameRoom gameRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    private String senderNickname;

    @Column(nullable = false, length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    private String roomCode;

    private Long teamId; // 팀 채팅일 경우 ID

    // create methods
    private void initializeMessage(String content) {
        validateContent(content);
        this.content = content;
    }

    //todo implement validate content method
    private void validateContent(String content) {

    }


}

package com.kospot.kospot.domain.multiplay.gamePlayer.entity;

import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.multiplay.gameRoom.entity.GameRoom;
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
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "game_room_id")
    private GameRoom gameRoom;

    private Integer roundRank; // 해당 라운드 순위
    private int score;

    @Enumerated(EnumType.STRING)
    private GamePlayerStatus status;

    //todo marker image, chatMessage

}

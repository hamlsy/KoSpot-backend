package com.kospot.kospot.domain.multiplay.gameRoom.entity;


import com.kospot.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.kospot.domain.game.entity.GameMode;
import com.kospot.kospot.domain.member.entity.Member;
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
public class GameRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String title;

    private GameMode gameMode;

    private Integer currentPlayers;

    private Integer maxPlayers;

    private String password;

    @Enumerated(EnumType.STRING)
    private GameRoomStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private Member host; //방장

    //todo game players

}

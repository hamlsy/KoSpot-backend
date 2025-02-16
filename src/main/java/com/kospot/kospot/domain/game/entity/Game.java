package com.kospot.kospot.domain.game.entity;

import com.kospot.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class Game extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 정답 좌표
    @Column(nullable = false)
    private double targetLng;

    @Column(nullable = false)
    private double targetLat;

    // 제출 좌표
    private double submittedLng;
    private double submittedLat;

    // 정답 시간
    private Long answerTime;

    // 점수
    private Long score;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type")
    private GameType gameType;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_status")
    private GameStatus gameStatus;

    private LocalDateTime endedAt;    // 게임 종료 시간

    public Game(Coordinate coordinate, Member member, GameType gameType, GameStatus gameStatus){
        this.member = member;
        this.gameType = gameType;
        this.gameStatus = gameStatus;
        this.targetLat = coordinate.getLat();
        this.targetLng = coordinate.getLng();
    }

}

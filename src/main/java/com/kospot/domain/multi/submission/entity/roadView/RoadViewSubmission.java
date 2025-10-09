package com.kospot.domain.multi.submission.entity.roadView;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
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
public class RoadViewSubmission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_round_id")
    private RoadViewGameRound roadViewGameRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_player_id")
    private GamePlayer gamePlayer;

    private Double lat;

    private Double lng;

    private PlayerMatchType playerMatchType;

    // 팀 번호 (1 또는 2)
    private Integer teamNumber;

    // 프론트에서 계산된 정답과의 거리 (미터 단위)
    private Double distance;

    // 순위에 따른 점수
    private Integer earnedScore;

    // 정답까지 걸린 시간(밀리초 단위)
    private Double timeToAnswer;

}

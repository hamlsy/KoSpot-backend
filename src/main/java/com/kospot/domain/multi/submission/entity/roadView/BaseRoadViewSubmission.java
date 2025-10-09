package com.kospot.domain.multi.submission.entity.roadView;


import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.multi.game.util.ScoreRule;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public class BaseRoadViewSubmission extends BaseTimeEntity {

    private Double lat;

    private Double lng;

    // 프론트에서 계산된 정답과의 거리 (미터 단위)
    private Double distance;

    protected Integer earnedScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_round_id")
    protected RoadViewGameRound roadViewGameRound;

    public void setRound(RoadViewGameRound roadViewGameRound) {
        this.roadViewGameRound = roadViewGameRound;
    }

    public void assignScore(Integer rank){
        this.earnedScore = ScoreRule.calculateScore(rank);
    }
}

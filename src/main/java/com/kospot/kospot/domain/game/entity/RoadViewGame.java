package com.kospot.kospot.domain.game.entity;

import com.kospot.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.kospot.domain.coordinate.entity.Coordinate;
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
public class RoadViewGame extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roadViewGame_id")
    private Long id;

    private Long answerTime;

    private double answerDistance;

    private int score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadViewGame_id", nullable = false)
    private Coordinate coordinate;

}

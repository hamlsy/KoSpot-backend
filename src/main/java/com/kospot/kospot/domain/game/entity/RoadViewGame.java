package com.kospot.kospot.domain.game.entity;

import com.kospot.kospot.domain.auditing.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class RoadViewGame extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated
    private GameType gameType;

    // todo 정답 Point 객체
    // todo 내가 고른 Point 객체

    private Long answerTime;

    private double answerDistance;




}

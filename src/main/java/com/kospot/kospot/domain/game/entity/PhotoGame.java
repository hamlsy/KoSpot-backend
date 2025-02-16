package com.kospot.kospot.domain.game.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

//todo
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoGame extends Game {

    // todo 정답 지역
    private String correctLocation;

    private Long averageAnswerTime;

    private int score;

}

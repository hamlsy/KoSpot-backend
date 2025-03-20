package com.kospot.kospot.domain.multiplay.gamePlayer.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GamePlayerStatus {
    WAITING("대기"), PLAYING("진행중"),
    FINISHED("종료"), NONE("none");

    private final String name;
}

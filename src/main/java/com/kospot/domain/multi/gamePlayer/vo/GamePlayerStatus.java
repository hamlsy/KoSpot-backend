package com.kospot.domain.multi.gamePlayer.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GamePlayerStatus {
    WAITING("대기"), PLAYING("진행중"), ABANDONED("탈주"),
    FINISHED("종료"), NONE("none");

    private final String name;
}

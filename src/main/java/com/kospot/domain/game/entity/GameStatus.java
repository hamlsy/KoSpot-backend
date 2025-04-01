package com.kospot.domain.game.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameStatus {
    ABANDONED("탈주"), COMPLETED("완료");

    private final String status;
}

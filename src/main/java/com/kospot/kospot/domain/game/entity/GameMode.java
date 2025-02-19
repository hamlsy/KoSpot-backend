package com.kospot.kospot.domain.game.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameMode {
    RANK("RANK_GAME"), PRACTICE("PRACTICE_GAME");

    private final String key;

}

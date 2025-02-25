package com.kospot.kospot.domain.game.entity;

import com.kospot.kospot.exception.object.domain.CoordinateHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum GameMode {
    RANK("RANK_GAME"), PRACTICE("PRACTICE_GAME");

    private final String mode;

    public static GameMode fromKey(String key) {
        return Arrays.stream(GameMode.values())
                .filter(s -> s.name().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new CoordinateHandler(ErrorStatus.GAME_MODE_NOT_FOUND));
    }

}

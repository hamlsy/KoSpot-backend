package com.kospot.domain.game.entity;

import com.kospot.global.exception.object.domain.CoordinateHandler;
import com.kospot.global.exception.payload.code.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum GameType {
    RANK("랭크 게임"), PRACTICE("연습 게임");

    private final String type;

    public static GameType fromKey(String key) {
        return Arrays.stream(GameType.values())
                .filter(s -> s.name().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new CoordinateHandler(ErrorStatus.GAME_MODE_NOT_FOUND));
    }

}

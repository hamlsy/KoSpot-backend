package com.kospot.kospot.domain.game.entity;

import com.kospot.kospot.exception.object.domain.CoordinateHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum GameType {
    ROADVIEW("로드뷰 모드"), PHOTO("사진 모드");

    private final String type;

    public static GameType fromKey(String key) {
        return Arrays.stream(GameType.values())
                .filter(s -> s.name().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new CoordinateHandler(ErrorStatus.GAME_TYPE_NOT_FOUND));
    }
}

package com.kospot.domain.game.entity;

import com.kospot.exception.object.domain.CoordinateHandler;
import com.kospot.exception.payload.code.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum GameMode {
    ROADVIEW("로드뷰 모드"), PHOTO("사진 모드");

    private final String mode;

    public static GameMode fromKey(String key) {
        return Arrays.stream(GameMode.values())
                .filter(s -> s.name().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new CoordinateHandler(ErrorStatus.GAME_TYPE_NOT_FOUND));
    }
}

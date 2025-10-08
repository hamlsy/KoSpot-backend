package com.kospot.domain.game.vo;

import com.kospot.infrastructure.exception.object.domain.GameHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum GameMode {
    ROADVIEW("로드뷰 모드", Duration.ofMinutes(2)), PHOTO("사진 모드", Duration.ofSeconds(30));

    private final String mode;
    private final Duration duration;

    public static GameMode fromKey(String key) {
        return Arrays.stream(GameMode.values())
                .filter(s -> s.name().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new GameHandler(ErrorStatus.GAME_TYPE_NOT_FOUND));
    }
}

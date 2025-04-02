package com.kospot.domain.multiGame.game.entity;

import com.kospot.domain.game.entity.GameType;
import com.kospot.exception.object.domain.CoordinateHandler;
import com.kospot.exception.payload.code.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PlayerMatchType {
    INDIVIDUAL("개인전"),
    COOPERATIVE("협동전");

    private final String name;

    public static PlayerMatchType fromKey(String key) {
        return Arrays.stream(PlayerMatchType.values())
                .filter(s -> s.name().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new CoordinateHandler(ErrorStatus.PLAYER_MATCH_TYPE_NOT_FOUND));
    }
} 
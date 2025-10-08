package com.kospot.domain.multi.game.vo;

import com.kospot.infrastructure.exception.object.domain.GameRoomHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PlayerMatchType {
    SOLO("개인전"),
    TEAM("협동전");

    private final String type;

    public static PlayerMatchType fromKey(String key) {
        return Arrays.stream(PlayerMatchType.values())
                .filter(s -> s.name().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new GameRoomHandler(ErrorStatus.PLAYER_MATCH_TYPE_NOT_FOUND));
    }
} 
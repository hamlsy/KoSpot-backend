package com.kospot.kospot.domain.item.entity;

import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.exception.object.domain.CoordinateHandler;
import com.kospot.kospot.exception.object.domain.ItemHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ItemType {
    MARKER("마커");

    private final String type;

    public static ItemType fromKey(String key) {
        return Arrays.stream(ItemType.values())
                .filter(s -> s.name().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new ItemHandler(ErrorStatus.ITEM_NOT_FOUND));
    }

}

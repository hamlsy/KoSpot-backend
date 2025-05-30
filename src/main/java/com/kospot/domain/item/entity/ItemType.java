package com.kospot.domain.item.entity;

import com.kospot.global.exception.object.domain.ItemHandler;
import com.kospot.global.exception.payload.code.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ItemType {
    MARKER("마커"), NONE("none");

    private final String type;

    public static ItemType fromKey(String key) {
        return Arrays.stream(ItemType.values())
                .filter(s -> s.name().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new ItemHandler(ErrorStatus.ITEM_NOT_FOUND));
    }

}

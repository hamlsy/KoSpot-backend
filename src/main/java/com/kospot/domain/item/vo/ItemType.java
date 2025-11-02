package com.kospot.domain.item.vo;

import com.kospot.infrastructure.exception.object.domain.ItemHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ItemType {
    MARKER("마커"), MARKER_ANIMATION("마커 애니메이션"),NONE("none");

    private final String type;

    public static ItemType fromKey(String key) {
        return Arrays.stream(ItemType.values())
                .filter(s -> s.name().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new ItemHandler(ErrorStatus.ITEM_NOT_FOUND));
    }

}

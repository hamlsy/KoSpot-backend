package com.kospot.kospot.domain.image.entity;

import com.kospot.kospot.exception.object.domain.ItemHandler;
import com.kospot.kospot.exception.object.domain.S3Handler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ImageType {

    ITEM("아이템"), NOTICE("공지사항"), EVENT("이벤트"),
    BANNER("배너");

    private final String type;

    public static ImageType fromKey(String key) {
        return Arrays.stream(ImageType.values())
                .filter(s -> s.name().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new S3Handler(ErrorStatus.IMAGE_TYPE_NOT_FOUND));
    }
}

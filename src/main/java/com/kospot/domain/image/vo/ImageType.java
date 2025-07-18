package com.kospot.domain.image.vo;

import com.kospot.infrastructure.exception.object.domain.S3Handler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ImageType {

    ITEM("아이템"), NOTICE("공지사항"), EVENT("이벤트"),
    BANNER("배너"), NONE("none"), TEMP("임시");

    private final String type;

    public static ImageType fromKey(String key) {
        return Arrays.stream(ImageType.values())
                .filter(s -> s.name().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new S3Handler(ErrorStatus.IMAGE_TYPE_NOT_FOUND));
    }
}

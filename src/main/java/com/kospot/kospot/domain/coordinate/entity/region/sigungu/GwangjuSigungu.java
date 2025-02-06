package com.kospot.kospot.domain.coordinate.entity.region.sigungu;

import com.kospot.kospot.domain.coordinate.entity.region.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GwangjuSigungu implements Sigungu{
    BUK_GU("북구"),
    DONG_GU("동구"),
    GWANGSAN_GU("광산구"),
    NAM_GU("남구");

    private final String name;

    @Override
    public Sido getParentSido() {
        return Sido.GWANGJU;
    }

}

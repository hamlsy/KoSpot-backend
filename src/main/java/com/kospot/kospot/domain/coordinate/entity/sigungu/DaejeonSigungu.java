package com.kospot.kospot.domain.coordinate.entity.sigungu;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DaejeonSigungu implements Sigungu{
    DAEDEOK_GU("대덕구"),
    DONG_GU("동구"),
    SEO_GU("서구"),
    YUSEONG_GU("유성구"),
    ;

    private final String name;

    @Override
    public Sido getParentSido() {
        return Sido.DAEJEON;
    }
}

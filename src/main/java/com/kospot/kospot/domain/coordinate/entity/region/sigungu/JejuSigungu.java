package com.kospot.kospot.domain.coordinate.entity.region.sigungu;

import com.kospot.kospot.domain.coordinate.entity.region.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JejuSigungu implements Sigungu{
    JEJU_SI("제주시"),
    SEOGWIPO_SI("서귀포시");

    private final String name;

    @Override
    public Sido getParentSido() {
        return Sido.JEJU;
    }
}

package com.kospot.kospot.domain.coordinate.entity.region.sigungu;

import com.kospot.kospot.domain.coordinate.entity.region.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JeonbukSigungu implements Sigungu{
    GOCHANG_GUN("고창군"),
    GUNSAN_SI("군산시"),
    GIMJE_SI("김제시"),
    NAMWON_SI("남원시"),
    MUJU_GUN("무주군"),
    BUAN_GUN("부안군"),
    SUNCHANG_GUN("순창군"),
    WANJU_GUN("완주군"),
    IKSAN_SI("익산시"),
    IMSIL_GUN("임실군"),
    JANGSU_GUN("장수군"),
    JEONJU_SI("전주시"),
    JEONGEUP_SI("정읍시"),
    JINAN_GUN("진안군"),
    ;

    private final String name;

    @Override
    public Sido getParentSido() {
        return Sido.JEONBUK;
    }
}

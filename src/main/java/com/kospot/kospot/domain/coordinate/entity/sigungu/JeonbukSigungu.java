package com.kospot.kospot.domain.coordinate.entity.sigungu;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JeonbukSigungu implements Sigungu {
   JEONJU_SI("전주시"),
   GUNSAN_SI("군산시"),
   IKSAN_SI("익산시"),
   JEONGEUP_SI("정읍시"),
   NAMWON_SI("남원시"),
   GIMJE_SI("김제시"),
   WANJU_GUN("완주군"),
   JINAN_GUN("진안군"),
   MUJU_GUN("무주군"),
   JANGSU_GUN("장수군"),
   IMSIL_GUN("임실군"),
   SUNCHANG_GUN("순창군"),
   GOCHANG_GUN("고창군"),
   BUAN_GUN("부안군"),
    ;

    private final String name;

    @Override
    public Sido getParentSido() {
        return Sido.JEONBUK;
    }
    }
